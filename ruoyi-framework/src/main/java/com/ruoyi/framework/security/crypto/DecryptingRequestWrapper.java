package com.ruoyi.framework.security.crypto;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class DecryptingRequestWrapper extends HttpServletRequestWrapper
{
    private final byte[] body;
    private final Map<String, String[]> parameters;

    public DecryptingRequestWrapper(HttpServletRequest request, String plainText,
                                    Map<String, String[]> parameters)
    {
        super(request);
        this.body = plainText == null ? new byte[0] : plainText.getBytes(StandardCharsets.UTF_8);
        this.parameters = parameters == null ? request.getParameterMap() : parameters;
    }

    @Override
    public ServletInputStream getInputStream()
    {
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream()
        {
            public boolean isFinished() { return input.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener listener) { }
            public int read() { return input.read(); }
        };
    }

    @Override
    public BufferedReader getReader()
    {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override public String getParameter(String name) { String[] v = parameters.get(name); return v == null ? null : v[0]; }
    @Override public Map<String, String[]> getParameterMap() { return Collections.unmodifiableMap(parameters); }
    @Override public Enumeration<String> getParameterNames() { return Collections.enumeration(parameters.keySet()); }
    @Override public String[] getParameterValues(String name) { return parameters.get(name); }
}
