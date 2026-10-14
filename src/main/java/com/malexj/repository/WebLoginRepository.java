package com.malexj.repository;

import com.malexj.model.WebLogin;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebLoginRepository extends MongoRepository<WebLogin, String> {}
