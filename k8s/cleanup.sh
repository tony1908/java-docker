#!/bin/bash

echo "Cleaning up Kubernetes resources..."

echo "Deleting application deployment..."
kubectl delete -f app-deployment.yaml

echo "Deleting MariaDB deployment..."
kubectl delete -f mariadb-deployment.yaml

echo "Deleting namespace..."
kubectl delete -f namespace.yaml

echo "Cleanup completed!"