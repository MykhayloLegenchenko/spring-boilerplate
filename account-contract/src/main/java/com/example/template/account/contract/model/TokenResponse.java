package com.example.template.account.contract.model;

import javax.validation.constraints.NotBlank;

@SuppressWarnings("UnusedVariable")
public record TokenResponse(@NotBlank String token) {}
