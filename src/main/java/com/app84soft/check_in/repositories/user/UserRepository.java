package com.app84soft.check_in.repositories.user;

import com.app84soft.check_in.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer>, UserRepositoryCustom {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
