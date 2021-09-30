package com.example.template.shared.test.keycloak;

import lombok.NonNull;

@SuppressWarnings("UnusedVariable")
public record RegisterUserData(
    @NonNull String email,
    @NonNull String password,
    @NonNull String firstName,
    @NonNull String lastName) {}
