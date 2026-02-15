# Infrastructure as Code (IaC)

Infrastructure as Code makes your environments reproducible, auditable, and consistent. This chapter covers a practical baseline for provisioning and managing production infrastructure for a NestJS API.

## Goals

- Provision repeatable environments
- Separate dev, staging, and prod safely
- Store and review infra changes in Git

## Core Concepts

- Use Terraform or Pulumi to define cloud resources.
- Use Kubernetes manifests or Helm charts for deployments.
- Use remote state for team collaboration.
- Avoid manual changes in the cloud console.

## Recommended Repo Layout

```
infra/
  environments/
    dev/
    staging/
    prod/
  modules/
    network/
    compute/
    database/
    observability/
  apps/
    api/
```

## Environment Promotion

1. Apply infrastructure changes in dev.
2. Promote the same change set to staging.
3. Deploy to prod only after checks pass.

Use separate state backends per environment to avoid accidental overlap.

## Example: Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: blog-api
  template:
    metadata:
      labels:
        app: blog-api
    spec:
      containers:
        - name: api
          image: registry.example.com/blog-api:1.0.0
          ports:
            - containerPort: 3000
          env:
            - name: NODE_ENV
              value: "production"
          resources:
            requests:
              cpu: "200m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
```

## Example: Terraform Skeleton

```hcl
# infra/environments/prod/main.tf
module "network" {
  source = "../modules/network"
}

module "database" {
  source = "../modules/database"
  db_name = "blog"
}
```

## Secrets and Config

- Use a secrets manager instead of plaintext env files in production.
- Mount secrets at runtime and never commit them to Git.

## Production Checklist

- Use separate accounts or projects per environment.
- Enable audit logs and access controls.
- Review and approve infra changes through pull requests.

---

[Previous: NestJS 11 Features](./23-nestjs-11-features.md) | [Back to Index](./README.md) | [Next: SRE and Operations ->](./25-sre-operations.md)
