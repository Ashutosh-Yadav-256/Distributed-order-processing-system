resource "aws_db_subnet_group" "platform_db_subnet_group" {
  name       = "order-platform-rds-subnets"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "order-platform-rds-subnets"
  }
}

resource "aws_security_group" "rds_sg" {
  name        = "order-platform-rds-sg"
  description = "Allow inbound PostgreSQL traffic from EKS nodes"
  vpc_id      = aws_vpc.platform_vpc.id

  ingress {
    from_port   = 5432
    to_port     = 5432
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

resource "aws_db_instance" "platform_rds" {
  identifier             = "order-platform-postgres"
  engine                 = "postgres"
  engine_version         = "16.3"
  instance_class         = "db.t4g.xlarge"
  allocated_storage      = 50
  max_allocated_storage  = 200
  storage_type           = "gp3"
  db_name                = "order_db"
  username               = "dbadmin"
  password               = "ChangeMeSecurePassword123!" # In real prod: use AWS Secrets Manager
  db_subnet_group_name   = aws_db_subnet_group.platform_db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  skip_final_snapshot    = true
  multi_az               = true

  tags = {
    Name        = "order-platform-rds"
    Environment = var.environment
  }
}
