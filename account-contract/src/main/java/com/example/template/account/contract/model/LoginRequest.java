package com.example.template.account.contract.model;

import javax.validation.constraints.NotBlank;

@SuppressWarnings("UnusedVariable")
public record LoginRequest(@NotBlank String email, @NotBlank String password, boolean rememberMe) {}
