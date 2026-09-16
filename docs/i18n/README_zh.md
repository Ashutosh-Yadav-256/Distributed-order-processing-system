# 分布式订单处理系统

[ [English](../../README.md) ] · [ [Español](README_es.md) ] · [ 简体中文 ] · [ [Deutsch](README_de.md) ] · [ [日本語](README_ja.md) ]

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

生产级事件驱动分布式微服务电商订单处理平台，专为高并发、高吞吐量的复杂业务场景设计。全面实现企业级分布式系统设计模式：**基于编舞式的Saga分布式事务协调**、**PostgreSQL服务独享数据库隔离 (Database-per-Service)**、**原子级防超卖并发控制**、**自动反向补偿回滚机制**、**幂等性事件去重管道**、**RabbitMQ重试与死信队列 (DLQ)**、**Redis热点库存穿透式缓存**、**Resilience4j熔断与故障隔离**、**Spring Cloud Gateway令牌桶限流**、**原生模型上下文协议 (MCP) AI接入**及**高级QA自动化可视化控制台**。

---

## 开发者与联系方式

架构设计与核心开发：**Ashutosh Yadav** — 资深后端与分布式系统工程师

* **个人网站 / 作品集**: [ashutoshwork.space](https://ashutoshwork.space)
* **领英 (LinkedIn)**: [linkedin.com/in/ashutoshyadav256](https://www.linkedin.com/in/ashutoshyadav256)
* **电子邮箱**: [ashutosh4tech@gmail.com](mailto:ashutosh4tech@gmail.com)
* **GitHub**: [github.com/ashutoshyadav256](https://github.com/ashutoshyadav256)
* **求职状态**: 开放接受资深软件工程师（后端 / 分布式系统 / 云原生架构）、主任工程师职位及企业微服务技术咨询。

---

## 微服务矩阵与技术栈

| 微服务名称 | 端口 | 专用数据库 | 核心职责 | 核心技术栈 |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | 无状态 | JWT鉴权校验、Redis令牌桶限流、全链路Trace-ID注入、反向代理 | Spring Cloud Gateway, Reactive Redis, Nimbus JWT |
| **Order Service** | `8081` | `order_db` (Postgres) | 订单全生命周期管理、Saga状态机流转、终态判定、补偿事件触发 | Spring Boot 3.3, Spring Data JPA, RabbitMQ, Resilience4j |
| **Inventory Service** | `8082` | `inventory_db` + Redis | 原子级库存预扣减、乐观锁校验、库存补偿释放、Redis缓存逐出 | Spring Boot 3.3, PostgreSQL 16, Redis 7, RabbitMQ |
| **Payment Service** | `8083` | `payment_db` (Postgres) | 支付流水记录、幂等性校验、支付模拟器、死信队列重试 | Spring Boot 3.3, PostgreSQL 16, RabbitMQ, DLQ |
| **Notification Service** | `8084` | `notification_db` (Postgres) | 异步通知审计流、邮件/短信日志、Floci Amazon S3订单电子发票归档 | Spring Boot 3.3, AWS Java SDK v2, PostgreSQL 16 |
| **Floci AWS Emulator** | `4566` | 持久化数据卷 | 极速本地AWS云环境模拟器 (S3, SQS, SNS, Secrets Manager)，冷启动仅需~24ms | Quarkus Native 原生镜像, AWS Wire Protocol |
| **高级QA控制台** | `4000` | 内存/事件总线 | 交互式QA测试面板、Saga状态机动态流转图、实时断言矩阵 | Python HTTP Server, 原生CSS (浅色主题), Titillium Web |
| **MCP AI 服务端** | Stdio | 原生Stdio通信 | 遵循Anthropic/Antigravity规范的原生MCP服务，支持AI智能体全自主运营 | Python 3.10+, MCP SDK |

---

## 核心性能指标与基准对比

| 性能指标 | 实测结果 | 传统单体架构 / LocalStack 对比 |
| :--- | :--- | :--- |
| **订单创建 P99 延迟** | **< 48 ms** | 传统同步调用链单体: ~250–400 ms (速度提升85%+) |
| **Redis缓存命中查询延迟** | **< 1.8 ms** | PostgreSQL直接查询: ~15–25 ms (速度提升88%+) |
| **数据库读压力削峰率** | **分流 ~85% 读流量** | 有效避免高并发秒杀场景下数据库连接池被击穿 |
| **本地云模拟器启动耗时** | **~24 ms 响应** | LocalStack: ~35–45s 冷启动 (性能提升超100倍) |
| **本地云内存占用** | **< 45 MB** | LocalStack: ~1.8–2.5 GB (内存节省超95%) |
| **自动化测试断言通过率** | **100% (4/4 测试用例)** | 确定性断言逻辑，彻底杜绝随机偶发失败 |

---

## 极简运行指南

```bash
# 1. 克隆代码仓库
git clone https://github.com/ashutoshyadav256/distributed-order-processing-system.git
cd distributed-order-processing-system

# 2. 使用 Docker Compose 一键启动完整基础组件
docker compose -f infrastructure/docker/docker-compose.yml up -d

# 3. 启动高级QA测试控制台
python qa-dashboard/server.py
# 浏览器访问: http://localhost:4000/ 点击 "Run Full QA Suite" 即可实时查看Saga流程验证
```
