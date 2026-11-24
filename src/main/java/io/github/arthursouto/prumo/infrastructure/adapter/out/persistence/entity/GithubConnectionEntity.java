package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_connections")
@Getter
@Setter
@NoArgsConstructor
public class GithubConnectionEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "token_cipher", nullable = false)
    private String tokenCipher;

    @Column(nullable = false)
    private String login;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
