#!/bin/bash
# 使用 Docker 编译 NotifyRelay APK
# 基于 ghcr.io/srevinsaju/androidsdk 镜像

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/notify-relay-app" && pwd)"
IMAGE="ghcr.io/srevinsaju/androidsdk:latest"

echo "🐳 使用 Docker 编译 APK..."
echo "📁 项目目录: $PROJECT_DIR"

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 拉取镜像（如果不存在）
echo "🔄 拉取 Android SDK Docker 镜像..."
docker pull "$IMAGE" 2>/dev/null || true

# 运行编译
echo "🔨 开始编译..."
docker run --rm \
    -v "$PROJECT_DIR:/app" \
    -w /app \
    "$IMAGE" \
    bash -c "yes | sdkmanager --licenses && ./gradlew assembleDebug"

echo ""
echo "✅ 编译完成！"
echo "📱 APK 位置:"
find "$PROJECT_DIR/app/build/outputs/apk" -name "*.apk" -type f
