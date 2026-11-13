package edu.iit.cs513.adventure.game;

import java.util.Objects;
import java.util.function.Supplier;

/** Command invoker and owner of one game's lifecycle. Each operation returns an atomic snapshot. */
public final class GameSession {
    private final Supplier<Game> gameFactory;
    private Game game;

    /** Ordinary sessions generate a fresh random layout on every reset. */
    public GameSession() {
        this(Game::newGame);
    }

    /** Seeded sessions reset to the same layout with fresh entities and strategy state. */
    public GameSession(long seed) {
        this(() -> Game.newGame(seed));
    }

    /** The factory supplies a new, unstarted game for each lifecycle. */
    GameSession(Supplier<Game> gameFactory) {
        this.gameFactory = Objects.requireNonNull(gameFactory);
        game = Objects.requireNonNull(gameFactory.get());
    }

    public synchronized GameSnapshot snapshot() {
        return game.snapshot();
    }

    public synchronized ActionResult execute(GameCommand command) {
        Objects.requireNonNull(command);
        ActionStatus status;
        if (game.getState() != GameState.PLAYING) {
            status = ActionStatus.GAME_OVER;
        } else {
            status = command.execute(game) ? ActionStatus.APPLIED : ActionStatus.BLOCKED;
        }
        return new ActionResult(status, game.snapshot());
    }

    public synchronized GameSnapshot reset() {
        // Build before replacing so a creation failure preserves the existing game.
        game = Objects.requireNonNull(gameFactory.get());
        return game.snapshot();
    }
}
