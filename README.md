# 通知中继 APK 编译指南

## 项目结构

```
notify-relay-app/
├── app/
│   ├── src/main/java/com/notifyrelay/
│   │   ├── MainActivity.kt           # 主界面，模式选择
│   │   ├── NotificationService.kt   # 通知监听服务（发送端）
│   │   ├── WebSocketService.kt      # WebSocket 服务（接收端）
│   │   └── NotifyDismissReceiver.kt # 通知点击处理
│   ├── src/main/res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml    # 模式选择界面
│   │   │   └── activity_config.xml  # 配置界面
│   │   └── values/
│   │       ├── strings.xml
│   │       └── colors.xml
│   └── build.gradle
├── build.gradle (项目级)
├── settings.gradle
├── build.sh              # 编译脚本（需要 Android SDK）
├── build-docker.sh       # Docker 编译脚本
└── .github/workflows/
    └── build.yml         # GitHub Actions 自动编译
```

## 功能特性

| 功能 | 说明 |
|------|------|
| **三种模式** | 发送端 / 接收端 / 双端同步 |
| **通知监听** | 自动读取手机通知并转发 |
| **WebSocket 实时** | 低延迟，双向通信 |
| **离线缓存** | 断线重连后自动补拉历史 |
| **多设备同步** | 任意节点发送，所有接收端同时收到 |
| **中文界面** | 全中文，无任何洋词 |

## 编译方式

### 方式1: GitHub Actions（最推荐，零配置）

1. 将 `notify-relay-app` 目录推送到 GitHub 仓库
2. 在仓库页面 → Actions → Build APK → Run workflow
3. 等待编译完成（约 2-3 分钟）
4. 在 Artifacts 里下载 `NotifyRelay-Debug.zip`
5. 解压得到 APK，传到手机安装

### 方式2: Docker 编译

```bash
# 确保 Docker 已安装
cd /opt/data/notify-relay-app
bash build-docker.sh
```

编译完成的 APK 在：
```
/opt/data/notify-relay-app/app/build/outputs/apk/debug/app-debug.apk
```

### 方式3: 本地 Android Studio

1. 安装 [Android Studio](https://developer.android.com/studio)
2. 打开 `notify-relay-app` 目录作为项目
3. 等待 Gradle 同步完成
4. Build → Build Bundle(s)/APK(s) → Build APK(s)
5. 在 `app/build/outputs/apk/debug/` 找到 APK

## 安装到手机

1. 手机开启「未知来源」安装权限
2. 通过 USB 或文件管理器把 APK 传到手机
3. 点击安装

## 首次配置

1. 打开 App，选择工作模式
2. 填写服务器地址（如 `http://192.168.1.100:8000`）
3. 填写 Token（与 NAS 服务端一致）
4. 点击「连接」

### 发送端额外配置

- 打开系统设置 → 通知管理 → 找到「通知中继」→ 开启「通知监听权限」
- 允许 App 后台运行（电池优化白名单）

### 接收端额外配置

- 开启「显示通知」权限
- 允许 App 后台运行

## 注意事项

- **最低 Android 版本**: Android 8.0 (API 26)
- **网络要求**: 手机和 NAS 需在同一局域网，或 NAS 有公网 IP
- **Token 安全**: 请设置强密码，避免未授权访问
- **耗电**: WebSocket 长连接会消耗一定电量，建议开启电池优化白名单

## 与服务端配合

确保 NAS 上的服务端已启动：
```bash
cd /opt/data/scripts
python3 notify_server.py
```

或使用 Docker：
```bash
docker run -d --name notify-relay -p 8000:8000 \
  -v /opt/data/notify:/app/data \
  python:3.11-slim \
  sh -c "pip install fastapi uvicorn pydantic && python /app/notify_server.py"
```

## 故障排查

| 问题 | 解决 |
|------|------|
| 连接失败 | 检查 NAS 地址是否正确，手机能否访问 `http://NAS:8000/health` |
| 收不到通知 | 检查 Token 是否一致，WebSocket 是否连接成功 |
| 发送端没反应 | 检查通知监听权限是否开启 |
| 应用闪退 | 查看 Logcat 日志，确认 Android 版本 >= 8.0 |

---

**开发者**: Hermes Agent (本小姐亲自写的，代码风格怎么样？😏)
