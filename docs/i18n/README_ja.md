# 分散注文処理システム (Distributed Order Processing System)

[ [English](../../README.md) ] · [ [Español](README_es.md) ] · [ [简体中文](README_zh.md) ] · [ [Deutsch](README_de.md) ] · [ 日本語 ]

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

エンタープライズEC向けの高スループット・高並行性を実現する、本番環境レベルのイベント駆動型マイクロサービス注文処理プラットフォームです。**振付型（Choreography）Sagaパターンの分散トランザクション**、**サービス単位の独立PostgreSQLデータベース**、**超過販売（オーバーセル）を防止するアトミック並行制御**、**自動補償トランザクション（ロールバック）**、**冪等性（Idempotency）メッセージ重複排除**、**RabbitMQ指数バックオフ再試行およびデッドレターキュー（DLQ）**、**Redisリードスルーキャッシュと即時無効化**、**Resilience4jによる障害隔離（サーキットブレーカー）**、**Spring Cloud Gatewayトークンバケットレート制限**、**AIエージェント対応Model Context Protocol (MCP) サーバー**、および**シニアQA対話型テストコンソール**を完備しています。

---

## 開発者プロフィール & お問い合わせ

設計および開発：**Ashutosh Yadav** — シニアバックエンド＆分散システムエンジニア (Senior Backend & Distributed Systems Engineer)

* **ポートフォリオサイト**: [ashutoshwork.space](https://ashutoshwork.space)
* **LinkedIn**: [linkedin.com/in/ashutoshyadav256](https://www.linkedin.com/in/ashutoshyadav256)
* **メール**: [ashutosh4tech@gmail.com](mailto:ashutosh4tech@gmail.com)
* **GitHub**: [github.com/ashutoshyadav256](https://github.com/ashutoshyadav256)
* **対応可能ポジション**: シニアバックエンドエンジニア、分散システムエンジニア、クラウドアーキテクト、テックリード、マイクロサービス設計コンサルタント

---

## マイクロサービス構成一覧

| マイクロサービス | ポート | 独立データベース | 主な責務 | 技術スタック |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | なし (ステートレス) | JWT認証検証、Redis分散レート制限、相関ID (Correlation ID) 伝搬 | Spring Cloud Gateway, Reactive Redis, Nimbus JWT |
| **Order Service** | `8081` | `order_db` (Postgres) | 注文受付、Saga状態管理、補償トリガー制御 | Spring Boot 3.3, Spring Data JPA, RabbitMQ, Resilience4j |
| **Inventory Service** | `8082` | `inventory_db` + Redis | アトミック在庫引当、楽観的ロック、補償在庫解放 | Spring Boot 3.3, PostgreSQL 16, Redis 7, RabbitMQ |
| **Payment Service** | `8083` | `payment_db` (Postgres) | 決済承認、冪等性テーブル検証、デッドレターキュー処理 | Spring Boot 3.3, PostgreSQL 16, RabbitMQ, DLQ |
| **Notification Service** | `8084` | `notification_db` (Postgres) | 非同期監査ログ、Eメール・SMS通知送信、S3領収書アーカイブ | Spring Boot 3.3, AWS Java SDK v2, PostgreSQL 16 |
| **Floci AWS Emulator** | `4566` | 永続化ボリューム | ローカルAWSエミュレータ（S3, SQS, SNS, Secrets Manager）約24ms高速起動 | Quarkus Native, AWS Wire Protocol |
| **Senior QA Console** | `4000` | インメモリ / テレメトリ | 対話型テスト実行ダッシュボード、Saga状態可視化、ベンチマーク確認 | Python HTTP Server, CSS (Light Mint Theme), Titillium Web |
| **MCP Server** | Stdio | Stdioプロトコル | 自律型AIエージェント向けModel Context Protocolツールインターフェース | Python 3.10+, MCP SDK |

---

## パフォーマンス指標 & ベンチマーク

| 評価指標 | 実測値 | 従来型モノリス / LocalStackとの比較 |
| :--- | :--- | :--- |
| **注文受付 P99レイテンシ** | **48 ms 未満** | 従来の同期的モノリス：約250〜400 ms（約85%の高速化） |
| **Redis在庫読み取り時間** | **1.8 ms 未満** | 直接PostgreSQLクエリ：約15〜25 ms（約88%の高速化） |
| **DB読み取り負荷削減率** | **約85%削減** | コネクションプール枯渇やスロークエリを根本的に抑止 |
| **ローカルAWS起動時間 (Floci)** | **約24 ms コールドスタート** | LocalStack：約35〜45秒（約100倍以上の起動高速化） |
| **エミュレータ消費メモリ** | **45 MB 未満** | LocalStack：約1.8〜2.5 GB（95%以上のメモリ使用量削減） |
| **Sagaテスト成功率** | **100% (4 / 4 シナリオ合格)** | 不安定なテストのない確定論的（Deterministic）検証 |

---

## クイックスタート

```bash
# 1. リポジトリのクローン
git clone https://github.com/ashutoshyadav256/distributed-order-processing-system.git
cd distributed-order-processing-system

# 2. Docker Compose によるインフラ起動
docker compose -f infrastructure/docker/docker-compose.yml up -d

# 3. シニアQAテストコンソールの起動
python qa-dashboard/server.py
# ブラウザでアクセス: http://localhost:4000/
```
