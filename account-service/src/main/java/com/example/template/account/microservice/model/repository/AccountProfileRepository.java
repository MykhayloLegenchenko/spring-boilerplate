package com.example.template.account.microservice.model.repository;

import com.example.template.account.microservice.model.entity.AccountProfile;
import org.springframework.data.repository.CrudRepository;

public interface AccountProfileRepository extends CrudRepository<AccountProfile, String> {}
