package com.malexj.repository;

import com.malexj.model.TelegramWebUser;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramWebUserRepository extends MongoRepository<TelegramWebUser, String> {

  /** All web-login users, most recent login first. */
  List<TelegramWebUser> findAllByOrderByLastLoginAtDesc();
}
