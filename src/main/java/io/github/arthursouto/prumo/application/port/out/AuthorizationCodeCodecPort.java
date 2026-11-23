package io.github.arthursouto.prumo.application.port.out;

public interface AuthorizationCodeCodecPort {

    String generate();

    String hash(String rawCode);
}
