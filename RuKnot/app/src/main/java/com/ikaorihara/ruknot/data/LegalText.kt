package com.ikaorihara.ruknot.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

// ==========================================
// 1. 获取 用户协议 (EULA)
// ==========================================
@Composable
fun getEulaText(): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]

    // 动态获取 Gradle 里的 versionName
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.3.0" // 获取失败时的兜底
        }
    }

    return if (locale.language.contains("zh")) getEulaCn(version) else getEulaEn(version)
}

// ==========================================
// 2. 获取 隐私政策 (Privacy Policy)
// ==========================================
@Composable
fun getPrivacyText(): String {
    val locale = LocalConfiguration.current.locales[0]
    return if (locale.language.contains("zh")) PRIVACY_CN else PRIVACY_EN
}

// ==========================================
// 3. 获取 开源许可 (Open Source)
// ==========================================
@Composable
fun getOpenSourceText(): String {
    val locale = LocalConfiguration.current.locales[0]
    return if (locale.language.contains("zh")) OPEN_SOURCE_CN else OPEN_SOURCE_EN
}

// ==========================================
// 4. 获取 特别感谢 (Special Thanks)
// ==========================================
@Composable
fun getSpecialThanksText(): String {
    val locale = LocalConfiguration.current.locales[0]
    return if (locale.language.contains("zh")) SPECIAL_THANKS_CN else SPECIAL_THANKS_EN
}

// ########################################################################
//                             文本内容区域
// ########################################################################

// ------------------------------------------------------------------------
// A. 用户协议与免责声明 (EULA) - 中文
// ------------------------------------------------------------------------
private fun getEulaCn(version: String) = """
【露刻 (Ru-Knot) 最终用户许可协议 (EULA)】

版本：$version
最后更新日期：2026年2月24日

在使用“露刻 (Ru-Knot)”（以下简称“本应用”或“本软件”）之前，请您（以下简称“用户”）务必仔细阅读并充分理解本《最终用户许可协议与免责声明》（以下简称“本协议”）的所有条款。

1. 非官方性质与身份声明
(1) 本应用是由开发者“伊卡-折原 (Ika Orihara)”基于个人兴趣独立开发、发布并维护的非官方粉丝辅助工具。
(2) 本应用与主播“折原露露 (Orihara RuRu)”本人、其所属运营团队及哔哩哔哩 (Bilibili) 官方无任何隶属、授权、代理或背书关系。
(3) 本应用产生的任何技术故障、运营决策或法律纠纷，均由开发者个人承担全部责任，与折原露露本人及其运营团队无关。

2. 知识产权声明
(1) 素材归属：本应用内展示的所有涉及“折原露露”的形象、立绘、直播画面截图及相关元数据，其知识产权均归折原露露及其版权所有方所有。
(2) 合理使用：本应用依据著作权法中关于“个人学习、研究或欣赏”的合理使用 (Fair Use) 原则，对上述素材进行非商业性质的展示。
(3) 侵权处理：若权利方认为本应用侵犯了其合法权益，请联系开发者，我们将于24小时内处理。

3. 非盈利与开源承诺
(1) 免费政策：本应用承诺永久免费，不包含任何付费下载、内购或会员机制。
(2) 无商业广告：本应用界面内不植入任何第三方商业广告。
(3) 资金来源：本应用的开发与维护成本由开发者个人承担，不接受商业赞助。

4. 服务内容与使用限制
(1) 禁止行为：用户不得利用本应用进行反向工程，不得对 Bilibili 发起恶意攻击，不得发布侮辱、诽谤主播的言论。
(2) 违规后果：如用户违反规定，开发者有权终止服务并配合法律追责。

5. 免责声明 (重要)
(1) 账号与凭证安全：本应用允许用户手动填入 Bilibili 登录凭证 (Cookie) 以获取高级功能。用户需自行妥善保管该凭证，切勿将其截图或发送给他人。因用户自身泄露凭证导致的一切账号安全问题及财产损失，开发者概不负责。
(2) 服务稳定性：因系统设置（如杀后台）、Bilibili 接口变动或网络波动导致的提醒失败、闹钟未响，开发者不承担赔偿责任。
(3) 风险自担：用户应自行合理安排时间，因过分依赖提醒导致的损失由用户自行承担。
(4) 网络资源：应用内提供的“个性化背景”功能需连接第三方数据节点下载资源，开发者不对第三方服务的稳定性及内容安全性负责。

6. 终止
如果您违反本协议的任何条款，您的使用许可将自动终止。

7. 其他
(1) 开发者保留随时修改本协议的权利。
(2) 本协议最终解释权归开发者 伊卡-折原 所有。

--------------------------------------------------
【开发者寄语】
此应用源于热爱。
希望能让每一位喜欢/爱折原露露的粉丝，不错过她的每一次闪耀。
此时此刻，我永远爱折原露露。
""".trimIndent()

