# BackCam Recorder

一个支持后台运行的Android相机录像应用，用于个人活动记录（骑行、跑步等）。

## 功能特性

- **后台录制**：支持在后台持续录制，锁屏或切换应用时录制不中断
- **可选预览**：既能实时预览，也能最小化到后台继续录制
- **隐私相册**：默认开启隐私相册，视频保存到应用私有目录，系统相册不可见
- **灵活设置**：可配置分辨率(4K/1080p/720p)和帧率(60fps/30fps)
- **通知控制**：通过通知栏快捷控制录制状态

## 技术架构

| 技术 | 说明 |
|-----|------|
| CameraX | Google官方相机库，生命周期感知 |
| Foreground Service | 后台录制服务 |
| Jetpack Compose | 现代声明式UI |
| Room | 本地数据库存储 |
| Hilt | 依赖注入 |
| DataStore | 设置持久化 |

## 项目结构

```
app/
├── src/main/java/com/backcam/recorder/
│   ├── ui/                    # UI层 (Compose)
│   │   ├── screens/           # 各界面
│   │   ├── components/        # UI组件
│   │   └── navigation/        # 导航
│   ├── service/               # 后台录制服务
│   ├── viewmodel/             # ViewModel
│   ├── repository/            # 数据仓库
│   ├── data/                  # 数据层
│   │   ├── local/             # Room数据库
│   │   └── datastore/         # 设置存储
│   ├── camera/                # 相机管理
│   ├── storage/               # 存储管理
│   ├── permission/            # 权限管理
│   └── model/                 # 数据模型
│   └── util/                  # 工具类
│
└── src/main/res/              # 资源文件
```

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34
- Kotlin 1.9.23
- Gradle 8.5
- 目标设备：Android 13+ (API 33+)

## 构建与运行

```bash
# 克隆项目
git clone https://github.com/YOUR_USERNAME/BackCamRecorder.git
cd BackCamRecorder

# 构建项目
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

## 权限说明

| 权限 | 用途 |
|-----|------|
| CAMERA | 相机录制 |
| RECORD_AUDIO | 音频录制 |
| FOREGROUND_SERVICE | 后台服务 |
| POST_NOTIFICATIONS | 通知栏显示(Android 13+) |

## 开发状态

项目处于初始开发阶段，核心功能框架已搭建，待完善：

- [ ] 后台录制服务完整实现
- [ ] CameraX预览与录制绑定
- [ ] 隐私相册存储逻辑
- [ ] 系统相册导出功能
- [ ] 视频播放界面
- [ ] 单元测试与集成测试

## 设计文档

详细设计文档位于：`docs/superpowers/specs/2026-06-30-background-camera-recorder-design.md`

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request。