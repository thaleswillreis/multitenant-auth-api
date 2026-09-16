package com.thaleswillreis.authapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ter um formato valido")
    private String email;

    // ATENCAO: armazenado como texto puro por enquanto - hashing sera feito na Tarefa 2.2
    @NotBlank(message = "Senha e obrigatoria")
    @Size(min = 8, message = "Senha deve ter no minimo 8 caracteres")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}