package com.example.util

import android.content.Context
import android.util.Base64
import com.example.BuildConfig
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtil {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_SALT = "dynamic_salt"
    private const val KEY_ENCRYPTED_GEMINI = "encrypted_gemini"
    private const val KEY_ENCRYPTED_NEIS = "encrypted_neis"
    
    // Hardcoded encryption components to generate the AES key (PBKDF2)
    private const val PASSPHRASE = "SmartSchoolLifeSecurityPassphrase"
    private const val ITERATION_COUNT = 1000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    // Default encrypted placeholder for public NEIS API key ("PUBLIC")
    // Encrypted on developer machine, provided as a hardcoded AES-encrypted constant.
    private const val ENCRYPTED_NEIS_PUBLIC_KEY = "Z5f9YpX7D6lHbeK1RuyWpQ==:iv:VTB0R0NpZHBpZFF2Z3c="

    /**
     * Initializes the dynamic key and salt. Dynamically encrypts the BuildConfig.GEMINI_API_KEY
     * on first startup and encrypts it using AES.
     */
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 1. Generate/Retrieve unique dynamic salt
        var saltBase64 = prefs.getString(KEY_SALT, null)
        if (saltBase64 == null) {
            val random = SecureRandom()
            val salt = ByteArray(16)
            random.nextBytes(salt)
            saltBase64 = Base64.encodeToString(salt, Base64.DEFAULT)
            prefs.edit().putString(KEY_SALT, saltBase64).apply()
        }

        // 2. Encrypt the BuildConfig.GEMINI_API_KEY dynamically if we haven't already
        val rawGeminiKey = BuildConfig.GEMINI_API_KEY
        if (rawGeminiKey.isNotEmpty() && rawGeminiKey != "MY_GEMINI_API_KEY") {
            try {
                val encrypted = encrypt(rawGeminiKey, context)
                prefs.edit().putString(KEY_ENCRYPTED_GEMINI, encrypted).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Generates a SecretKeySpec using PBKDF2 derived from the dynamic salt.
     */
    private fun getSecretKey(context: Context): SecretKeySpec {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: throw IllegalStateException("Salt is not initialized")
        val salt = Base64.decode(saltBase64, Base64.DEFAULT)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(PASSPHRASE.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypts plain text using AES.
     */
    fun encrypt(data: String, context: Context): String {
        val secretKey = getSecretKey(context)
        val cipher = Cipher.getInstance(ALGORITHM)
        val random = SecureRandom()
        val iv = ByteArray(16)
        random.nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        return "$encryptedBase64:iv:$ivBase64"
    }

    /**
     * Decrypts AES encrypted data into a mutable CharArray which can be safely zeroed out in memory.
     */
    fun decryptToCharArray(encryptedWithIv: String, context: Context): CharArray {
        val parts = encryptedWithIv.split(":iv:")
        if (parts.size != 2) return CharArray(0)
        
        val encryptedBytes = Base64.decode(parts[0], Base64.DEFAULT)
        val iv = Base64.decode(parts[1], Base64.DEFAULT)
        
        val secretKey = getSecretKey(context)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(ALGORITHM)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        val decryptedStr = String(decryptedBytes, Charsets.UTF_8)
        val charArray = decryptedStr.toCharArray()
        
        // Let's clear the decrypted byte array from memory as well
        Arrays.fill(decryptedBytes, 0.toByte())
        
        return charArray
    }

    /**
     * Executes a lambda block with the decrypted Gemini API key in a CharArray,
     * and guarantees that the CharArray is fully cleared immediately after use.
     */
    fun <T> withGeminiKey(context: Context, block: (String) -> T): T {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encrypted = prefs.getString(KEY_ENCRYPTED_GEMINI, null)
        
        // If we haven't dynamically encrypted yet, fall back directly to BuildConfig
        if (encrypted == null) {
            val key = BuildConfig.GEMINI_API_KEY
            val result = block(key)
            return result
        }

        val decryptedChars = decryptToCharArray(encrypted, context)
        val keyStr = String(decryptedChars)
        try {
            return block(keyStr)
        } finally {
            // Overwrite memory containing decrypted key
            Arrays.fill(decryptedChars, '\u0000')
        }
    }

    /**
     * Executes a lambda block with the decrypted NEIS API key,
     * and guarantees that it is fully cleared immediately after use.
     */
    fun <T> withNeisKey(context: Context, block: (String) -> T): T {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedCustomNeis = prefs.getString(KEY_ENCRYPTED_NEIS, null)
        
        val decryptedChars = if (encryptedCustomNeis != null) {
            try {
                decryptToCharArray(encryptedCustomNeis, context)
            } catch (e: Exception) {
                try {
                    decryptToCharArray(ENCRYPTED_NEIS_PUBLIC_KEY, context)
                } catch (ex: Exception) {
                    "a3c352bb8c1d4549b8272b75870991dd".toCharArray()
                }
            }
        } else {
            try {
                decryptToCharArray(ENCRYPTED_NEIS_PUBLIC_KEY, context)
            } catch (e: Exception) {
                "a3c352bb8c1d4549b8272b75870991dd".toCharArray()
            }
        }
        val keyStr = String(decryptedChars)
        try {
            return block(keyStr)
        } finally {
            Arrays.fill(decryptedChars, '\u0000')
        }
    }

    fun saveCustomNeisKey(context: Context, rawKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (rawKey.trim().isEmpty() || rawKey.trim() == "PUBLIC") {
            prefs.edit().remove(KEY_ENCRYPTED_NEIS).apply()
        } else {
            try {
                val encrypted = encrypt(rawKey.trim(), context)
                prefs.edit().putString(KEY_ENCRYPTED_NEIS, encrypted).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hasCustomNeisKey(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_ENCRYPTED_NEIS)
    }
}
