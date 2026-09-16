#!/usr/bin/env bash
# AWS EC2 User Data script for Amazon Linux 2023 / Ubuntu 22.04
# Automatically provisions Docker, Docker Compose, Swap memory, and firewall.

set -e

echo "=== 1. Allocating 4GB Swap Space for Free Tier EC2 (prevents OOM on Java apps) ==="
if [ ! -f /swapfile ]; then
    fallocate -l 4G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=4096
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    echo "Swap allocated successfully."
fi

echo "=== 2. Installing Docker & Docker Compose ==="
if command -v dnf &> /dev/null; then
    # Amazon Linux 2023
    dnf update -y
    dnf install -y docker git
    systemctl enable --now docker
    usermod -aG docker ec2-user
    # Install Compose v2
    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
    ln -s /usr/local/lib/docker/cli-plugins/docker-compose /usr/bin/docker-compose || true
elif command -v apt-get &> /dev/null; then
    # Ubuntu
    apt-get update -y
    apt-get install -y docker.io docker-compose-v2 git curl
    systemctl enable --now docker
    usermod -aG docker ubuntu
fi

echo "=== 3. EC2 Bootstrap Complete! ==="
echo "Now SSH into this instance and run:"
echo "git clone https://github.com/Ashutosh-Yadav-256/Distributed-order-processing-system.git"
echo "cd Distributed-order-processing-system"
echo "./infrastructure/aws/ec2-deploy.sh"