// ------------------------------------------------------------------------
// A. User License Agreement (EULA) - English
// ------------------------------------------------------------------------
private fun getEulaEn(version: String) = """
【Ru-Knot End User License Agreement (EULA)】

Version: $version
Last Updated: February 24, 2026

Before using "Ru-Knot" (hereinafter referred to as "the App"), please carefully read and fully understand all terms of this Agreement.

1. Non-Official Identity & Disclaimer
(1) The App is an UNOFFICIAL fan-made utility tool developed by "Ika-Orihara" out of personal interest.
(2) The App is NOT affiliated with, authorized by, endorsed by, or in any way officially connected with the VTuber "Orihara RuRu", her management team, or Bilibili.
(3) Any technical issues, operational decisions, or legal disputes arising from the App are the sole responsibility of the developer and are unrelated to Orihara RuRu or her management.

2. Intellectual Property Rights (IPR)
(1) Ownership: All images, Live2D models, logos, screenshots, and metadata related to "Orihara RuRu" displayed in the App remain the intellectual property of Orihara RuRu and her respective copyright holders.
(2) Fair Use: The App uses the aforementioned materials for non-commercial purposes under the principle of "Fair Use" for personal study, research, or appreciation.
(3) Infringement: If rights holders believe the App infringes upon their rights, please contact the developer for immediate removal or modification within 24 hours.

3. Non-Profit & Open Source Commitment
(1) Free of Charge: The App is provided permanently free of charge, with NO paid downloads, In-App Purchases (IAP), or subscription mechanisms.
(2) No Ads: The App does not contain any third-party commercial advertisements.
(3) Funding: All development and maintenance costs are borne personally by the developer. No commercial sponsorship is accepted.

4. Usage Restrictions
(1) Prohibited Acts: Users shall not reverse engineer the App, launch malicious attacks against Bilibili servers, or publish content that insults or defames the streamer.
(2) Consequences: The developer reserves the right to terminate services for users who violate these terms.

5. Limitation of Liability
(1) Account Security: The App allows users to input their Bilibili login token (Cookie) for advanced features. Users are solely responsible for keeping this token safe. The developer is NOT liable for any account compromise or asset loss resulting from users leaking their tokens.
(2) Service Stability: The developer assumes NO liability for missed alarms or notification failures caused by system restrictions (e.g., battery optimization), Bilibili API changes, or network instability.
(3) Risk Assumption: Users assume all risks associated with the use of the App. The developer is not liable for any loss resulting from reliance on the App's notifications.
(4) Network Resources: Features like "Personalized Background" download assets from third-party repositories. The developer is not responsible for the stability or safety of these third-party services.

6. Termination
This license terminates automatically if you violate any of its terms.

7. Miscellaneous
(1) The developer reserves the right to modify this agreement at any time.
(2) The final interpretation right of this agreement belongs to the developer, IkaIka-Orihara.

--------------------------------------------------
[Developer's Note]
This app is born of love.
I hope it helps every "Gachikoi" fan of Orihara RuRu never miss her shining moments.
At this moment, I will always love Orihara RuRu.
""".trimIndent()

// ------------------------------------------------------------------------
// B. 隐私政策 (Privacy Policy) - 中文
// ------------------------------------------------------------------------
private const val PRIVACY_CN = """
【隐私政策 (Privacy Policy)】

1. 数据收集原则
本应用坚持“绝对不收集”原则。我们绝不收集您的姓名、身份证号、手机号、设备序列号或任何生物识别信息。本应用也没有自己的独立后端服务器用于收集用户画像。

2. Bilibili 账号信息与凭证安全 (极重要)
本应用不需要登录即可使用。内置浏览器 (WebView) 产生的所有登录数据（Cookie/Token）均存储在您设备的本地 WebView 容器中，直接与 Bilibili 官方服务器交互。本应用无法、也不会获取或上传您的账号密码。
本应用提供“动态提醒”高级功能，需要您手动填入 Bilibili 的免密登录凭证 (Cookie)。
请您放心：您填入的 Cookie 采用 Android 官方的 DataStore 技术加密存储于您的手机本地沙盒中。该凭证【仅用于】本应用在后台向 Bilibili 官方服务器发起合法的数据查询请求。本应用【绝对不会】收集、窃取或将您的 Cookie 上传至任何第三方服务器。

3. 本地数据存储
您的所有设置（如闹钟时间、关注列表、自定义背景图片、更新频率等）仅存储在您的手机本地数据库或文件系统中。卸载应用或清除数据后，这些信息将被彻底且永久删除。

4. 权限使用说明
(1) 网络权限：用于向 Bilibili 官方接口请求公开的直播状态和动态更新。
(2) 通知权限：用于发送开播及动态提醒。
(3) 闹钟与后台权限：为了确保在锁屏和深度休眠状态下依然能准时唤醒提醒，我们需要申请忽略电池优化的权限。

5. 第三方服务与数据来源
(1) 直播数据：直接源于 Bilibili 公开接口。
(2) 背景资源：个性化背景图片/视频资源下载自云存储节点。
(3) 纯净承诺：本应用未接入任何第三方数据分析 SDK (如友盟、Bugly) 或广告 SDK。您的每一次点击都是绝对私密的。
"""

