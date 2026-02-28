package com.kaos.jira.entity.converter;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Conversor JPA que cifra/descifra campos sensibles usando AES-256/GCM/NoPadding.
 *
 * <p>La clave de cifrado se obtiene de la variable de entorno {@code AES_SECRET_KEY}.
 * Debe ser una cadena codificada en Base64 que represente exactamente 32 bytes (256 bits).
 *
 * <p>El valor almacenado en BD tiene formato: {@code BASE64(IV_12bytes || ciphertext || tag_16bytes)}.
 *
 * <p>Ejemplo de generación de clave:
 * <pre>
 *   openssl rand -base64 32
 * </pre>
 *
 * <p>Uso en entidad:
 * <pre>
 *   @Convert(converter = AesEncryptConverter.class)
 *   @Column(name = "token", nullable = false, length = 500)
 *   private String token;
 * </pre>
 */
@Converter(autoApply = false)
public class AesEncryptConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptConverter.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTE = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final String ENV_KEY = "AES_SECRET_KEY";

    private final SecretKey secretKey;

    public AesEncryptConverter() {
        String base64Key = System.getenv(ENV_KEY);
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("[AesEncryptConverter] Variable de entorno {} no definida. " +
                     "El cifrado de tokens Jira NO está activo.", ENV_KEY);
            this.secretKey = null;
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            log.info("[AesEncryptConverter] Clave AES-256 cargada correctamente desde {}.", ENV_KEY);
        }
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) return null;
        if (secretKey == null) return plainText; // sin cifrado si no hay clave

        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // concatenar IV + ciphertext+tag y codificar en Base64
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("[AesEncryptConverter] Error al cifrar el token Jira", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String encryptedBase64) {
        if (encryptedBase64 == null) return null;
        if (secretKey == null) return encryptedBase64; // sin descifrado si no hay clave

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTE];
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] decrypted = cipher.doFinal(cipherText);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("[AesEncryptConverter] Error al descifrar el token Jira", e);
        }
    }
}
