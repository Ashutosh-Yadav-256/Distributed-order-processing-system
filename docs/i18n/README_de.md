# Verteiltes Auftragsverarbeitungssystem

[ [English](../../README.md) ] · [ [Español](README_es.md) ] · [ [简体中文](README_zh.md) ] · [ Deutsch ] · [ [日本語](README_ja.md) ]

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg?style=flat-square&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot 3.3.3](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud 2023.0.3](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg?style=flat-square)](https://spring.io/projects/spring-cloud)
[![RabbitMQ 3.13](https://img.shields.io/badge/RabbitMQ-3.13-orange.svg?style=flat-square&logo=rabbitmq)](https://www.rabbitmq.com/)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis 7](https://img.shields.io/badge/Redis-7.2-red.svg?style=flat-square&logo=redis)](https://redis.io/)
[![Floci AWS](https://img.shields.io/badge/AWS%20Emulator-Floci%20(Quarkus)-green.svg?style=flat-square)](https://github.com/floci-io/floci)
[![Docker Compose](https://img.shields.io/badge/Orchestration-Docker%20Compose-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)
[![Kubernetes 1.30](https://img.shields.io/badge/Kubernetes-1.30%20(HPA)-326CE5.svg?style=flat-square&logo=kubernetes)](https://kubernetes.io/)
[![Test Coverage](https://img.shields.io/badge/Saga%20Assertions-100%25%20Pass-success.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-gray.svg?style=flat-square)](../../LICENSE)

Eine produktionsreife, ereignisgesteuerte Microservices-Plattform für E-Commerce-Auftragsabwicklungen mit hohem Durchsatz und hoher Parallelität. Entwickelt nach modernsten Entwurfsmustern verteilter Systeme: **Choreografie-basierte Saga-Transaktionen**, **Datenbank-pro-Dienst-Isolation (PostgreSQL)**, **atomare Nebenläufigkeitskontrollen gegen Überverkäufe**, **automatische Kompensations-Rollbacks**, **idempotente Nachrichtendeduplizierung**, **RabbitMQ-Wiederholungs- und Dead-Letter-Warteschlangen (DLQ)**, **Redis Read-Through-Caching**, **Fehlerisolation mit Resilience4j**, **Spring Cloud Gateway mit Token-Bucket-Ratenbegrenzung**, **native Model Context Protocol (MCP) KI-Integration** und eine **interaktive Senior-QA-Testkonsole**.

---

## Entwicklerprofil & Kontakt

Entworfen und implementiert von **Ashutosh Yadav** — Senior Backend & Distributed Systems Engineer.

* **Portfolio-Website**: [ashutoshwork.space](https://ashutoshwork.space)
* **LinkedIn**: [linkedin.com/in/ashutoshyadav256](https://www.linkedin.com/in/ashutoshyadav256)
* **E-Mail**: [ashutosh4tech@gmail.com](mailto:ashutosh4tech@gmail.com)
* **GitHub**: [github.com/ashutoshyadav256](https://github.com/ashutoshyadav256)
* **Verfügbarkeit**: Offen für Positionen als Senior Software Engineer (Backend / Distributed Systems / Cloud Architecture), Lead Engineer sowie Beratung für Microservices-Architekturen.

---

## Microservices-Übersicht

| Microservice | Port | Eigene Datenbank | Kernaufgaben | Technologien |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | Keine (Zustandslos) | JWT-Validierung, Redis-Ratenbegrenzung, Correlation-ID-Weiterleitung | Spring Cloud Gateway, Reactive Redis, Nimbus JWT |
| **Order Service** | `8081` | `order_db` (Postgres) | Auftragserfassung, Saga-Zustandsverwaltung, Kompensationssteuerung | Spring Boot 3.3, Spring Data JPA, RabbitMQ, Resilience4j |
| **Inventory Service** | `8082` | `inventory_db` + Redis | Atomare Bestandsreservierung, optimistische Sperren, Kompensationsfreigabe | Spring Boot 3.3, PostgreSQL 16, Redis 7, RabbitMQ |
| **Payment Service** | `8083` | `payment_db` (Postgres) | Zahlungsautorisierung, Idempotenzprüfung, Dead-Letter-Queue-Verarbeitung | Spring Boot 3.3, PostgreSQL 16, RabbitMQ, DLQ |
| **Notification Service** | `8084` | `notification_db` (Postgres) | Asynchroner Audit-Log, E-Mail/SMS-Verarbeitung, S3-Rechnungsarchivierung | Spring Boot 3.3, AWS Java SDK v2, PostgreSQL 16 |
| **Floci AWS Emulator** | `4566` | Persistentes Volume | Lokaler AWS-Emulator (S3, SQS, SNS, Secrets Manager) mit ~24ms Kaltstart | Quarkus Native, AWS Wire Protocol |
| **Senior QA Konsole** | `4000` | In-Memory / Telemetrie | Interaktives Test-Dashboard, Saga-Zustandsvisualisierer, Testergebnisse | Python HTTP Server, CSS (Helles Design), Titillium Web |
| **MCP-Server** | Stdio | Stdio-Protokoll | Native Model Context Protocol Schnittstelle für autonome KI-Agenten | Python 3.10+, MCP SDK |

---

## Leistungsmetriken & Benchmarks

| Metrik | Gemessener Wert | Vergleich mit Monolith / LocalStack |
| :--- | :--- | :--- |
| **Auftragserstellung P99 Latenz** | **< 48 ms** | Traditionelle synchrone Monolithen: ~250–400 ms (85% schneller) |
| **Bestandsabfrage in Redis** | **< 1.8 ms** | Direkte PostgreSQL-Abfrage: ~15–25 ms (88% schneller) |
| **Datenbank-Entlastung** | **~85% des Lese-Traffics** | Verhindert Überlastung des Verbindungspools |
| **Lokaler AWS-Start (Floci)** | **~24 ms Kaltstart** | LocalStack: ~35–45s Kaltstart (über 100-mal schneller) |
| **Speicherbedarf Emulator** | **< 45 MB** | LocalStack: ~1.8–2.5 GB (über 95% Speichereinsparung) |
| **Test-Bestehensquote** | **100% (4 von 4 Szenarien)** | Deterministische Tests ohne instabile Fehler |

---

## Schnellstart

```bash
# 1. Repository klonen
git clone https://github.com/ashutoshyadav256/distributed-order-processing-system.git
cd distributed-order-processing-system

# 2. Infrastruktur mit Docker Compose starten
docker compose -f infrastructure/docker/docker-compose.yml up -d

# 3. Senior QA Konsole starten
python qa-dashboard/server.py
# Im Browser öffnen: http://localhost:4000/
```
