package io.github.arthursouto.prumo.application.port.out;

public interface RefreshTokenCodecPort {

    String generate();

    String hash(String rawToken);
}
