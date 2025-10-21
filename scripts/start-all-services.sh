#!/bin/bash
set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查Docker是否运行
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}❌ Docker未运行,请先启动Docker${NC}"
        exit 1
    fi
}

# 检查docker compose是否可用(V2版本)
check_docker_compose() {
    if ! docker compose version &> /dev/null; then
        echo -e "${RED}❌ Docker Compose未安装,请先安装Docker Compose V2${NC}"
        exit 1
    fi
}

# 等待服务健康检查通过
# 参数: $1 = 服务名称, $2 = 健康检查URL, $3 = 最大等待时间(秒)
wait_for_healthy() {
    local service_name=$1
    local health_url=$2
    local max_wait=${3:-120}
    local elapsed=0
    local interval=5

    echo -e "${YELLOW}⏳ 等待 $service_name 就绪...${NC}"

    while [ $elapsed -lt $max_wait ]; do
        # 检查容器是否运行
        if ! docker compose ps $service_name | grep -q "Up"; then
            echo -e "${RED}❌ $service_name 容器未运行${NC}"
            return 1
        fi

        # 检查健康状态
        if curl -sf "$health_url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name 已就绪 (耗时: ${elapsed}秒)${NC}"
            return 0
        fi

        echo -e "${BLUE}⏳ $service_name 尚未就绪，继续等待... (${elapsed}/${max_wait}秒)${NC}"
        sleep $interval
        elapsed=$((elapsed + interval))
    done

    echo -e "${RED}❌ $service_name 启动超时 (超过 ${max_wait}秒)${NC}"
    echo -e "${YELLOW}提示: 可以使用 'docker compose logs $service_name' 查看日志${NC}"
    return 1
}

echo "=========================================="
echo -e "${BLUE}启动 NUSHungry 微服务架构${NC}"
echo "=========================================="

# 环境检查
check_docker
check_docker_compose

# 检查 .env 文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  未找到 .env 文件,从 .env.example 复制...${NC}"
    if [ ! -f .env.example ]; then
        echo -e "${RED}❌ .env.example 文件不存在${NC}"
        exit 1
    fi
    cp .env.example .env
    echo -e "${GREEN}✅ 已创建 .env 文件,请根据需要修改配置${NC}"
fi

# 启动基础设施服务
echo ""
echo -e "${BLUE}📦 启动基础设施服务 (PostgreSQL, MongoDB, RabbitMQ, MinIO, Redis, Zipkin)...${NC}"
docker compose up -d postgres mongodb rabbitmq minio redis zipkin

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 基础设施服务启动失败${NC}"
    exit 1
fi

# 等待基础设施服务就绪
echo ""
echo -e "${YELLOW}⏳ 等待基础设施服务启动...${NC}"
sleep 20

# 检查基础设施服务健康状态
echo ""
echo -e "${BLUE}🔍 检查基础设施服务健康状态...${NC}"
docker compose ps postgres mongodb rabbitmq minio redis zipkin

# 启动配置中心
echo ""
echo -e "${BLUE}⚙️  启动配置中心 (Config Server)...${NC}"
docker compose up -d config-server

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 配置中心启动失败${NC}"
    exit 1
fi

# 等待配置中心就绪（使用健康检查）
echo ""
if ! wait_for_healthy "config-server" "http://localhost:8888/actuator/health" 120; then
    echo -e "${RED}❌ Config Server 启动失败或超时${NC}"
    echo -e "${YELLOW}查看日志: docker compose logs config-server${NC}"
    exit 1
fi

# 验证 Config Server 认证
echo ""
echo -e "${BLUE}🔐 验证 Config Server 认证...${NC}"
if curl -sf -u config:config123 "http://localhost:8888/review-service/prod" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Config Server 认证正常${NC}"
else
    echo -e "${YELLOW}⚠️  Config Server 认证验证失败，但继续启动...${NC}"
fi

# 启动服务注册中心
echo ""
echo -e "${BLUE}🔍 启动服务注册中心 (Eureka Server)...${NC}"
docker compose up -d eureka-server

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Eureka Server启动失败${NC}"
    exit 1
fi

# 等待 Eureka Server 就绪（使用健康检查）
echo ""
if ! wait_for_healthy "eureka-server" "http://localhost:8761/actuator/health" 120; then
    echo -e "${RED}❌ Eureka Server 启动失败或超时${NC}"
    echo -e "${YELLOW}查看日志: docker compose logs eureka-server${NC}"
    exit 1
fi

# 启动微服务
echo ""
echo -e "${BLUE}🚀 启动微服务...${NC}"
docker compose up -d admin-service cafeteria-service review-service media-service preference-service gateway-service

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 微服务启动失败${NC}"
    exit 1
fi

# 等待微服务健康检查（并行检查多个服务）
echo ""
echo -e "${YELLOW}⏳ 等待微服务就绪...${NC}"
echo -e "${BLUE}提示: 微服务会重试连接 Config Server，初次可能会有 401 警告（正常现象）${NC}"

# 定义需要检查的微服务及其端口
declare -A services=(
    ["admin-service"]="8082"
    ["cafeteria-service"]="8083"
    ["review-service"]="8084"
    ["media-service"]="8085"
    ["preference-service"]="8086"
)

# 等待所有微服务就绪
all_healthy=true
for service in "${!services[@]}"; do
    port=${services[$service]}
    echo ""
    if ! wait_for_healthy "$service" "http://localhost:$port/actuator/health" 180; then
        echo -e "${YELLOW}⚠️  $service 启动超时，但继续检查其他服务...${NC}"
        all_healthy=false
    fi
done

# 启动 Gateway Service
echo ""
echo -e "${BLUE}🌐 启动 Gateway Service...${NC}"
if ! wait_for_healthy "gateway-service" "http://localhost:8080/actuator/health" 120; then
    echo -e "${YELLOW}⚠️  Gateway Service 启动超时${NC}"
    all_healthy=false
fi

# 显示所有服务状态
echo ""
echo -e "${BLUE}📊 所有服务状态:${NC}"
docker compose ps

# 显示启动结果
echo ""
if [ "$all_healthy" = true ]; then
    echo -e "${GREEN}✅ 所有服务健康检查通过！${NC}"
else
    echo -e "${YELLOW}⚠️  部分服务未通过健康检查，请查看日志${NC}"
    echo -e "${YELLOW}提示: 使用 'docker compose logs -f [service-name]' 查看详细日志${NC}"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ 所有服务已启动!${NC}"
echo "=========================================="
echo ""
echo -e "${BLUE}核心服务访问地址:${NC}"
echo "  - Gateway (API入口):  http://localhost:8080"
echo "  - Eureka Server:      http://localhost:8761 (eureka/eureka)"
echo "  - Config Server:      http://localhost:8888"
echo ""
echo -e "${BLUE}微服务访问地址:${NC}"
echo "  - Admin Service:      http://localhost:8082"
echo "  - Cafeteria Service:  http://localhost:8083"
echo "  - Review Service:     http://localhost:8084"
echo "  - Media Service:      http://localhost:8085"
echo "  - Preference Service: http://localhost:8086"
echo ""
echo -e "${BLUE}基础设施管理界面:${NC}"
echo "  - RabbitMQ:  http://localhost:15672 (guest/guest)"
echo "  - MinIO:     http://localhost:9001 (minioadmin/minioadmin)"
echo "  - Zipkin:    http://localhost:9411"
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看日志: docker compose logs -f [service-name]"
echo "  停止服务: ./scripts/stop-all-services.sh"
echo "=========================================="
