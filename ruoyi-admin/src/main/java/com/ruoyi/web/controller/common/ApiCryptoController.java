package com.ruoyi.web.controller.common;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.framework.security.crypto.ApiCryptoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crypto")
public class ApiCryptoController
{
    private final ApiCryptoService cryptoService;

    public ApiCryptoController(ApiCryptoService cryptoService)
    {
        this.cryptoService = cryptoService;
    }

    @GetMapping("/public-key")
    public AjaxResult publicKey()
    {
        return AjaxResult.success("操作成功", cryptoService.getPublicKeyPem());
    }
}
