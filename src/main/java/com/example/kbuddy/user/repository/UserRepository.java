package com.example.kbuddy.user.repository;

import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByEmail(String email);
}
