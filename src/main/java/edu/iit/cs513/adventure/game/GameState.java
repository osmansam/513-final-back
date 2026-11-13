package edu.iit.cs513.adventure.game;

/** State pattern: playing games execute turns; terminal states reject further moves. */
public enum GameState {
    PLAYING {
        @Override
        boolean move(Game game, Direction direction) {
            return game.playTurn(direction);
        }
    },
    WON,
    LOST;

    boolean move(Game game, Direction direction) {
        return false;
    }
}
