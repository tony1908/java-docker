#!/bin/bash

echo "Creating namespace..."
kubectl apply -f namespace.yaml

echo "Waiting for namespace to be created..."
sleep 2

echo "Deploying MariaDB..."
kubectl apply -f mariadb-deployment.yaml

echo "Waiting for MariaDB to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/mariadb -n javadb-app

echo "Deploying Java application..."
kubectl apply -f app-deployment.yaml

echo "Waiting for Java application to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/java-app -n javadb-app

echo ""
echo "Deployment completed successfully!"
echo ""
echo "To check the status of your pods:"
echo "kubectl get pods -n javadb-app"
echo ""
echo "To access the application:"
echo "kubectl port-forward service/java-app-service 8080:8080 -n javadb-app"
echo "Then visit: http://localhost:8080"