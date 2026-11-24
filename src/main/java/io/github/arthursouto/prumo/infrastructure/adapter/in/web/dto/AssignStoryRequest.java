package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AssignStoryRequest(@Schema(description = "Member to assign, or null to unassign") UUID assigneeId) {
}
