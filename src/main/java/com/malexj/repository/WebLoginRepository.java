package com.malexj.repository;

import com.malexj.model.WebLogin;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebLoginRepository extends MongoRepository<WebLogin, String> {

  /** All login attempts of a given method ({@code phone} / {@code email}), newest first. */
  List<WebLogin> findByMethodOrderByCreatedAtDesc(String method);
}
