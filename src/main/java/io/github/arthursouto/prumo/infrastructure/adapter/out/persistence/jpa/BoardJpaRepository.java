package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BoardJpaRepository extends JpaRepository<BoardEntity, UUID> {

    boolean existsByKeyPrefix(String keyPrefix);
}
