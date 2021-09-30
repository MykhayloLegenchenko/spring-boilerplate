package com.example.template.account.contract;

import com.example.template.account.contract.model.LoginRequest;
import com.example.template.account.contract.model.RegisterUserRequest;
import com.example.template.account.contract.model.TokenResponse;
import com.example.template.shared.microservice.ReactiveMicroserviceContract;
import com.example.template.shared.model.JsonEntity;
import reactor.core.publisher.Mono;

public interface ReactiveAccountContract extends ReactiveMicroserviceContract {

  Mono<JsonEntity<TokenResponse>> register(RegisterUserRequest request);

  Mono<JsonEntity<TokenResponse>> login(LoginRequest request);

  Mono<JsonEntity<TokenResponse>> refresh();

  Mono<JsonEntity<Void>> logout();

  Mono<JsonEntity<Void>> update();
}
