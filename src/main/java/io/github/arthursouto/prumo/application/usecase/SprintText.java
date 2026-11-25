package io.github.arthursouto.prumo.application.usecase;

final class SprintText {

    private SprintText() {
    }

    static String goal(String goal) {
        return goal == null || goal.isBlank() ? null : goal.strip();
    }
}
