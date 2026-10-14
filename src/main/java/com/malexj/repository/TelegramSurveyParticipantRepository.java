package com.malexj.repository;

import com.malexj.model.TelegramSurveyParticipant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramSurveyParticipantRepository
    extends MongoRepository<TelegramSurveyParticipant, String> {}
