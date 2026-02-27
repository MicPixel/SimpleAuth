package rip.snake.simpleauth.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import rip.snake.simpleauth.SimpleAuth;
import rip.snake.simpleauth.managers.PlayerManager;
import rip.snake.simpleauth.player.AuthPlayer;
import rip.snake.simpleauth.player.TPlayer;
import rip.snake.simpleauth.utils.MojangAPI;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionListener {
    private final SimpleAuth simpleAuth;

    // Stores players who are currently being tested for premium auth.
    // Key: Username, Value: Timestamp of the attempt.
    private final Map<String, Long> pendingPremiumChecks = new ConcurrentHashMap<>();

    public ConnectionListener(SimpleAuth simpleAuth) {
        this.simpleAuth = simpleAuth;
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onPreLogin(PreLoginEvent event) {
        String username = event.getUsername().toLowerCase();
        Optional<AuthPlayer> authPlayerOpt = simpleAuth.getMongoManager().fetchUsername(username);

        // 1. Player is already registered in our database
        if (authPlayerOpt.isPresent()) {
            if (authPlayerOpt.get().isPremium()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            } else {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            }
            return;
        }

        // 2. Unregistered Player (New) - Double Join Logic

        // Check if they tried to join in the last 15 seconds and failed.
        // If they are here again so quickly, it means their premium auth failed and they got disconnected (Bad Login).
        if (pendingPremiumChecks.containsKey(username)) {
            long lastAttempt = pendingPremiumChecks.get(username);
            if (System.currentTimeMillis() - lastAttempt < 15000) {
                // They are doing the "Double Join". Let them in as offline/cracked.
                simpleAuth.getLogger().info("[{}] Failed premium check previously. Letting in as offline.", username);
                pendingPremiumChecks.remove(username); // Clean up cache
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
                return;
            }
        }

        // They are new and haven't tried recently. We must check the Mojang API [web:5].
        Optional<UUID> uuid = MojangAPI.fetchUsername(username);

        if (uuid.isPresent()) {
            // The name is premium. Force Online Mode to test them [web:5].
            // We add them to our pending map.
            pendingPremiumChecks.put(username, System.currentTimeMillis());
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            simpleAuth.getLogger().info("[{}] Name is premium. Forcing Online Mode to verify.", username);
        } else {
            // The name doesn't exist in Mojang. Let them in directly as cracked.
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            simpleAuth.getLogger().info("[{}] Name is not premium. Forcing Offline Mode.", username);
        }
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onLogin(LoginEvent event) {
        String username = event.getPlayer().getUsername().toLowerCase();
        Optional<AuthPlayer> authPlayerOpt = simpleAuth.getMongoManager().fetchUsername(username);
        TPlayer tPlayer = PlayerManager.GET_TMP_PLAYER(username);

        // If they reached here and they were in pending, it means they SUCCESSFULLY completed premium auth!
        if (pendingPremiumChecks.containsKey(username)) {
            pendingPremiumChecks.remove(username); // Clean up

            // They are a real premium player joining for the first time.
            // Save them to the database automatically as premium.
            simpleAuth.getMongoManager().createPlayerOrUpdate(new AuthPlayer(
                    event.getPlayer().getUniqueId().toString(), // Use Velocity's provided UUID (verified by Mojang)
                    username,
                    "none",
                    event.getPlayer().getRemoteAddress().getAddress().getHostAddress(),
                    System.currentTimeMillis(),
                    "none",
                    true // isPremium = true
            ));

            tPlayer.setAuthenticated(true);
            tPlayer.setLoggedIn(true);
            tPlayer.setNeedAuth(false);
            simpleAuth.getLogger().info("[{}] Verified as REAL premium on first join! Saved to DB.", username);
            return;
        }

        // Standard logic for already registered players
        if (authPlayerOpt.isPresent() && authPlayerOpt.get().isPremium()) {
            tPlayer.setAuthenticated(true);
            tPlayer.setLoggedIn(true);
            tPlayer.setNeedAuth(false);
            simpleAuth.getLogger().info("[{}] The player is premium, and bypassed auth.", username);
        } else {
            // They are offline mode (cracked), need to register/login in auth server.
            tPlayer.setAuthenticated(false);
            tPlayer.setLoggedIn(false);
            tPlayer.setNeedAuth(true);
            simpleAuth.getLogger().info("[{}] The player is not premium or is new, needs auth.", username);
        }
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        String username = event.getPlayer().getUsername().toLowerCase();
        simpleAuth.getLogger().info("[{}] Disconnected", username);
        PlayerManager.REMOVE_PLAYER(event.getPlayer().getUniqueId(), username);

        // Note: We deliberately DO NOT remove them from pendingPremiumChecks here.
        // If a cracked player fails Mojang auth, Velocity kicks them, and we WANT them
        // to stay in the cache so their immediate double-join is recognized as offline.
    }
}
