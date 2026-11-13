package edu.iit.cs513.adventure.game;

import java.util.Objects;

public record ActionResult(ActionStatus status, GameSnapshot snapshot) {
    public ActionResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(snapshot);
    }
}
