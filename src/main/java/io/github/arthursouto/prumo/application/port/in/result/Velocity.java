package io.github.arthursouto.prumo.application.port.in.result;

import java.util.List;

public record Velocity(List<VelocitySprint> sprints, double averageCompleted, double averageCommitted) {
}
