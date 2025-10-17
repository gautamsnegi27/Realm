package com.gn.reminder.userservice.user.service;

import com.gn.reminder.userservice.shared.exception.UserNotFoundException;
import com.gn.reminder.userservice.user.dto.UserProfileResponse;
import com.gn.reminder.userservice.user.dto.UserRequest;
import com.gn.reminder.userservice.user.repository.UserRepo;
import com.gn.reminder.userservice.user.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepo repository;

  /**
   * method to create a new user
   * Cache is evicted on creation for all user-related caches
   *
   * @param request
   * @return
   */
  @CacheEvict(value = {"userProfiles", "userByEmail"}, allEntries = true)
  public String createUser(UserRequest request) {
    var user = repository.save(UserUtil.toUser(request));
    log.info("user created");
    return user.getId();
  }

  /**
   * method to update an existing user
   * Cache is evicted for the specific user on update
   *
   * @param userId
   * @param request
   */
  @CacheEvict(value = {"userProfiles", "userByEmail"}, key = "#userId")
  public void updateUser(String userId, UserRequest request) {
    var user = repository.findById(userId).orElseThrow(() -> new UserNotFoundException(
            String.format("update user:: no user found with provided id: %s", request.id())));
    repository.save(UserUtil.updateUserProfile(request, user));
  }

  /**
   * method to get user based on below param
   * Result is cached for 30 minutes
   *
   * @param email
   * @return
   */
  @Cacheable(value = "userByEmail", key = "#email")
  public UserProfileResponse getUser(String email) {
    log.info("Cache miss - Fetching user from database for email: {}", email);
    var user = repository.findByEmail(email).orElseThrow(
            () -> new UserNotFoundException(String.format("no user found with provided email: %s", email)));
    return UserProfileResponse.fromUser(user);
  }

  /**
   * method to get user profile by user ID (from JWT token)
   * Returns all user details except password
   * Result is cached for 30 minutes to reduce database load
   *
   * @param userId
   * @return UserProfileResponse without password
   */
  @Cacheable(value = "userProfiles", key = "#userId")
  public UserProfileResponse getUserProfile(String userId) {
    log.info("Cache miss - Fetching user profile from database for userId: {}", userId);
    var user = repository.findById(userId).orElseThrow(
            () -> new UserNotFoundException(String.format("no user found with provided id: %s", userId)));
    return UserProfileResponse.fromUser(user);
  }
}

