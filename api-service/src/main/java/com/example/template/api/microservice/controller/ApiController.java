package com.example.template.api.microservice.controller;

import com.example.template.api.contract.ApiContract;
import com.example.template.shared.model.JsonEntity;
import com.example.template.shared.model.JsonEntityFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController implements ApiContract {

  @Override
  @PostMapping("/version")
  public JsonEntity<String> version() {
    log.info("version");

    return JsonEntityFactory.ok("0.0.1-SNAPSHOT");
  }
}
