package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Provider 凭证编解码边界。
 *
 * <p>V1 使用本地 AES-GCM 做数据库静态加密，并兼容旧明文记录。后续可在此处替换为 KMS。</p>
 */
@Service
@RequiredArgsConstructor
public class AigcProviderCredentialCodec {

    public static final String ENCRYPTED_PREFIX = "enc:v1:";

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final AigcProperties aigcProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encode(String credential) {
        String normalized = normalize(credential)
                .orElseThrow(() -> new IllegalArgumentException("credential 不能为空"));
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        byte[] cipherText = doCipher(Cipher.ENCRYPT_MODE, normalized.getBytes(StandardCharsets.UTF_8), iv);
        return ENCRYPTED_PREFIX
                + Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
                + ":"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(cipherText);
    }

    public Optional<String> decode(String storedValue) {
        return normalize(storedValue).map(value -> {
            if (!isEncrypted(value)) {
                return value;
            }
            String payload = value.substring(ENCRYPTED_PREFIX.length());
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("Provider 凭证密文格式无效");
            }
            byte[] iv = Base64.getUrlDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getUrlDecoder().decode(parts[1]);
            byte[] plainText = doCipher(Cipher.DECRYPT_MODE, cipherText, iv);
            return new String(plainText, StandardCharsets.UTF_8);
        });
    }

    public boolean isEncrypted(String storedValue) {
        return storedValue != null && storedValue.startsWith(ENCRYPTED_PREFIX);
    }

    private Optional<String> normalize(String value) {
        if (value == null || value.isBlank() || value.startsWith("<")) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private byte[] doCipher(int mode, byte[] input, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, new SecretKeySpec(resolveKey(), ALGORITHM), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new IllegalStateException("Provider 凭证加密处理失败", e);
        }
    }

    private byte[] resolveKey() {
        String masterKey = aigcProperties.getSecurity().getCredentialMasterKey();
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException("aigc.security.credential-master-key 未配置");
        }
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(masterKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Provider 凭证主密钥不可用", e);
        }
    }
}
