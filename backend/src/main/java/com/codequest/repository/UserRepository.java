package com.codequest.repository;

import com.codequest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data writes the implementation for us at startup: the method NAME is the query.
 * findByEmailIgnoreCase -> SELECT ... WHERE lower(email) = lower(?)
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
