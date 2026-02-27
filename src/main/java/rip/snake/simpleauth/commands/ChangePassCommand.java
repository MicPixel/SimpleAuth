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
public class ChangePassCommand implements SimpleCommand {

    private final SimpleAuth simpleAuth;

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            return;
        }

        Player player = (Player) invocation.source();
        TPlayer tPlayer = PlayerManager.GET_TMP_PLAYER(player.getUsername());

        String[] args = invocation.arguments();
        if (args.length != 2) {
            invocation.source().sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.change-password-usage", "<red>Usage: <yellow>/changepass <password> <password>")
            ));
            return;
        }

        String password = args[0];
        String confirm_password = args[1];

        if (!password.equals(confirm_password)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.passwords-not-match", "<red>Passwords do not match!")
            ));
            return;
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                simpleAuth.getMessages().getString("messages.changing-password", "<yellow>Changing password...")
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
                simpleAuth.getMessages().getString("messages.password-changed", "<green>Password changed!")
        ));

        simpleAuth.getServersManager().sendLobby(player);
    }

}