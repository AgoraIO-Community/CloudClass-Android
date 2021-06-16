_[English](README.md) | 中文_

本文指导你运行 Android 示例项目。

## 如何运行示例项目

### 前提条件

- 准备工作：请确保你已经完成 [前提条件](https://docs.agora.io/cn/agora-class/agora_class_prep?platform=Android) 中的准备工作。
- 开发环境：
  - JDK
  - Android Studio 2.0  及以上
- Android 设备。部分模拟机可能存在功能缺失或者性能问题，所以推荐使用真机。

### 运行示例项目
- 在声网控制台获取App ID, App Certificate并配置进string_config.xml中。
- string_config.xml中的两个host相关配置可忽略。
- 同步并运行工程。
> *但是我们极力不推荐这种本地生成rtmToken的不安全方案，具体安全方案请参考[生成 RTM Token](https://docs.agora.io/cn/agora-class/agora_class_prep#step5)。

   > 参考 [校验用户权限](https://docs.agora.io/cn/Agora%20Platform/token) 了解如何获取 App ID 和 Token。你可以获取一个临时 token，快速运行示例项目。
   >
   > 生成 Token 使用的频道名必须和加入频道时使用的频道名一致。

   > 为提高项目的安全性，Agora 使用 Token（动态密钥）对即将加入频道的用户进行鉴权。
   >
   > 临时 Token 仅作为演示和测试用途。在生产环境中，你需要自行部署服务器签发 Token，详见[生成 Token](https://docs.agora.io/cn/Interactive%20Broadcast/token_server)。

### 手动接入SDK
- 参考[快速接入](https://docs.agora.io/cn/agora-class/agora_class_quickstart_android?platform=Android)


## 反馈

如果你有任何问题或建议，可以通过 [issue](https://github.com/AgoraIO-Community/CloudClass-Android/issues) 的形式反馈。

## 参考文档

- [声网灵动课堂产品概述](https://docs.agora.io/cn/agora-class/product_agora_class?platform=Android)
- [Classroom SDK API 参考](https://docs.agora.io/cn/agora-class/agora_class_api_ref_android?platform=Android)

## 相关资源

- 你可以先参阅 [常见问题](https://docs.agora.io/cn/faq)
- 如果你想了解更多官方示例，可以参考 [官方 SDK 示例](https://github.com/AgoraIO)
- 如果你想了解声网 SDK 在复杂场景下的应用，可以参考 [官方场景案例](https://github.com/AgoraIO-usecase)
- 如果你想了解声网的一些社区开发者维护的项目，可以查看 [社区](https://github.com/AgoraIO-Community)
- 若遇到问题需要开发者帮助，你可以到 [开发者社区](https://rtcdeveloper.com/) 提问
- 如果需要售后技术支持, 你可以在 [Agora Dashboard](https://dashboard.agora.io) 提交工单

## 代码许可

示例项目遵守 MIT 许可证。