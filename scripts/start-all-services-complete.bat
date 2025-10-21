@echo off
chcp 65001 >nul
echo ==========================================
echo 启动 NUSHungry 完整微服务架构（包含所有 13 个服务）
echo ==========================================

REM 检查 .env 文件
if not exist .env (
    echo ⚠️  未找到 .env 文件，从 .env.example 复制...
    copy .env.example .env
    echo ✅ 已创建 .env 文件，请根据需要修改配置
    echo.
)

REM ========== 阶段 1: 启动基础设施服务 ==========
echo.
echo 📦 [阶段 1/4] 启动基础设施服务...
echo    - PostgreSQL (5432)
echo    - MongoDB (27017)
echo    - Redis (6379)
echo    - RabbitMQ (5672, 15672)
echo    - MinIO (9000, 9001)
echo    - Zipkin (9411)
docker-compose up -d postgres mongodb redis rabbitmq minio zipkin

echo.
echo ⏳ 等待基础设施服务启动（30 秒）...
timeout /t 30 /nobreak >nul

echo.
echo 🔍 检查基础设施服务状态...
docker-compose ps postgres mongodb redis rabbitmq minio zipkin

REM ========== 阶段 2: 启动服务注册中心 ==========
echo.
echo 📊 [阶段 2/4] 启动 Eureka 服务注册中心...
echo    - Eureka Server (8761)
docker-compose up -d eureka-server

echo.
echo ⏳ 等待 Eureka 启动（60 秒）...
echo    提示：Eureka 需要较长时间初始化，请耐心等待...
timeout /t 60 /nobreak >nul

echo.
echo 🔍 检查 Eureka 状态...
docker-compose ps eureka-server

REM ========== 阶段 3: 启动业务微服务 ==========
echo.
echo 🚀 [阶段 3/4] 启动业务微服务...
echo    - Admin Service (8082)
echo    - Cafeteria Service (8083)
echo    - Review Service (8084)
echo    - Media Service (8085)
echo    - Preference Service (8086)
docker-compose up -d admin-service cafeteria-service review-service media-service preference-service

echo.
echo ⏳ 等待微服务启动并注册到 Eureka（40 秒）...
timeout /t 40 /nobreak >nul

echo.
echo 🔍 检查微服务状态...
docker-compose ps admin-service cafeteria-service review-service media-service preference-service

REM ========== 阶段 4: 启动 API 网关 ==========
echo.
echo 🌐 [阶段 4/4] 启动 API Gateway（统一入口）...
echo    - Gateway Service (8080)
docker-compose up -d gateway-service

echo.
echo ⏳ 等待 Gateway 启动（20 秒）...
timeout /t 20 /nobreak >nul

REM ========== 显示最终状态 ==========
echo.
echo ==========================================
echo 📊 所有服务最终状态：
echo ==========================================
docker-compose ps

echo.
echo ==========================================
echo ✅ 所有 13 个服务已启动！
echo ==========================================
echo.
echo 🌐 核心访问地址：
echo   - API Gateway (推荐):  http://localhost:8080
echo   - Swagger API 文档:    http://localhost:8080/swagger-ui.html
echo   - Eureka Dashboard:    http://localhost:8761 (eureka/eureka)
echo.
echo 🔧 微服务直接访问（仅调试用）：
echo   - Admin Service:       http://localhost:8082
echo   - Cafeteria Service:   http://localhost:8083
echo   - Review Service:      http://localhost:8084
echo   - Media Service:       http://localhost:8085
echo   - Preference Service:  http://localhost:8086
echo.
echo 📦 基础设施管理界面：
echo   - RabbitMQ:            http://localhost:15672 (guest/guest)
echo   - MinIO:               http://localhost:9001 (minioadmin/minioadmin)
echo   - Zipkin 追踪:         http://localhost:9411
echo.
echo 💡 提示：
echo   - 推荐通过 Gateway (8080) 访问所有 API
echo   - 查看日志: docker-compose logs -f [service-name]
echo   - 停止服务: scripts\stop-all-services.bat
echo   - 验证脚本: verify-services.bat
echo.
echo ==========================================
echo 🔍 快速验证命令：
echo ==========================================
echo   curl http://localhost:8080/actuator/health
echo   curl http://localhost:8761/eureka/apps
echo ==========================================
pause
