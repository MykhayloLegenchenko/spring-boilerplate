package com.example.template.shared.microservice;

import com.example.template.shared.model.JsonEntity;

public interface MicroserviceContract {
  JsonEntity<String> version();
}
