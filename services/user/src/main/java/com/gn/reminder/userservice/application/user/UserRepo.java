package com.gn.reminder.userservice.application.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

interface UserRepo extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
}
