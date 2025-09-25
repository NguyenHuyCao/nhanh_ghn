package com.app84soft.check_in.repositories.auth;

import com.app84soft.check_in.entities.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    @Modifying
    @Query("update RefreshToken r set r.revokedAt = CURRENT_TIMESTAMP where r.user.id = :userId and r.revokedAt is null")
    void revokeAllByUser(@Param("userId") Integer userId);
}
