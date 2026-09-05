#!/bin/bash
# EC2 user-data: runs once on first boot (Amazon Linux 2023).
# Installs Docker and creates a swap file so the t2.micro (1 GB RAM)
# can comfortably run the JVM container.
set -euxo pipefail

# --- Docker ---
dnf update -y
dnf install -y docker
systemctl enable --now docker
usermod -aG docker ec2-user

# --- 2 GB swap (helps the 1 GB t2.micro avoid OOM) ---
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# Signal that setup finished
touch /home/ec2-user/USER_DATA_DONE
