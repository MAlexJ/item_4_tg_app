package com.malexj.repository;

import com.malexj.model.TelegramChannelMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramChannelMemberRepository
    extends MongoRepository<TelegramChannelMember, String> {}
