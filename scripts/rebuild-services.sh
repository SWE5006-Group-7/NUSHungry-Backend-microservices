#!/bin/bash
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ========================================
# 服务列表配置
# ========================================
# 需要重新构建和启动的微服务列表
# 可选服务: admin-service cafeteria-service review-service media-service preference-service gateway-service
SERVICES_TO_REBUILD=(
)

# 如果列表为空,则重新构建所有服务
if [ ${#SERVICES_TO_REBUILD[@]} -eq 0 ]; then
    SERVICES_TO_REBUILD=(
        "admin-service"
        "cafeteria-service"
        "review-service"
        "media-service"
        "preference-service"
        "gateway-service"
    )
fi

echo "=========================================="
echo -e "${BLUE}重新构建 NUSHungry 微服务${NC}"
echo "=========================================="
echo -e "${YELLOW}将要重新构建的服务:${NC}"
for service in "${SERVICES_TO_REBUILD[@]}"; do
    echo "  - $service"
done
echo ""

# 停止所有服务
echo -e "${YELLOW}🛑 停止所有服务...${NC}"
docker compose down

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠️  部分服务停止失败,继续清理...${NC}"
fi

# 强制清理可能残留的容器
echo -e "${YELLOW}🧹 清理残留容器...${NC}"
docker rm -f $(docker ps -aq --filter "name=nushungry-") 2>/dev/null || true

echo ""
echo -e "${GREEN}✅ 服务已停止${NC}"
echo ""

# 重新构建指定的服务
echo -e "${BLUE}🔨 重新构建微服务镜像...${NC}"
echo -e "${YELLOW}⚠️  这可能需要几分钟时间...${NC}"
echo ""

docker compose build --no-cache "${SERVICES_TO_REBUILD[@]}"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 构建失败${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ 镜像构建完成${NC}"
echo ""

# 启动基础设施服务
echo -e "${BLUE}📦 启动基础设施服务...${NC}"
docker compose up -d postgres mongodb rabbitmq minio redis zipkin

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 基础设施服务启动失败${NC}"
    exit 1
fi

echo -e "${YELLOW}⏳ 等待基础设施服务就绪 (20秒)...${NC}"
sleep 20

# 启动配置中心
echo ""
echo -e "${BLUE}⚙️  启动配置中心...${NC}"
docker compose up -d config-server

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 配置中心启动失败${NC}"
    exit 1
fi

echo -e "${YELLOW}⏳ 等待配置中心就绪 (15秒)...${NC}"
sleep 15

# 启动 Eureka
echo ""
echo -e "${BLUE}🔍 启动服务注册中心...${NC}"
docker compose up -d eureka-server

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Eureka Server启动失败${NC}"
    exit 1
fi

echo -e "${YELLOW}⏳ 等待 Eureka 就绪 (20秒)...${NC}"
sleep 20

# 启动指定的微服务
echo ""
echo -e "${BLUE}🚀 启动微服务...${NC}"
docker compose up -d "${SERVICES_TO_REBUILD[@]}"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 微服务启动失败${NC}"
    exit 1
fi

echo -e "${YELLOW}⏳ 等待微服务启动 (30秒)...${NC}"
sleep 30

# 显示服务状态
echo ""
echo -e "${BLUE}📊 所有服务状态:${NC}"
docker compose ps

echo ""
echo "=========================================="
echo -e "${GREEN}✅ 重新构建和启动完成!${NC}"
echo "=========================================="
echo ""
echo -e "${BLUE}查看日志命令:${NC}"
echo "  docker compose logs -f media-service"
echo "  docker compose logs -f [service-name]"
echo ""
echo -e "${BLUE}检查特定服务:${NC}"
echo "  docker compose ps media-service"
echo ""
