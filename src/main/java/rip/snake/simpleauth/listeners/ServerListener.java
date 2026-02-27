package rip.snake.simpleauth.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import rip.snake.simpleauth.SimpleAuth;
import rip.snake.simpleauth.managers.PlayerManager;
import rip.snake.simpleauth.player.TPlayer;

import java.util.Optional;

@AllArgsConstructor
public class ServerListener {
    private final SimpleAuth simpleAuth;

    @Subscribe
    public void onPlayerChooseServer(PlayerChooseInitialServerEvent event) {
        TPlayer tPlayer = PlayerManager.GET_TMP_PLAYER(event.getPlayer().getUsername());
        simpleAuth.getLogger().info("PlayerChooseInitialServerEvent: " + tPlayer.getUsername() + " " + tPlayer.isNeedAuth() + " " + tPlayer.isLoggedIn());

        if (tPlayer.isNeedAuth() && !tPlayer.isLoggedIn()) {
            Optional<RegisteredServer> authServer = simpleAuth.getServersManager().getAuthServer();

            if (authServer.isEmpty()) {
                event.setInitialServer(null);
                return;
            }

            tPlayer.setServer(authServer.get());
            event.setInitialServer(authServer.get());
        } else if (!tPlayer.isNeedAuth() && tPlayer.isLoggedIn()) {
            Optional<RegisteredServer> lobbyServer = simpleAuth.getServersManager().getLobbyServer();

            if (lobbyServer.isEmpty()) {
                event.setInitialServer(null);
                return;
            }

            event.setInitialServer(lobbyServer.get());
        }
    }

    @Subscribe
    public void onPostConnect(ServerConnectedEvent event) {
        TPlayer tPlayer = PlayerManager.GET_TMP_PLAYER(event.getPlayer().getUsername());
        simpleAuth.getLogger().info("ServerConnectedEvent: " + tPlayer.getUsername() + " " + tPlayer.isNeedAuth() + " " + tPlayer.isLoggedIn());

        if (tPlayer.isNeedAuth() && !tPlayer.isLoggedIn()) {
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(simpleAuth.getMessages().getString(
                    tPlayer.isAuthenticated() ? "messages.not-logged-in" : "messages.not-registered",
                    "<red>You are not logged in!"
            )));
        }
    }

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent event) {
        TPlayer tPlayer = PlayerManager.GET_TMP_PLAYER(event.getPlayer().getUsername());
        simpleAuth.getLogger().info("ServerPreConnectEvent: " + tPlayer.getUsername() + " " + tPlayer.isNeedAuth() + " " + tPlayer.isLoggedIn());

        // Cancel server connection if player is not logged in
        if (tPlayer.isNeedAuth() && !tPlayer.isLoggedIn()) {
            Optional<RegisteredServer> server = event.getResult().getServer();

            if (server.isEmpty()) {
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                return;
            }

            if (server.get().equals(tPlayer.getServer())) {
                return;
            }

            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(simpleAuth.getMessages().getString(
                    tPlayer.isAuthenticated() ? "messages.not-logged-in" : "messages.not-registered",
                    "<red>You are not logged in!"
            )));
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

}
