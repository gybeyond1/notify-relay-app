#!/bin/bash
# 编译 NotifyRelay APK
# 需要: Android SDK 或 Docker

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/notify-relay-app"

echo "🔨 开始编译 NotifyRelay APK..."
echo "📁 项目目录: $PROJECT_DIR"

# 检查是否有 Android SDK
if [ -n "$ANDROID_HOME" ] || [ -n "$ANDROID_SDK_ROOT" ]; then
    echo "✅ 检测到 Android SDK"
    echo "🧹 清理旧构建..."
    cd "$PROJECT_DIR"
    ./gradlew clean
    
    echo "🔧 编译 Debug APK..."
    ./gradlew assembleDebug
    
    echo "📦 编译 Release APK（需要签名密钥）..."
    if [ -f "$SCRIPT_DIR/keystore.properties" ]; then
        ./gradlew assembleRelease
    else
        echo "⚠️  未找到 keystore.properties，跳过 Release 编译"
        echo "   如需签名编译，请创建 keystore.properties 文件"
    fi
    
    echo "✅ 编译完成！"
    echo "📱 APK 位置:"
    ls -lh "$PROJECT_DIR/app/build/outputs/apk/"*/output.json 2>/dev/null || true
    find "$PROJECT_DIR/app/build/outputs/apk" -name "*.apk" -type f
    
else
    echo "❌ 未检测到 Android SDK"
    echo ""
    echo "请选择一种编译方式："
    echo ""
    echo "方式1: 使用 Docker（推荐）"
    echo "  docker run --rm -v $PROJECT_DIR:/app -w /app ghcr.io/srevinsaju/androidsdk:latest bash -c './gradlew assembleDebug'"
    echo ""
    echo "方式2: 安装 Android SDK"
    echo "  参考: https://developer.android.com/studio"
    echo ""
    echo "方式3: 使用 GitHub Actions（零配置）"
    echo "  1. 将项目推送到 GitHub"
    echo "  2. 创建 .github/workflows/build.yml（见下方）"
    echo "  3. 推代码后自动编译，Artifacts 里下载 APK"
fi
