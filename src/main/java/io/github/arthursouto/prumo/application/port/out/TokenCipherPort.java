package io.github.arthursouto.prumo.application.port.out;

public interface TokenCipherPort {

    String encrypt(String plain);

    String decrypt(String cipher);
}
