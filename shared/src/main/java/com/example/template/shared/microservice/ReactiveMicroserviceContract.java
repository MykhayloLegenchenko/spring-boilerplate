package com.example.template.shared.microservice;

import com.example.template.shared.model.JsonEntity;
import reactor.core.publisher.Mono;

public interface ReactiveMicroserviceContract {
  Mono<JsonEntity<String>> version();
}