// ------------------------------------------------------------------------
// B. Privacy Policy - English
// ------------------------------------------------------------------------
private const val PRIVACY_EN = """
【Privacy Policy】

1. Data Collection
We adhere to a strict "Zero Collection" policy. We do NOT collect any Personally Identifiable Information (PII) such as your real name, ID, phone number, or device serial numbers. The App does not have a backend server to track user behavior.

2. Account Credentials
No login is required to use the App. Login sessions within the built-in browser (WebView) are stored locally on your device and communicate directly with Bilibili servers. The App cannot access or upload your password.
The App offers an advanced "Dynamic Notification" feature that requires users to manually input their Bilibili login token (Cookie).
Please rest assured: The Cookie you provide is securely encrypted and stored locally on your device using Android DataStore. This token is STRICTLY used by the App to authenticate requests sent DIRECTLY to Bilibili's official servers. We will NEVER collect, steal, or upload your Cookie to any third-party servers.

3. Local Storage
All your configurations (e.g., alarm settings, streamer lists, personalized backgrounds) are stored locally on your device. Uninstalling the App will permanently delete all such data.

4. Permissions
(1) Internet: To fetch live status and dynamic updates from Bilibili APIs.
(2) Notifications: To send stream and dynamic alerts.
(3) Alarms & Background: To ensure alerts work reliably even when the screen is off and the device is in deep sleep mode, we request battery optimization exemptions.

5. Third-Party Services
(1) Live Data: Fetched directly from Bilibili's public APIs.
(2) Background Assets: Downloaded from cloud storage nodes.
(3) Clean Commitment: The App does NOT integrate any third-party analytics SDKs (e.g., Google Analytics) or ad networks. Your usage is completely private.
"""

// ------------------------------------------------------------------------
// C. 开源许可 (Open Source) - 中文
// ------------------------------------------------------------------------
private const val OPEN_SOURCE_CN = """
【开源许可致谢】
本应用 (Ru-Knot) 使用了以下开源项目，感谢开发者的贡献：

1. Android Jetpack Compose (Apache 2.0)
2. Retrofit & OkHttp (Apache 2.0)
3. Coil (Apache 2.0)
4. AndroidX Media3 / ExoPlayer (Apache 2.0)
5. Material Design Icons (Apache 2.0)

(详细协议文本略，遵循 Apache 2.0 标准)
"""

// ------------------------------------------------------------------------
// C. Open Source - English
// ------------------------------------------------------------------------
private const val OPEN_SOURCE_EN = """
【Open Source Licenses】
We gratefully acknowledge the following open source projects:

1. Android Jetpack Compose (Apache 2.0)
2. Retrofit & OkHttp (Apache 2.0)
3. Coil (Apache 2.0)
4. AndroidX Media3 / ExoPlayer (Apache 2.0)
5. Material Design Icons (Apache 2.0)

(Licensed under Apache License 2.0)
"""

// ------------------------------------------------------------------------
// D. 特别感谢 (Special Thanks) - 中文
// ------------------------------------------------------------------------
private const val SPECIAL_THANKS_CN = """
【特别感谢 (Special Thanks)】

1. 灵感来源
折原露露 (Orihara RuRu)
感谢露宝带来的每一次直播、每一首歌曲和每一份快乐。
是你的闪耀，让我有了开发这个应用的动力。

2. 协助与反馈
感谢以下“露刻制作组”成员在开发过程中提供的建议，测试与帮助：
- 波波波波羊 (美术组)
- Kon秋山澪
- miao逝
- m姓管理员
- 高师傅
- 以及所有参与内测的群友们

3. 技术支持
感谢 Bilibili API 提供的开放接口。
感谢 GitHub 开源社区的前辈们。

4. 还有你
感谢下载并使用这个应用的每一位 Gachikoi。
希望 露刻 能陪伴你度过美好的追播时光。
"""

// ------------------------------------------------------------------------
// D. Special Thanks - English
// ------------------------------------------------------------------------
private const val SPECIAL_THANKS_EN = """
【Special Thanks】

1. My Muse
Orihara RuRu
Thank you for every stream, every song, and every joy you bring.
Your shine is the motivation behind this app.

2. Beta Testers & Contributors
Thanks to the "Ru-Knot Team" who provided feedback, support and help:
- 波波波波羊 (Art Materials)
- Kon秋山澪
- miao逝
- m姓管理员
- 高师傅
- And all beta testers

3. Technical Support
Thanks to the Bilibili Developer Community.
Thanks to the open-source community on GitHub.

4. And YOU
Thank you for using Ru-Knot.
I hope this app helps you stay connected with RuRu.
"""