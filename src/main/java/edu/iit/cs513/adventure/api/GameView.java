package edu.iit.cs513.adventure.api;

import edu.iit.cs513.adventure.game.GameSnapshot;

public record GameView(String id, GameSnapshot snapshot) {
}
