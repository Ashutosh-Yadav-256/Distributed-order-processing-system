package com.platform.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service to archive customer order invoices to Amazon S3 (Floci or AWS).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3InvoiceArchiverService {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.invoice-bucket:ecommerce-order-invoices}")
    private String bucketName;

    /**
     * Upload an archived JSON invoice to Amazon S3.
     */
    public boolean archiveInvoice(String orderId, String customerId, String customerEmail, BigDecimal totalAmount, String currency) {
        if (s3Client == null) {
            log.warn("S3Client is null; skipping invoice upload to S3.");
            return false;
        }

        String invoiceKey = String.format("invoices/order-%s.json", orderId);

        try {
            ensureBucketExists();

            Map<String, Object> invoiceData = new HashMap<>();
            invoiceData.put("invoiceId", "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            invoiceData.put("orderId", orderId);
            invoiceData.put("customerId", customerId);
            invoiceData.put("customerEmail", customerEmail);
            invoiceData.put("amount", totalAmount);
            invoiceData.put("currency", currency != null ? currency : "USD");
            invoiceData.put("status", "PAID");
            invoiceData.put("archivedAt", Instant.now().toString());
            invoiceData.put("storageProvider", "Floci AWS S3 Emulator / Amazon S3");

            String invoiceJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(invoiceData);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(invoiceKey)
                    .contentType("application/json")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromString(invoiceJson));
            log.info("✔ Successfully archived order invoice to s3://{}/{}", bucketName, invoiceKey);
            return true;

        } catch (Exception e) {
            log.warn("Failed to archive invoice to S3 (Floci AWS emulator may be offline): {}", e.getMessage());
            return false;
        }
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            log.info("Bucket {} does not exist in Floci S3. Creating bucket...", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            // Bucket might already exist or head check not supported by emulator version
            log.debug("HeadBucket check info: {}", e.getMessage());
        }
    }
}
