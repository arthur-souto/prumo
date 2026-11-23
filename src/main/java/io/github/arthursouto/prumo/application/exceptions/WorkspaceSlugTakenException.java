package io.github.arthursouto.prumo.application.exceptions;

public class WorkspaceSlugTakenException extends RuntimeException {

    public WorkspaceSlugTakenException() {
        super("Workspace slug already taken");
    }
}
