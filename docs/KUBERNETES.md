# Kubernetes Deployment Guide

## 🎯 Обзор

Проект использует Helm для деплоя в Kubernetes. Поддерживаются:
- **k3d** - локальная разработка
- **EKS/GKE/AKS** - production окружения

## 📁 Структура Helm Chart

```
helm/order-platform/
├── Chart.yaml              # Метаданные chart
├── values.yaml             # Значения по умолчанию
├── values-dev.yaml         # Development окружение
├── values-prod.yaml        # Production окружение
└── templates/
    ├── _helpers.tpl        # Helper функции
    ├── namespace.yaml      # Namespace
    ├── configmap.yaml      # ConfigMap
    ├── secrets.yaml        # Secrets
    ├── serviceaccount.yaml # ServiceAccount
    ├── api-gateway/        # API Gateway templates
    ├── auth-service/       # Auth Service templates
    ├── user-service/       # User Service templates
    ├── product-service/    # Product Service templates
    ├── order-service/      # Order Service templates
    ├── inventory-service/  # Inventory Service templates
    └── notification-service/ # Notification Service templates
```

## 🚀 Quick Start (k3d)

### Требования
- Docker
- kubectl
- k3d
- Helm 3.x

### Установка k3d
```bash
# macOS
brew install k3d

# Linux
curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash

# Windows (PowerShell)
choco install k3d
```

### Запуск

```bash
# 1. Создать кластер
make k3d-up

# 2. Установить Helm chart
make helm-install

# 3. Проверить статус
make k8s-status

# 4. Открыть API Gateway
make port-forward
# → http://localhost:8080
```

## 📦 Helm Commands

### Установка
```bash
# Development
helm install order-platform ./helm/order-platform \
    -n order-platform-dev \
    --create-namespace \
    -f ./helm/order-platform/values-dev.yaml

# Production
helm install order-platform ./helm/order-platform \
    -n order-platform-prod \
    --create-namespace \
    -f ./helm/order-platform/values-prod.yaml \
    --set secrets.jwtSecret=$JWT_SECRET \
    --set secrets.postgresPassword=$POSTGRES_PASSWORD
```

### Обновление
```bash
helm upgrade order-platform ./helm/order-platform \
    -n order-platform-dev \
    -f ./helm/order-platform/values-dev.yaml
```

### Удаление
```bash
helm uninstall order-platform -n order-platform-dev
```

### Отладка
```bash
# Просмотр сгенерированных манифестов
helm template order-platform ./helm/order-platform \
    -f ./helm/order-platform/values-dev.yaml

# Dry-run установки
helm install order-platform ./helm/order-platform \
    -n order-platform-dev \
    --dry-run --debug
```

## 🔧 Конфигурация

### values.yaml - основные параметры

```yaml
global:
  namespace: order-platform        # Kubernetes namespace
  imagePullPolicy: IfNotPresent    # Image pull policy
  imageRegistry: ghcr.io/your-org  # Docker registry
  imageTag: latest                 # Default image tag

# Отключение компонентов
postgresql:
  enabled: true
mongodb:
  enabled: true
redis:
  enabled: true
kafka:
  enabled: true

# Микросервисы
apiGateway:
  enabled: true
  replicaCount: 2
  resources:
    requests:
      memory: "256Mi"
      cpu: "200m"
```

### Окружения

| Файл | Описание |
|------|----------|
| `values.yaml` | Базовые значения |
| `values-dev.yaml` | Development (1 replica, low resources) |
| `values-prod.yaml` | Production (HA, autoscaling) |

### Secrets

```bash
# Установка secrets через CLI
helm install order-platform ./helm/order-platform \
    --set secrets.jwtSecret="your-32-char-secret" \
    --set secrets.postgresPassword="secure-password"

# Или через External Secrets Operator
kubectl apply -f external-secrets.yaml
```

## 📊 Архитектура в Kubernetes

```
                         ┌─────────────────┐
                         │    Ingress      │
                         │  (nginx/traefik)│
                         └────────┬────────┘
                                  │
                         ┌────────▼────────┐
                         │   API Gateway   │
                         │   (2 replicas)  │
                         └────────┬────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼───────┐  ┌──────────────▼──────────────┐  ┌───────▼───────┐
│  Auth Service │  │       Core Services         │  │  Notification │
│  (2 replicas) │  │ User, Product, Order, Inv.  │  │   (1 replica) │
└───────────────┘  └──────────────────────────────┘  └───────────────┘
        │                         │                         │
        └─────────────────────────┼─────────────────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼───────┐  ┌──────────────▼──────────────┐  ┌───────▼───────┐
│   PostgreSQL  │  │          Kafka             │  │    MongoDB    │
│   (Bitnami)   │  │        (Bitnami)           │  │   (Bitnami)   │
└───────────────┘  └─────────────────────────────┘  └───────────────┘
                                  │
                         ┌───────▼───────┐
                         │     Redis     │
                         │   (Bitnami)   │
                         └───────────────┘
```

## 🔍 Мониторинг

### Logs
```bash
# Все сервисы
kubectl logs -f -n order-platform-dev -l app.kubernetes.io/instance=order-platform

# Конкретный сервис
kubectl logs -f -n order-platform-dev -l app.kubernetes.io/name=order-service
```

### Health Checks
```bash
# Проверка health endpoints
kubectl exec -it -n order-platform-dev deploy/order-platform-api-gateway -- \
    curl localhost:8080/actuator/health
```

### Port Forwarding
```bash
# API Gateway
kubectl port-forward -n order-platform-dev svc/order-platform-api-gateway 8080:8080

# PostgreSQL
kubectl port-forward -n order-platform-dev svc/order-platform-postgresql 5432:5432

# Kafka
kubectl port-forward -n order-platform-dev svc/order-platform-kafka 9092:9092
```

## 🚀 Production Deployment

### Checklist
- [ ] Изменить `imageRegistry` на production registry
- [ ] Установить secrets через External Secrets или Vault
- [ ] Включить TLS в Ingress
- [ ] Настроить resource limits
- [ ] Включить autoscaling для критичных сервисов
- [ ] Настроить PodDisruptionBudgets
- [ ] Включить metrics для Prometheus

### External Secrets (AWS)
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: order-platform-secrets
spec:
  secretStoreRef:
    kind: ClusterSecretStore
    name: aws-secrets-manager
  target:
    name: order-platform-secrets
  data:
    - secretKey: jwt-secret
      remoteRef:
        key: order-platform/jwt-secret
    - secretKey: postgres-password
      remoteRef:
        key: order-platform/postgres-password
```

### Autoscaling
```yaml
# values-prod.yaml
apiGateway:
  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70
```

## 🔧 Troubleshooting

### Pods не запускаются
```bash
# Проверить события
kubectl describe pod -n order-platform-dev <pod-name>

# Проверить логи init containers
kubectl logs -n order-platform-dev <pod-name> -c <init-container-name>
```

### База данных недоступна
```bash
# Проверить PostgreSQL
kubectl get pods -n order-platform-dev -l app.kubernetes.io/name=postgresql

# Проверить secrets
kubectl get secret -n order-platform-dev order-platform-secrets -o yaml
```

### Kafka не работает
```bash
# Проверить Kafka pods
kubectl get pods -n order-platform-dev -l app.kubernetes.io/name=kafka

# Проверить Zookeeper
kubectl get pods -n order-platform-dev -l app.kubernetes.io/name=zookeeper
```

## 📚 Дополнительные ресурсы

- [Helm Documentation](https://helm.sh/docs/)
- [k3d Documentation](https://k3d.io/)
- [Bitnami Helm Charts](https://github.com/bitnami/charts)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)

