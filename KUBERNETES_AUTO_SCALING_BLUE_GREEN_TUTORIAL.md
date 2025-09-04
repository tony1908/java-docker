# Kubernetes Auto Scaling and Blue-Green Setup - Step by Step

This is a practical guide to add auto scaling and blue-green deployment to your Kubernetes application. Follow each step in order.

## Prerequisites - Do This First

**For Minikube users:**
```bash
minikube addons enable metrics-server
```

**For other Kubernetes:**
```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Verify it's working:
```bash
kubectl top nodes
```

## Step 1: Deploy Your Application with Auto Scaling + Blue-Green

Your `k8s/app-deployment.yaml` now includes:
- Resource limits for auto scaling
- Blue-green rolling update strategy
- Health checks for safe deployments
- Version tracking

**1.1 Deploy everything**
```bash
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/app-hpa.yaml
```

**1.2 Check it's working**
```bash
kubectl get hpa -n java-app
kubectl get pods -n java-app
kubectl top pods -n java-app
```

You should see:
- 2 pods running (replicas: 2)
- HPA monitoring CPU/memory
- Health checks passing

**1.3 Understanding the HPA Configuration**

The `k8s/app-hpa.yaml` file contains:

```yaml
minReplicas: 2      # Always keep at least 2 pods
maxReplicas: 10     # Never scale beyond 10 pods
metrics:
  - cpu: 70%        # Scale up when CPU > 70%
  - memory: 80%     # Scale up when memory > 80%
```

**Scaling Behavior:**
- **Scale Up**: Max 50% increase or 2 pods per minute (fast response to load)
- **Scale Down**: Max 10% decrease per minute, waits 5 minutes (prevents flapping)

**What this means:**
- **Low traffic** → 2 pods (saves money)
- **High traffic** → Up to 10 pods (handles load)
- **Smart scaling** → Quick up, slow down

## Step 2: Deploy a New Version (Simple Blue-Green)

Your deployment now uses a simple blue-green strategy with rolling updates.

**2.1 Deploy new version**
```bash
# Update to new image version
kubectl set image deployment/app-deployment app=toony1908/java-mariadb-app:v2.0 -n java-app

# Watch the rolling update (blue-green effect)
kubectl rollout status deployment/app-deployment -n java-app

# Update version tracking
kubectl patch configmap deployment-tracker -n java-app -p '{"data":{"current-version":"v2.0","previous-version":"v1.0"}}'
```

**2.2 What happens during deployment:**
- Kubernetes creates new pods (v2.0) alongside old ones (v1.0)  
- Health checks ensure new pods are ready
- Old pods are terminated only after new pods are healthy
- Zero downtime!

**2.3 Check the deployment**
```bash
kubectl get pods -n java-app -w  # Watch pods change
kubectl get rs -n java-app       # See old and new replica sets
```

## Step 3: Rollback (If Needed)

If something goes wrong:

**3.1 Quick rollback**
```bash
kubectl rollout undo deployment/app-deployment -n java-app
```

**3.2 Check rollback status**
```bash
kubectl rollout status deployment/app-deployment -n java-app
kubectl get pods -n java-app
```

## Step 4: Check Everything is Working

**4.1 Check auto scaling**
```bash
kubectl get hpa -n java-app
kubectl get pods -n java-app
```

**4.2 Check deployment status**
```bash
# Which version is running?
kubectl get configmap deployment-tracker -n java-app -o jsonpath='{.data.current-version}'

# What pods are running?
kubectl get pods -n java-app --show-labels

# Deployment history
kubectl rollout history deployment/app-deployment -n java-app
```

## Step 5: Test Auto Scaling (Optional)

Let's see auto scaling in action by generating load.

**5.1 Create load and watch scaling**
```bash
# Terminal 1: Generate load
kubectl run load-generator --image=busybox -n java-app --rm -it --restart=Never -- /bin/sh
# Inside the pod run:
while true; do wget -q -O- http://app-service:8080; done

# Terminal 2: Watch the magic happen
kubectl get hpa -n java-app -w
kubectl get pods -n java-app -w
```

**5.2 What you'll see:**
```bash
# HPA will show increasing CPU usage
NAME      REFERENCE          TARGETS    MINPODS   MAXPODS   REPLICAS
app-hpa   Deployment/app     15%/70%    2         10        2
app-hpa   Deployment/app     85%/70%    2         10        2     # CPU spiking
app-hpa   Deployment/app     85%/70%    2         10        3     # Scaling up
app-hpa   Deployment/app     60%/70%    2         10        4     # More pods
```

**5.3 Stop load and watch scale down:**
- Stop the load generator (Ctrl+C)
- Wait 5 minutes (stabilization period)
- Watch pods slowly scale back down to 2

**5.4 Customize HPA (optional):**

To change scaling thresholds, edit the HPA:
```yaml
# More aggressive scaling
minReplicas: 1       # Scale down to 1 pod
maxReplicas: 20      # Scale up to 20 pods
metrics:
  - cpu: 50%         # Scale at 50% CPU (more sensitive)
  - memory: 60%      # Scale at 60% memory
```

Apply changes:
```bash
kubectl apply -f k8s/app-hpa.yaml
```

## Quick Reference Commands

**Deploy new version:**
```bash
kubectl set image deployment/app-deployment app=your-image:new-tag -n java-app
kubectl rollout status deployment/app-deployment -n java-app
```

**Rollback:**
```bash
kubectl rollout undo deployment/app-deployment -n java-app
```

**Check status:**
```bash
kubectl get pods,svc,hpa -n java-app
kubectl get configmap deployment-tracker -n java-app -o yaml
```

**View deployment history:**
```bash
kubectl rollout history deployment/app-deployment -n java-app
```

## Troubleshooting

**HPA shows "unknown" targets:**
```bash
kubectl top pods -n java-app  # Should show CPU/Memory usage
```

**Deployment not working:**
```bash
kubectl describe deployment app-deployment -n java-app
kubectl logs -l app=app -n java-app
```

## How This Works

**Auto Scaling (HPA):**
- Monitors CPU and memory usage every 15 seconds
- Scales up when thresholds exceeded (CPU > 70% OR Memory > 80%)
- Scales down when usage drops (waits 5 minutes to avoid flapping)
- Uses `kubectl top pods` data from metrics-server

**Blue-Green Strategy:**
- `maxSurge: 100%` = Doubles pods during update (creates new pods before removing old)
- `maxUnavailable: 0` = No downtime (keeps old pods until new are ready)  
- Health checks ensure new pods work before switching traffic
- Rolling update gradually replaces old pods with new ones

**HPA Field Explanations:**
- `minReplicas: 2` = Always keep at least 2 pods running
- `maxReplicas: 10` = Never scale beyond 10 pods (cost control)
- `averageUtilization: 70` = Scale up when average CPU across all pods > 70%
- `stabilizationWindowSeconds: 300` = Wait 5 minutes before scaling down
- `maxSurge: 100%` = Can double pod count during deployments
- `maxUnavailable: 0` = Never take down existing pods during updates

**Benefits:**
- Zero downtime deployments
- Automatic cost optimization
- Easy rollback with one command
- Simple - just update image tag
- Built-in Kubernetes features only

That's it! You now have auto scaling and blue-green deployment set up with simple kubectl commands.