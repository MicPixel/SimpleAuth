package rip.snake.simpleauth.player;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TPlayer {

    private final String username;

    private String serverCaptcha;
    private boolean needAuth;
    private boolean loggedIn;
    private boolean authenticated;
    private int attempts;

    private RegisteredServer server;
    private long lastDisconnectTime;
    private String lastIp;

    public boolean attempt() {
        return attempts++ >= 3;
    }

}
