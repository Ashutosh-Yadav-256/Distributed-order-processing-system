resource "aws_elasticache_subnet_group" "redis_subnet_group" {
  name       = "order-platform-redis-subnets"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_security_group" "redis_sg" {
  name        = "order-platform-redis-sg"
  description = "Allow inbound Redis traffic from EKS nodes"
  vpc_id      = aws_vpc.platform_vpc.id

  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.platform_vpc.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_elasticache_replication_group" "platform_redis" {
  replication_group_id          = "order-platform-redis"
  description                   = "Redis cluster for Distributed Order Processing System"
  node_type                     = "cache.t4g.medium"
  num_cache_clusters            = 2
  port                          = 6379
  parameter_group_name          = "default.redis7"
  subnet_group_name             = aws_elasticache_subnet_group.redis_subnet_group.name
  security_group_ids            = [aws_security_group.redis_sg.id]
  automatic_failover_enabled    = true
  at_rest_encryption_enabled    = true
  transit_encryption_enabled    = false

  tags = {
    Name        = "order-platform-redis"
    Environment = var.environment
  }
}
