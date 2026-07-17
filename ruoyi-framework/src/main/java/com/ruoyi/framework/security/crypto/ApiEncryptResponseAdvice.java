package com.ruoyi.framework.security.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ControllerAdvice
public class ApiEncryptResponseAdvice implements ResponseBodyAdvice<Object>
{
    private final ApiCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public ApiEncryptResponseAdvice(ApiCryptoService cryptoService, ObjectMapper objectMapper)
    {
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType)
    {
        return true;
    }

    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response)
    {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest servletRequest = attrs == null ? null : attrs.getRequest();
        byte[] key = servletRequest == null ? null
                : (byte[]) servletRequest.getAttribute(ApiCryptoFilter.ATTR_SESSION_KEY);
        if (key == null || body == null || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) return body;
        try
        {
            byte[] iv = new byte[16];
            random.nextBytes(iv);
            String ivText = Base64.getEncoder().encodeToString(iv);
            String cipherText = cryptoService.encrypt(objectMapper.writeValueAsString(body), key, iv);
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("encrypted", true);
            envelope.put("data", cipherText);
            envelope.put("iv", ivText);
            envelope.put("signature", cryptoService.sign(ivText, cipherText, key));
            return envelope;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("接口响应加密失败", e);
        }
    }
}
