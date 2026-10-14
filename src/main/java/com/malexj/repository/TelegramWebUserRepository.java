package com.malexj.repository;

import com.malexj.model.TelegramWebUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramWebUserRepository extends MongoRepository<TelegramWebUser, String> {}
