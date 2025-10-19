# Review Service 部署文档

## 目录
- [部署架构](#部署架构)
- [环境要求](#环境要求)
- [本地开发部署](#本地开发部署)
- [生产环境部署](#生产环境部署)
- [健康检查和监控](#健康检查和监控)
- [服务注册和发现](#服务注册和发现)
- [故障排查](#故障排查)
- [回滚方案](#回滚方案)

---

## 部署架构

```
┌─────────────────────────────────────────────────────────┐
│                    Load Balancer                         │
│                   (Nginx/ALB/Traefik)                    │
└──────────────────────┬──────────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
    ┌────▼────┐   ┌────▼────┐  ┌────▼────┐
    │ Review  │   │ Review  │  │ Review  │
    │ Service │   │ Service │  │ Service │
    │Instance1│   │Instance2│  │Instance3│
    └────┬────┘   └────┬────┘  └────┬────┘
         │             │             │
         └─────────────┼─────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
    ┌────▼────┐   ┌────▼────┐  ┌────▼────┐
    │ MongoDB │   │RabbitMQ │  │ Consul  │
    │ Cluster │   │ Cluster │  │  /Eureka│
    └─────────┘   └─────────┘  └─────────┘
```

---

## 环境要求

### 硬件要求

| 环境 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 开发环境 | 2 Core | 4 GB | 20 GB |
| 测试环境 | 4 Core | 8 GB | 50 GB |
| 生产环境 | 8 Core | 16 GB | 200 GB (SSD) |

### 软件要求

- **Java**: JDK 17+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+ (可选)
- **MongoDB**: 7.0+
- **RabbitMQ**: 3.12+
- **Maven**: 3.9+ (构建时)

---

## 本地开发部署

### 方式1: Docker Compose (推荐)

**步骤1**: 克隆代码
```bash
cd nushungry-Backend/review-service
```

**步骤2**: 启动所有服务
```bash
# Linux/Mac
./scripts/start-services.sh

# Windows
scripts\start-services.bat
```

**步骤3**: 验证服务
```bash
# 健康检查
curl http://localhost:8084/actuator/health

# API 文档
open http://localhost:8084/swagger-ui.html

# RabbitMQ 管理界面
open http://localhost:15672
# 账号: admin / password123
```

**步骤4**: 停止服务
```bash
# Linux/Mac
./scripts/stop-services.sh

# Windows
scripts\stop-services.bat
```

### 方式2: 手动启动

**步骤1**: 启动 MongoDB
```bash
docker run -d --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=password123 \
  -v mongodb_data:/data/db \
  mongo:7.0
```

**步骤2**: 启动 RabbitMQ
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=password123 \
  rabbitmq:3.12-management-alpine
```

**步骤3**: 启动应用
```bash
# 设置环境变量
export SPRING_PROFILES_ACTIVE=dev

# 启动应用
mvn spring-boot:run
```

---

## 生产环境部署

### 方式1: Docker 部署

**步骤1**: 构建镜像
```bash
cd review-service

# 构建镜像
docker build -t nushungry/review-service:1.0.0 .

# 推送到镜像仓库（可选）
docker tag nushungry/review-service:1.0.0 registry.example.com/nushungry/review-service:1.0.0
docker push registry.example.com/nushungry/review-service:1.0.0
```

**步骤2**: 准备环境变量文件 `.env.prod`
```bash
# MongoDB 配置
MONGODB_HOST=mongodb-prod.example.com
MONGODB_PORT=27017
MONGODB_DATABASE=review_service
MONGODB_USERNAME=review_user
MONGODB_PASSWORD=<strong-password>

# RabbitMQ 配置
RABBITMQ_HOST=rabbitmq-prod.example.com
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=review_user
RABBITMQ_PASSWORD=<strong-password>
RABBITMQ_VHOST=/review

# JVM 配置
JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Swagger 配置
SWAGGER_ENABLED=false

# CORS 配置
ALLOWED_ORIGINS=https://nushungry.com,https://www.nushungry.com
```

**步骤3**: 启动容器
```bash
docker run -d \
  --name review-service \
  --restart unless-stopped \
  -p 8084:8084 \
  -p 9084:9084 \
  --env-file .env.prod \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /var/log/review-service:/var/log/review-service \
  nushungry/review-service:1.0.0
```

**步骤4**: 验证部署
```bash
# 健康检查
curl http://localhost:9084/actuator/health

# 查看日志
docker logs -f review-service

# 查看指标
curl http://localhost:9084/actuator/metrics
```

### 方式2: Kubernetes 部署

**步骤1**: 创建 Kubernetes 资源文件

`k8s/deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: review-service
  labels:
    app: review-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: review-service
  template:
    metadata:
      labels:
        app: review-service
    spec:
      containers:
      - name: review-service
        image: registry.example.com/nushungry/review-service:1.0.0
        ports:
        - containerPort: 8084
          name: http
        - containerPort: 9084
          name: management
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: MONGODB_HOST
          valueFrom:
            configMapKeyRef:
              name: review-config
              key: mongodb.host
        - name: MONGODB_USERNAME
          valueFrom:
            secretKeyRef:
              name: review-secret
              key: mongodb.username
        - name: MONGODB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: review-secret
              key: mongodb.password
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 9084
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 9084
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: review-service
spec:
  selector:
    app: review-service
  ports:
  - name: http
    port: 8084
    targetPort: 8084
  - name: management
    port: 9084
    targetPort: 9084
  type: ClusterIP
```

**步骤2**: 创建 ConfigMap 和 Secret
```bash
# ConfigMap
kubectl create configmap review-config \
  --from-literal=mongodb.host=mongodb-service \
  --from-literal=rabbitmq.host=rabbitmq-service

# Secret
kubectl create secret generic review-secret \
  --from-literal=mongodb.username=review_user \
  --from-literal=mongodb.password=<password> \
  --from-literal=rabbitmq.username=review_user \
  --from-literal=rabbitmq.password=<password>
```

**步骤3**: 部署应用
```bash
kubectl apply -f k8s/deployment.yaml
```

**步骤4**: 验证部署
```bash
# 查看 Pod 状态
kubectl get pods -l app=review-service

# 查看日志
kubectl logs -l app=review-service -f

# 端口转发（测试）
kubectl port-forward svc/review-service 8084:8084
```

---

## 健康检查和监控

### 健康检查端点

| 端点 | 说明 | 用途 |
|------|------|------|
| `/actuator/health` | 总体健康状态 | 负载均衡器健康检查 |
| `/actuator/health/liveness` | 存活探针 | Kubernetes Liveness Probe |
| `/actuator/health/readiness` | 就绪探针 | Kubernetes Readiness Probe |
| `/actuator/info` | 服务信息 | 版本和构建信息 |
| `/actuator/metrics` | Prometheus 指标 | 监控指标采集 |

### 监控指标

**关键指标**:
- `jvm.memory.used`: JVM 内存使用
- `jvm.gc.pause`: GC 暂停时间
- `http.server.requests`: HTTP 请求统计
- `mongodb.driver.pool.size`: MongoDB 连接池大小
- `rabbitmq.channels`: RabbitMQ 通道数量

**示例: Prometheus 配置**
```yaml
scrape_configs:
  - job_name: 'review-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['review-service:9084']
```

### 日志配置

**日志位置**: `/var/log/review-service/application.log`

**日志级别**:
- 生产环境: `INFO`
- 开发环境: `DEBUG`

**日志轮转**: 单文件最大 100MB，保留 30 天

---

## 服务注册和发现

### Consul 集成（推荐）

**步骤1**: 添加 Consul 依赖（可选配置）
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
```

**步骤2**: 配置 Consul
```yaml
spring:
  cloud:
    consul:
      host: consul-server
      port: 8500
      discovery:
        service-name: review-service
        health-check-path: /actuator/health
        health-check-interval: 10s
        instance-id: ${spring.application.name}:${random.value}
```

### 负载均衡配置

**Nginx 示例**:
```nginx
upstream review-service {
    least_conn;
    server review-service-1:8084 max_fails=3 fail_timeout=30s;
    server review-service-2:8084 max_fails=3 fail_timeout=30s;
    server review-service-3:8084 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.nushungry.com;

    location /api/reviews {
        proxy_pass http://review-service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 超时配置
        proxy_connect_timeout 10s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
        
        # 健康检查
        proxy_next_upstream error timeout http_502 http_503 http_504;
    }
}
```

---

## 故障排查

### 常见问题

#### 1. 服务无法启动

**症状**: 容器启动后立即退出

**排查步骤**:
```bash
# 查看容器日志
docker logs review-service

# 检查配置文件
docker exec review-service cat /app/application-prod.yml

# 检查环境变量
docker exec review-service env | grep MONGODB
```

**可能原因**:
- MongoDB 连接失败
- RabbitMQ 连接失败
- 端口被占用
- 配置错误

#### 2. MongoDB 连接超时

**排查步骤**:
```bash
# 测试 MongoDB 连接
docker exec -it mongodb mongosh -u admin -p password123

# 检查网络连通性
docker exec review-service ping mongodb

# 查看 MongoDB 日志
docker logs mongodb
```

**解决方案**:
- 检查 MongoDB 服务状态
- 检查防火墙规则
- 检查用户名密码是否正确
- 增加连接超时时间

#### 3. RabbitMQ 消息发送失败

**排查步骤**:
```bash
# 查看 RabbitMQ 队列
curl -u admin:password123 http://localhost:15672/api/queues

# 查看连接状态
curl -u admin:password123 http://localhost:15672/api/connections

# 查看日志
docker logs review-service | grep RabbitMQ
```

**解决方案**:
- 检查 RabbitMQ 服务状态
- 检查交换机和队列是否创建
- 检查路由键配置
- 启用消息重试机制

#### 4. 内存不足 (OOM)

**排查步骤**:
```bash
# 查看内存使用
docker stats review-service

# 查看 JVM 堆转储
docker exec review-service jmap -heap 1
```

**解决方案**:
- 增加容器内存限制
- 调整 JVM 参数 (`-Xms`, `-Xmx`)
- 启用 GC 日志分析
- 检查内存泄漏

---

## 回滚方案

### 场景1: Docker 部署回滚

**步骤1**: 停止当前版本
```bash
docker stop review-service
docker rm review-service
```

**步骤2**: 启动旧版本
```bash
docker run -d \
  --name review-service \
  --restart unless-stopped \
  -p 8084:8084 \
  --env-file .env.prod \
  -e SPRING_PROFILES_ACTIVE=prod \
  nushungry/review-service:0.9.0  # 旧版本
```

**步骤3**: 验证服务
```bash
curl http://localhost:8084/actuator/health
```

### 场景2: Kubernetes 部署回滚

**快速回滚**:
```bash
# 回滚到上一个版本
kubectl rollout undo deployment/review-service

# 回滚到指定版本
kubectl rollout undo deployment/review-service --to-revision=2

# 查看回滚状态
kubectl rollout status deployment/review-service
```

**查看历史版本**:
```bash
kubectl rollout history deployment/review-service
```

### 场景3: 数据库回滚

**MongoDB 数据回滚**:
```bash
# 1. 停止写入（切换服务到只读模式）
# 2. 从备份恢复数据
mongorestore --uri="mongodb://admin:password@localhost:27017" --drop /backup/review_service_backup

# 3. 验证数据
mongosh mongodb://localhost:27017/review_service
db.review.countDocuments()
```

---

## 部署清单 (Checklist)

### 部署前检查
- [ ] 代码已合并到主分支
- [ ] 所有测试通过
- [ ] 构建镜像成功
- [ ] 环境变量已配置
- [ ] 数据库已初始化
- [ ] RabbitMQ 队列已创建
- [ ] 负载均衡器已配置
- [ ] 监控告警已配置

### 部署中检查
- [ ] 服务启动成功
- [ ] 健康检查通过
- [ ] 日志无错误
- [ ] 数据库连接正常
- [ ] RabbitMQ 连接正常
- [ ] API 响应正常

### 部署后验证
- [ ] 冒烟测试通过
- [ ] 监控指标正常
- [ ] 日志输出正常
- [ ] 性能测试通过
- [ ] 负载均衡正常
- [ ] 备份任务运行

---

## 联系方式

如遇部署问题，请联系：
- 开发团队: dev-team@nushungry.com
- 运维团队: ops-team@nushungry.com
- 紧急联系: oncall@nushungry.com
