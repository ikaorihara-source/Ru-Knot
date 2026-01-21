# 露刻 (Ru-Knot) ⏰

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

请前往 [Releases](https://github.com/ikaorihara-source/Ru-Knot/releases) 页面下载最新版本的 APK。

> **注意**：首次安装后，请务必在设置中授予 **“自启动”** 和 **“后台无限制”** 权限，以确保监控服务不被系统查杀。

### ⚠️ 防杀后台设置指南 (必读)
**App 只有“活着”才能叫醒你！国产手机杀后台严重，请务必进行以下设置：**

#### 🚀 通用第一步
在手机的 **“最近任务列表”**（多任务界面）中，找到《露刻》，长按卡片并选择 **“锁定”** (🔒)。

#### 📱 各品牌详细路径 (点击展开)

<details>
<summary><strong>👉 三星 (One UI)</strong></summary>

1. **电池无限制**：设置 > 应用程序 > 露刻 > 电池 > 选择【无限制 (Unrestricted)】。
2. **闹钟权限**：特别访问权限 > 闹钟和提醒 > 开启露刻。
</details>

<details>
<summary><strong>👉 小米 / Redmi (HyperOS & MIUI) [最严，必看]</strong></summary>

1. **自启动 & 省电策略**：设置 > 应用设置 > 应用管理 > 露刻 > 开启【自启动】，并将【省电策略】改为【无限制】。
2. **霸屏权限 (关键)**：在应用权限管理中，务必将【后台弹出界面】和【锁屏显示】设为【始终允许】。
</details>

<details>
<summary><strong>👉 vivo / iQOO (OriginOS)</strong></summary>

1. **允许高耗电**：设置 > 电池 > 后台耗电管理 > 露刻 > 选择【允许后台高耗电】。
2. **权限开启**：在权限管理中开启【自启动】和【锁屏显示】。
</details>

<details>
<summary><strong>👉 OPPO / 一加 / Realme (ColorOS)</strong></summary>

1. **允许完全后台**：设置 > 应用管理 > 露刻 > 耗电管理 > 开启【允许完全后台行为】。
2. **锁屏通知**：确保通知与状态栏中的【锁屏通知】已开启。
</details>

<details>
<summary><strong>👉 华为 / 荣耀 (HarmonyOS & MagicOS)</strong></summary>

1. **手动管理**：设置 > 应用和服务 > 应用启动管理 > 露刻 > 关闭自动管理 > 手动勾选【允许自启动】、【允许关联启动】、【允许后台活动】。
2. **悬浮窗**：权限管理中开启【悬浮窗】。
</details>

<details>
<summary><strong>👉 魅族 (Flyme)</strong></summary>

1. **后台管理**：手机管家 > 权限管理 > 后台管理 > 露刻 > 选择【允许后台运行】。
</details>

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
