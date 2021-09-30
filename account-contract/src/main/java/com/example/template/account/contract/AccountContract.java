package com.example.template.account.contract;

import com.example.template.account.contract.model.LoginRequest;
import com.example.template.account.contract.model.RegisterUserRequest;
import com.example.template.account.contract.model.TokenResponse;
import com.example.template.shared.microservice.MicroserviceContract;
import com.example.template.shared.model.JsonEntity;

public interface AccountContract extends MicroserviceContract {

  JsonEntity<TokenResponse> register(RegisterUserRequest request);

  JsonEntity<TokenResponse> login(LoginRequest request);

  JsonEntity<TokenResponse> refresh();

  JsonEntity<Void> logout();

  JsonEntity<Void> update();
}
