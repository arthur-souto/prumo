package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetVelocityQuery;
import io.github.arthursouto.prumo.application.port.in.result.Velocity;

public interface GetVelocityUseCase {

    Velocity execute(GetVelocityQuery query);
}
