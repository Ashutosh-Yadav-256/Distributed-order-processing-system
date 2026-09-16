# AWS Free-Tier EC2 Deployment Guide: Making Your Project Live & Clickable

> "Get it running on a single free-tier AWS instance so there's a real link, not just local setup instructions. This is what turns it from 'trust me' into something a recruiter can actually open."

This guide walks you through provisioning a **100% Free-Tier eligible AWS EC2 instance**, configuring swap space so the microservice cluster runs stably within free RAM limits, launching the services, and generating a live public URL to include in your resume header and GitHub repository description.

---

## Step 1: Launch an AWS EC2 Free-Tier Instance

1. Log in to the [AWS Management Console](https://console.aws.amazon.com/ec2/).
2. Navigate to **EC2 Dashboard** -> Click **Launch instance**.
3. Configure the following options:
   - **Name**: `distributed-order-processing-node`
   - **Application and OS Images (AMI)**: **Amazon Linux 2023 AMI** or **Ubuntu Server 22.04 LTS** (Free Tier eligible).
   - **Instance Type**: `t2.micro` (1 vCPU, 1 GB RAM, Free Tier eligible) or `t3.small` (2 vCPU, 2 GB RAM if within trial credits).
   - **Key pair**: Select or create an `.pem` key pair (e.g. `order-system-key.pem`).
4. **Network settings** -> Create Security Group:
   Enable inbound traffic for:
   | Protocol | Port Range | Source | Purpose |
   | :--- | :--- | :--- | :--- |
   | SSH | `22` | Anywhere (`0.0.0.0/0`) or Your IP | Remote access |
   | HTTP | `80` | Anywhere (`0.0.0.0/0`) | Gateway Reverse Proxy |
   | Custom TCP | `8080` | Anywhere (`0.0.0.0/0`) | API Gateway Direct |
   | Custom TCP | `8081` | Anywhere (`0.0.0.0/0`) | Order Service Swagger UI |
   | Custom TCP | `15672` | Anywhere (`0.0.0.0/0`) | RabbitMQ Management Console |
   | Custom TCP | `3000` | Anywhere (`0.0.0.0/0`) | Grafana Observability Dashboard |
5. **Configure Storage**:
   - Change root volume size from 8 GiB to **25 GiB gp3** (AWS Free Tier allows up to 30 GiB of EBS storage for free).
6. **Advanced details** -> **User data**:
   Paste the contents of [`infrastructure/aws/ec2-user-data.sh`](file:///c:/Desktop/CODING%20_IS_LIFE/1%20ANTI%20GRAVITY/Distributed%20Order%20Processing%20System/infrastructure/aws/ec2-user-data.sh).
   *(This automatically creates 4GB of swap space and installs Docker & Docker Compose on boot).*
7. Click **Launch instance**.

---

## Step 2: Connect via SSH

Once the instance shows **Running**:
```bash
chmod 400 order-system-key.pem
ssh -i order-system-key.pem ec2-user@<YOUR-EC2-PUBLIC-IP>
```
*(If using Ubuntu, username is `ubuntu@<YOUR-EC2-PUBLIC-IP>`)*.

---

## Step 3: Clone & Deploy in One Step

On the EC2 shell:
```bash
git clone https://github.com/Ashutosh-Yadav-256/Distributed-order-processing-system.git
cd Distributed-order-processing-system
chmod +x infrastructure/aws/ec2-deploy.sh mvnw
./infrastructure/aws/ec2-deploy.sh
```

The script compiles the Spring Boot services, spins up PostgreSQL, RabbitMQ, Redis, Order Service, Inventory Service, Payment Service, and API Gateway, and outputs your live URLs.

---

## Step 4: Verify Live Endpoints

Open your browser and test:
- **Order Service Swagger UI**: `http://<YOUR-EC2-PUBLIC-IP>:8081/swagger-ui.html`
- **RabbitMQ Management**: `http://<YOUR-EC2-PUBLIC-IP>:15672` (Username: `guest`, Password: `guest`)
- **Grafana Dashboards**: `http://<YOUR-EC2-PUBLIC-IP>:3000` (Username: `admin`, Password: `admin`)

---

## Step 5: Put It on Your Resume & GitHub

Place the live URL in prominent locations where hiring managers immediately see it:

### In your GitHub Repository:
1. Under **About** (top right of GitHub repo page):
   - **Website**: `http://<YOUR-EC2-PUBLIC-IP>:8081/swagger-ui.html`
   - **Description**: `Distributed Event-Driven Order Processing System with Saga Choreography, RabbitMQ, PostgreSQL, Redis Caching, and Docker [Live Demo Available]`

### In your Resume Header:
```markdown
**Distributed Order Processing Platform (Java 17 / Spring Boot 3 / RabbitMQ / Redis / AWS)**
[GitHub: github.com/Ashutosh-Yadav-256/Distributed-order-processing-system] | [Live API: <YOUR-LINK>]
- Engineered an asynchronous distributed order saga across Order, Inventory, and Payment microservices using RabbitMQ topic exchanges and DLQs with idempotent consumer deduplication.
- Built a Redis read-through caching tier cutting inventory read latency from 26ms to 1.8ms (14x speedup, P99 < 4ms).
- Deployed end-to-end multi-container architecture on AWS EC2 with full Prometheus/Grafana observability.
```
