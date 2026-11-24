package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record UpdateBoardSettingsCommand(UUID userId, UUID workspaceId, String keyPrefix, Boolean automationEnabled) {
}
