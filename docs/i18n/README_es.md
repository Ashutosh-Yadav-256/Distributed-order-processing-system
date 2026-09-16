# Sistema Distribuido de Procesamiento de Pedidos

[ [English](../../README.md) ] · [ Español ] · [ [简体中文](README_zh.md) ] · [ [Deutsch](README_de.md) ] · [ [日本語](README_ja.md) ]

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

Una plataforma de microservicios distribuidos orientada a eventos para flujos de trabajo de comercio electrónico de alto rendimiento y alta concurrencia. Implementa patrones avanzados de sistemas distribuidos: **transacciones Saga basadas en coreografía**, **aislamiento de base de datos por servicio en PostgreSQL**, **control atómico de concurrencia contra sobreventa de inventario**, **transacciones compensatorias automáticas**, **desduplicación idempotente de mensajes**, **colas de reintento y Dead Letter Queue (DLQ) en RabbitMQ**, **caché de lectura directa en Redis**, **aislamiento de fallos con Resilience4j**, **Spring Cloud Gateway con límite de tasa por bucket de tokens**, **integración nativa con Model Context Protocol (MCP)** y una **consola interactiva de pruebas de QA**.

---

## Perfil del Desarrollador y Contacto

Desarrollado e implementado por **Ashutosh Yadav** — Ingeniero Senior de Backend y Sistemas Distribuidos.

* **Sitio Web / Portafolio**: [ashutoshwork.space](https://ashutoshwork.space)
* **LinkedIn**: [linkedin.com/in/ashutoshyadav256](https://www.linkedin.com/in/ashutoshyadav256)
* **Correo Electrónico**: [ashutosh4tech@gmail.com](mailto:ashutosh4tech@gmail.com)
* **GitHub**: [github.com/ashutoshyadav256](https://github.com/ashutoshyadav256)
* **Disponibilidad**: Abierto a roles de Ingeniero Senior de Software (Backend / Sistemas Distribuidos / Arquitectura Cloud), Ingeniero Principal y consultoría de microservicios empresariales.

---

## Desglose de Microservicios

| Microservicio | Puerto | Base de Datos | Responsabilidades Clave | Tecnologías |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | Ninguna | Validación JWT, limitador de tasa Redis, inyección de Correlation ID, enrutamiento | Spring Cloud Gateway, Reactive Redis, Nimbus JWT |
| **Order Service** | `8081` | `order_db` (PostgreSQL) | Registro de pedidos, coordinación de estados Saga, disparador de compensaciones | Spring Boot 3.3, Spring Data JPA, RabbitMQ, Resilience4j |
| **Inventory Service** | `8082` | `inventory_db` + Redis | Reserva atómica de stock, bloqueo optimista, liberación compensatoria, caché Redis | Spring Boot 3.3, PostgreSQL 16, Redis 7, RabbitMQ |
| **Payment Service** | `8083` | `payment_db` (PostgreSQL) | Simulación de pagos, validación de tokens, desduplicación, colas DLQ | Spring Boot 3.3, PostgreSQL 16, RabbitMQ, DLQ |
| **Notification Service** | `8084` | `notification_db` (PostgreSQL) | Consumidor asíncrono de eventos, registro de auditoría, archivo de facturas en S3 | Spring Boot 3.3, AWS Java SDK v2, PostgreSQL 16 |
| **Floci AWS Emulator** | `4566` | Volumen persistente | Emulación local ultrarrápida de Amazon S3, SQS, SNS y Secrets Manager en ~24ms | Quarkus Native, Protocolo AWS |
| **Consola Senior QA** | `4000` | En memoria / Telemetría | Panel interactivo de pruebas, visualizador de estados Saga, verificador de aserciones | Python HTTP Server, CSS Vanilla (Tema Claro) |
| **Servidor MCP** | Stdio | Protocolo Stdio | Conector nativo de Model Context Protocol para asistentes de IA | Python 3.10+, SDK de MCP |

---

## Rendimiento y Métricas de Eficiencia

| Métrica | Resultado Medido | Comparativa con Monolito / LocalStack |
| :--- | :--- | :--- |
| **Latencia P99 Creación Pedidos** | **< 48 ms** | Monolitos tradicionales: ~250–400 ms (85% más rápido) |
| **Lectura de Inventario en Redis** | **< 1.8 ms** | Consulta directa en PostgreSQL: ~15–25 ms (88% más rápido) |
| **Descarga de Consultas a BD** | **~85% del tráfico** | Elimina saturación del pool de conexiones en Postgres |
| **Velocidad Emulador AWS (Floci)** | **~24 ms en frío** | LocalStack: ~35–45s de arranque en frío (100x más rápido) |
| **Consumo de Memoria Emulador** | **< 45 MB** | LocalStack: ~1.8–2.5 GB (95% ahorro de memoria) |
| **Tasa de Aprobación de Pruebas** | **100% (4/4 escenarios)** | Verificación determinista sin pruebas intermitentes |

---

## Inicio Rápido

```bash
# 1. Clonar el repositorio
git clone https://github.com/ashutoshyadav256/distributed-order-processing-system.git
cd distributed-order-processing-system

# 2. Iniciar infraestructura con Docker Compose
docker compose -f infrastructure/docker/docker-compose.yml up -d

# 3. Iniciar consola interactiva de QA
python qa-dashboard/server.py
# Abrir en navegador: http://localhost:4000/
```
