package com.thaleswillreis.authapi.controller;

import com.thaleswillreis.authapi.dto.MfaCodeRequest;
import com.thaleswillreis.authapi.dto.MfaSetupResponse;
import com.thaleswillreis.authapi.service.MfaService;
import com.thaleswillreis.authapi.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/mfa")
@Tag(name = "MFA", description = "Configuracao de autenticacao multifator (TOTP) para o usuario autenticado")
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/setup")
    @Operation(summary = "Gera um novo segredo TOTP e o QR Code para configuracao", description = "O MFA so e ativado de fato apos chamar /enable com um codigo valido.")
    public MfaSetupResponse setup(HttpServletRequest httpRequest) {
        return mfaService.setup(ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirma a configuracao do MFA com um codigo TOTP valido")
    public void enable(@Valid @RequestBody MfaCodeRequest request, HttpServletRequest httpRequest) {
        mfaService.enable(request, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa o MFA (exige um codigo TOTP valido)")
    public void disable(@Valid @RequestBody MfaCodeRequest request, HttpServletRequest httpRequest) {
        mfaService.disable(request, ClientIpResolver.resolve(httpRequest));
    }

}