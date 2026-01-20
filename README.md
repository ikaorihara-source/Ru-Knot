# 露刻 (Ru-Knot) 🕯️

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)
![Android](https://img.shields.io/badge/Android-Jetpack_Compose-green.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**露刻** 是一款为 **折原露露 (Orihara Ruru)** 粉丝打造的 Android 专属闹钟与直播监控 App。
(Ru-Knot is a fan-made alarm clock and stream monitor app for Orihara Ruru.)

## ✨ 功能特点 (Features)

* **直播监控**：实时监控 Bilibili 直播状态，接收开播提醒。
* **智能防抖**：内置随机抖动轮询机制，避免被判定为爬虫。
* **多主播支持**：支持批量监控，开播时聚合展示，不错过任何 D 推。
* **闹钟唤醒**：锁屏状态下全屏弹窗提醒，支持自定义铃声。
* **置顶优先**：支持设置“单推”置顶，铃声优先级最高。

## 🛠️ 技术栈 (Tech Stack)

* **语言**: Kotlin
* **UI**: Jetpack Compose (Material3)
* **网络**: Retrofit2 + OkHttp3
* **图片加载**: Coil
* **数据存储**: Room Database + DataStore
* **后台任务**: Android Service + AlarmManager + Coroutines
* **异步处理**: Kotlin Coroutines & Flow
* **架构**: MVVM (ViewModel + Repository)

## 📥 下载与安装 | Download

请前往 [Releases](https://github.com/ikaorihara/RuKnot/releases) 页面下载最新版本的 APK。

> **注意**：首次安装后，请务必在设置中授予 **“自启动”** 和 **“后台无限制”** 权限，以确保监控服务不被系统查杀。

## ⚖️ 免责声明 (Disclaimer)

本项目为粉丝自制开源项目（Fan-made Project），与 **折原露露 (Orihara Ruru)** 本人及其所属运营方无任何官方关联。

* App 内使用的音频/图片等素材版权归原作者所有。
* 本项目为个人开源作品，与 Bilibili 官方无关。
* 请合理设置轮询间隔，避免对 B 站服务器造成压力。
* 本项目仅供学习交流使用，请勿用于商业用途。

## 📄 License

源代码遵循 [Apache License 2.0](LICENSE) 协议。

```text
Copyright 2026 Ika Orihara

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.