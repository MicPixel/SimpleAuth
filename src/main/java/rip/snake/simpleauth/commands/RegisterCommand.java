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
public class RegisterCommand implements SimpleCommand {

    private final SimpleAuth simpleAuth;

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            return;
        }

        Player player = (Player) invocation.source();
        TPlayer tPlayer = PlayerManager.GET_TMP_PLAYER(player.getUsername());

        if (tPlayer.isAuthenticated()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.already-registered", "<red>You are already registered!")
            ));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 3) {
            invocation.source().sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.register-usage", "<red>Usage: <yellow>/register <password> <password> <captcha>")
            ));
            return;
        }

        String password = args[0];
        String confirm_password = args[1];
        String captcha = args[2];

        if (!captcha.equals(tPlayer.getServerCaptcha())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.captcha-failed", "<red>Incorrect captcha!")
            ));
            return;
        }

        if (!password.equals(confirm_password)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.passwords-not-match", "<red>Passwords do not match!")
            ));
            return;
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                simpleAuth.getMessages().getString("messages.registering", "<yellow>Registering...")
        ));

        String hashedPassword = PasswordUtils.hashPassword(password);
        AuthPlayer authPlayer = new AuthPlayer(
                player.getUniqueId().toString(),
                player.getUsername(),
                "none",
                player.getRemoteAddress().getAddress().getHostAddress(),
                System.currentTimeMillis(),
                hashedPassword,
                false
        );

        simpleAuth.getMongoManager().createPlayerOrUpdate(authPlayer);
        tPlayer.setLoggedIn(true);
        tPlayer.setAuthenticated(true);

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                simpleAuth.getMessages().getString("messages.register-success", "<green>You have successfully registered!")
        ));

        simpleAuth.getServersManager().sendLobby(player);
    }

}