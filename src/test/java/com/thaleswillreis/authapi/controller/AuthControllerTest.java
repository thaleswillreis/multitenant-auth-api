package com.thaleswillreis.authapi.controller;

import com.thaleswillreis.authapi.config.SecurityConfig;
import com.thaleswillreis.authapi.dto.LoginResponse;
import com.thaleswillreis.authapi.security.JwtService;
import com.thaleswillreis.authapi.security.TokenBlacklistService;
import com.thaleswillreis.authapi.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private TokenBlacklistService tokenBlacklistService;

        @Test
        void allowsLoginRequestWithoutAuthentication() throws Exception {
                when(authService.login(any(), any()))
                                .thenReturn(new LoginResponse("access-token", "refresh-token", "Bearer", 900L));

                String payload = """
                                {"email":"joao@acme.com","password":"12345678"}
                                """;

                mockMvc.perform(post("/api/auth/login")
                                .header("X-Tenant-Id", "3bccd450-629e-4ccd-9caf-1678b5575581")
                                .contentType("application/json")
                                .content(payload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access-token"))
                                .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        void rejectsInvalidLoginPayload() throws Exception {
                String invalidPayload = """
                                {"email":"nao-e-email","password":""}
                                """;

                mockMvc.perform(post("/api/auth/login")
                                .header("X-Tenant-Id", "3bccd450-629e-4ccd-9caf-1678b5575581")
                                .contentType("application/json")
                                .content(invalidPayload))
                                .andExpect(status().isBadRequest());
        }

}