package com.example.template.account.contract.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@SuppressWarnings("UnusedVariable")
public record RegisterUserRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email @NotNull String email,
    @NotBlank String password,
    boolean subscribe) {}
