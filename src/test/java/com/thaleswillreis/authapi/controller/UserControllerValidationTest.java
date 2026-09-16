package com.thaleswillreis.authapi.controller;

import com.thaleswillreis.authapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void rejectsCreationWithInvalidEmailAndShortPassword() throws Exception {
        String invalidPayload = """
                {"email":"nao-e-um-email","password":"123"}
                """;

        mockMvc.perform(post("/api/users")
                        .header("X-Tenant-Id", "3bccd450-629e-4ccd-9caf-1678b5575581")
                        .contentType("application/json")
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void rejectsCreationWithBlankFields() throws Exception {
        String blankPayload = """
                {"email":"","password":""}
                """;

        mockMvc.perform(post("/api/users")
                        .header("X-Tenant-Id", "3bccd450-629e-4ccd-9caf-1678b5575581")
                        .contentType("application/json")
                        .content(blankPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

}