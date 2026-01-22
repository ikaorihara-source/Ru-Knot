package com.ikaorihara.ruknot.utils

import android.util.Base64
import com.ikaorihara.ruknot.BuildConfig
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    // ★★★ 自定义密钥：你可以随便改，但发布后绝对不能变！否则旧备份会解不开 ★★★
    private const val SECRET_KEY_SEED = BuildConfig.BACKUP_SECRET

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"

    // 生成 32位 的 AES 密钥 (通过 SHA-256)
    private val secretKeySpec: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = SECRET_KEY_SEED.toByteArray(Charsets.UTF_8)
        digest.update(bytes, 0, bytes.size)
        val key = digest.digest()
        SecretKeySpec(key, ALGORITHM)
    }

    /**
     * 加密：输入 JSON 字符串 -> 输出 Base64 乱码
     */
    fun encrypt(plainText: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(16) // 生成一个随机 IV，增加安全性
            java.security.SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // 格式：IV + 密文 (IV必须保存，否则解不开)
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            // 转成 Base64 字符串方便存储
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Encryption failed")
        }
    }

    /**
     * 解密：输入 Base64 乱码 -> 输出 JSON 字符串
     */
    fun decrypt(encryptedBase64: String): String {
        try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

            // 分离 IV 和 密文
            val iv = ByteArray(16)
            val encryptedBytes = ByteArray(combined.size - 16)
            System.arraycopy(combined, 0, iv, 0, 16)
            System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.size)

            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)

            val originalBytes = cipher.doFinal(encryptedBytes)
            return String(originalBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Decryption failed: 文件可能被篡改或不兼容")
        }
    }
}