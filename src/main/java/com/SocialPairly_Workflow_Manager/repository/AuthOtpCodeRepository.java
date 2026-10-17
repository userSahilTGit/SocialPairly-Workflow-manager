package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.AuthOtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthOtpCodeRepository extends JpaRepository<AuthOtpCode, Long> {

    /**
     * Unconsumed OTPs for purpose/identifier. Expiry is enforced in {@code OtpService}
     * so callers can return "OTP has expired" instead of a generic "not found".
     */
    @Query("""
            SELECT o FROM AuthOtpCode o
            WHERE o.purpose = :purpose
              AND o.identifier = :identifier
              AND o.consumedAt IS NULL
            ORDER BY o.createdAt DESC
            """)
    List<AuthOtpCode> findActive(
            @Param("purpose") String purpose,
            @Param("identifier") String identifier);

    default Optional<AuthOtpCode> findLatestActive(String purpose, String identifier, LocalDateTime now) {
        List<AuthOtpCode> list = findActive(purpose, identifier);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE AuthOtpCode o
            SET o.consumedAt = :now
            WHERE o.purpose = :purpose
              AND o.identifier = :identifier
              AND o.consumedAt IS NULL
            """)
    int consumeAllActive(
            @Param("purpose") String purpose,
            @Param("identifier") String identifier,
            @Param("now") LocalDateTime now);
}
