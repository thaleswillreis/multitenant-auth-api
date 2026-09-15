package com.thaleswillreis.authapi.dto;

public class CreateUserRequest {

    private String email;

    // ATENCAO: armazenado como texto puro por enquanto - hashing sera feito na Tarefa 2.2
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