package edu.iit.cs513.adventure.api;

import edu.iit.cs513.adventure.game.ActionResult;
import edu.iit.cs513.adventure.game.Direction;
import edu.iit.cs513.adventure.game.GameSession;
import edu.iit.cs513.adventure.game.MoveCommand;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** In-memory game collection owned by one server instance. */
public final class GameService {
    private final ConcurrentMap<String, GameSession> games = new ConcurrentHashMap<>();

    public GameView create(Long seed) {
        GameSession session = seed == null ? new GameSession() : new GameSession(seed);
        String id = UUID.randomUUID().toString();
        games.put(id, session);
        return new GameView(id, session.snapshot());
    }

    public GameView get(String id) {
        return new GameView(id, require(id).snapshot());
    }

    public ActionResult move(String id, Direction direction) {
        return require(id).execute(new MoveCommand(direction));
    }

    public GameView reset(String id) {
        return new GameView(id, require(id).reset());
    }

    public void delete(String id) {
        if (games.remove(id) == null) {
            throw new NoSuchElementException("Game not found");
        }
    }

    private GameSession require(String id) {
        GameSession session = games.get(id);
        if (session == null) {
            throw new NoSuchElementException("Game not found");
        }
        return session;
    }
}
