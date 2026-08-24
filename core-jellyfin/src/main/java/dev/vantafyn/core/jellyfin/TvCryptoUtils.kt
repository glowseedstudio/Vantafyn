package dev.vantafyn.core.jellyfin

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end cryptographic utilities for secure TV remote input and pairing over LAN.
 * Uses ECDH (NIST P-256) key agreement + AES-256-GCM authenticated encryption.
 */
object TvCryptoUtils {
    private const val EC_CURVE = "secp256r1"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private val secureRandom = SecureRandom()

    fun generateEcKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(EC_CURVE), secureRandom)
        return kpg.generateKeyPair()
    }

    fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun decodePublicKey(base64PublicKey: String): PublicKey {
        val keyBytes = Base64.decode(base64PublicKey, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(spec)
    }

    fun deriveSharedAesKey(myPrivateKey: PrivateKey, peerPublicKey: PublicKey): SecretKey {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(myPrivateKey)
        ka.doPhase(peerPublicKey, true)
        val sharedSecret = ka.generateSecret()

        // Derive 256-bit AES key via SHA-256
        val md = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = md.digest(sharedSecret)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    fun encryptAesGcm(plainText: String, key: SecretKey): Pair<String, String> {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)

        return Pair(ivBase64, cipherBase64)
    }

    fun decryptAesGcm(ivBase64: String, cipherBase64: String, key: SecretKey): String {
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
