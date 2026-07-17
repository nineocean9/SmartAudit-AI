package com.ruoyi.framework.security.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiCryptoFilter extends OncePerRequestFilter
{
    public static final String ATTR_SESSION_KEY = ApiCryptoFilter.class.getName() + ".sessionKey";
    private final ApiCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public ApiCryptoFilter(ApiCryptoService cryptoService, ObjectMapper objectMapper)
    {
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException
    {
        String encryptedKey = request.getHeader("X-Encrypted-Key");
        if (encryptedKey == null || encryptedKey.isBlank())
        {
            filterChain.doFilter(request, response);
            return;
        }
        try
        {
            String ivText = request.getHeader("X-Encrypted-IV");
            String signature = request.getHeader("X-Encrypted-Signature");
            String cipherText;
            boolean queryRequest = "GET".equalsIgnoreCase(request.getMethod())
                    || "DELETE".equalsIgnoreCase(request.getMethod());
            if (queryRequest)
            {
                cipherText = request.getParameter("__enc");
            }
            else
            {
                Map<String, Object> envelope = objectMapper.readValue(request.getInputStream(),
                        new TypeReference<Map<String, Object>>() { });
                cipherText = String.valueOf(envelope.get("data"));
            }
            byte[] sessionKey = cryptoService.decryptSessionKey(encryptedKey);
            if (ivText == null || signature == null || cipherText == null
                    || !cryptoService.verify(ivText, cipherText, signature, sessionKey))
            {
                throw new SecurityException("加密请求签名校验失败");
            }
            String plainText = cryptoService.decrypt(cipherText, sessionKey,
                    Base64.getDecoder().decode(ivText));
            request.setAttribute(ATTR_SESSION_KEY, sessionKey);

            if (queryRequest)
            {
                Map<String, Object> values = objectMapper.readValue(plainText,
                        new TypeReference<Map<String, Object>>() { });
                Map<String, String[]> params = new LinkedHashMap<>();
                values.forEach((key, value) -> params.put(key, new String[] { value == null ? "" : String.valueOf(value) }));
                filterChain.doFilter(new DecryptingRequestWrapper(request, null, params), response);
            }
            else
            {
                filterChain.doFilter(new DecryptingRequestWrapper(request, plainText, null), response);
            }
        }
        catch (Exception e)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":400,\"msg\":\"加密请求解析失败\"}");
        }
    }
}
