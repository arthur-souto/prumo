package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.AuthorizationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationCodeJpaRepository extends JpaRepository<AuthorizationCodeEntity, UUID> {

    Optional<AuthorizationCodeEntity> findByCodeHash(String codeHash);

    @Modifying
    @Query("update AuthorizationCodeEntity c set c.usedAt = :usedAt where c.id = :id and c.usedAt is null")
    int markUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);
}
