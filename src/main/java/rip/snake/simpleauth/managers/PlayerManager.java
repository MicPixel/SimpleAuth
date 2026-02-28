package rip.snake.simpleauth.managers;

import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.Nullable;
import rip.snake.simpleauth.player.AuthPlayer;
import rip.snake.simpleauth.player.TPlayer;

import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private static final Map<UUID, AuthPlayer> AUTH_PLAYERS = Maps.newConcurrentMap();
    private static final Map<String, TPlayer> TMP_PLAYERS = Maps.newConcurrentMap();

    public static void PUT_PLAYER(UUID uniqueId, AuthPlayer authPlayer) {
        AUTH_PLAYERS.put(uniqueId, authPlayer);
    }

    public static void REMOVE_PLAYER(UUID uniqueId, String username) {
        AUTH_PLAYERS.remove(uniqueId);
    }

    public static void REMOVE_TMP_PLAYER(String username) {
        TMP_PLAYERS.remove(username);
    }

    public static void CLEANUP_SESSIONS() {
        long now = System.currentTimeMillis();
        TMP_PLAYERS.entrySet().removeIf(entry -> {
            TPlayer player = entry.getValue();
            // If lastDisconnectTime is 0, player is connected (or just created and not disconn).
            // But we only want to remove if they are disconnected AND expired.
            // We need to ensure we set lastDisconnectTime when they quit.
            return player.getLastDisconnectTime() > 0 && (now - player.getLastDisconnectTime() > 15 * 60 * 1000);
        });
    }

    @Nullable
    public static AuthPlayer GET_PLAYER(UUID uniqueId) {
        return AUTH_PLAYERS.getOrDefault(uniqueId, null);
    }

    public static TPlayer GET_TMP_PLAYER(String username) {
        return TMP_PLAYERS.computeIfAbsent(username.toLowerCase(), s -> new TPlayer(username, "", false, false, false, 0, null, System.currentTimeMillis(), ""));
    }

}
