package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.WorkspaceSlugTakenException;
import io.github.arthursouto.prumo.application.port.in.CreateWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateWorkspaceCommand;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

import java.text.Normalizer;
import java.util.Locale;

public class CreateWorkspaceService implements CreateWorkspaceUseCase {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_SLUG_LENGTH = 50;
    private static final String FALLBACK_SLUG = "workspace";

    private final WorkspaceRepositoryPort workspaceRepository;

    public CreateWorkspaceService(WorkspaceRepositoryPort workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public Workspace execute(CreateWorkspaceCommand command) {
        String name = command.name().strip();
        String description = command.description() == null || command.description().isBlank()
                ? null
                : command.description().strip();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return workspaceRepository.save(Workspace.create(
                        name,
                        availableSlug(name),
                        description,
                        command.ownerId()
                ));
            } catch (WorkspaceSlugTakenException ignored) {
            }
        }

        throw new ConflictException("Could not generate a unique workspace slug");
    }

    private String availableSlug(String name) {
        String base = slugify(name);
        String slug = base;
        int suffix = 2;

        while (workspaceRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }

        return slug;
    }

    static String slugify(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }

        return slug.isEmpty() ? FALLBACK_SLUG : slug;
    }
}
