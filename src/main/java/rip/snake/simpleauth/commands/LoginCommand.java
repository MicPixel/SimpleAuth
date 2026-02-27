package rip.snake.simpleauth.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import rip.snake.simpleauth.SimpleAuth;
import rip.snake.simpleauth.managers.PlayerManager;
import rip.snake.simpleauth.player.AuthPlayer;
import rip.snake.simpleauth.player.TPlayer;
import rip.snake.simpleauth.utils.PasswordUtils;

@AllArgsConstructor
public class LoginCommand implements SimpleCommand {

    private final SimpleAuth simpleAuth;

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            return;
        }


        Player player = (Player) invocation.source();
        AuthPlayer authPlayer = PlayerManager.GET_PLAYER(player.getUniqueId());
        TPlayer tPlayer = PlayerManager.GET_TMP_PLAYER(player.getUsername());

        String[] args = invocation.arguments();

        if (tPlayer.isLoggedIn()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.already-logged-in", "<red>You are already logged in!")
            ));
            return;
        }

        if (authPlayer == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.not-registered", "<red>You are not registered!")
            ));
            return;
        }

        if (args.length != 2) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.login-usage", "<red>Usage: <yellow>/login <password> <captcha>")
            ));
            return;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                simpleAuth.getMessages().getString("messages.logging-in", "<yellow>Logging in...")
        ));

        String password = args[0];
        String captcha = args[1];

        if (!captcha.equals(tPlayer.getServerCaptcha())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.captcha-failed", "<red>Incorrect captcha!")
            ));
            return;
        }

        boolean verified = PasswordUtils.isPasswordValid(password, authPlayer.getHashedPassword());

        if (!verified) {
            if (tPlayer.attempt()) {
                player.disconnect(MiniMessage.miniMessage().deserialize(
                        simpleAuth.getMessages().getString("messages.too-many-attempts", "<red>Too many attempts!")
                ));
                return;
            }

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.login-failed", "<red>Incorrect password!")
            ));
            return;
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                simpleAuth.getMessages().getString("messages.login-success", "<green>Successfully logged in!")
        ));

        tPlayer.setLoggedIn(true);
        tPlayer.setAuthenticated(true);
        simpleAuth.getServersManager().sendLobby(player);
    }

}