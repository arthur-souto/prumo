package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.GithubConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GithubConnectionJpaRepository extends JpaRepository<GithubConnectionEntity, UUID> {
}
