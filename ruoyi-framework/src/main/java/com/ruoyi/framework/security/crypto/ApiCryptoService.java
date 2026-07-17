package com.ruoyi.framework.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class ApiCryptoService
{
    private final KeyPair keyPair;

    public ApiCryptoService()
    {
        try
        {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("初始化接口加密密钥失败", e);
        }
    }

    public String getPublicKeyPem()
    {
        String encoded = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded.replaceAll("(.{64})", "$1\n")
                + "\n-----END PUBLIC KEY-----";
    }

    public byte[] decryptSessionKey(String encryptedKey) throws Exception
    {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PrivateKey privateKey = keyPair.getPrivate();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
    }

    public String encrypt(String plainText, byte[] sessionKey, byte[] iv) throws Exception
    {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(derive(sessionKey, "enc"), "AES"),
                new IvParameterSpec(iv));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    public String decrypt(String cipherText, byte[] sessionKey, byte[] iv) throws Exception
    {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(derive(sessionKey, "enc"), "AES"),
                new IvParameterSpec(iv));
        return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
    }

    public String sign(String ivBase64, String cipherText, byte[] sessionKey) throws Exception
    {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(derive(sessionKey, "mac"), "HmacSHA256"));
        byte[] value = mac.doFinal((ivBase64 + "." + cipherText).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(value);
    }

    public boolean verify(String ivBase64, String cipherText, String signature, byte[] sessionKey) throws Exception
    {
        return MessageDigest.isEqual(Base64.getDecoder().decode(sign(ivBase64, cipherText, sessionKey)),
                Base64.getDecoder().decode(signature));
    }

    private byte[] derive(byte[] sessionKey, String purpose) throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(sessionKey);
        return digest.digest(purpose.getBytes(StandardCharsets.UTF_8));
    }
}
