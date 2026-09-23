package com.thaleswillreis.authapi.controller;

import com.thaleswillreis.authapi.dto.MfaCodeRequest;
import com.thaleswillreis.authapi.dto.MfaSetupResponse;
import com.thaleswillreis.authapi.service.MfaService;
import com.thaleswillreis.authapi.util.ClientIpResolver;
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
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/setup")
    public MfaSetupResponse setup(HttpServletRequest httpRequest) {
        return mfaService.setup(ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@Valid @RequestBody MfaCodeRequest request, HttpServletRequest httpRequest) {
        mfaService.enable(request, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@Valid @RequestBody MfaCodeRequest request, HttpServletRequest httpRequest) {
        mfaService.disable(request, ClientIpResolver.resolve(httpRequest));
    }

}