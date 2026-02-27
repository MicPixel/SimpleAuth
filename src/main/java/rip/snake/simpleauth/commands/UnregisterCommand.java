package rip.snake.simpleauth.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import rip.snake.simpleauth.SimpleAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@AllArgsConstructor
public class UnregisterCommand implements SimpleCommand {

    private final SimpleAuth simpleAuth;

    // Check if the player has permission to use the command
    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("simpleauth.command.unregister");
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            String currentArg = invocation.arguments().length > 0 ? invocation.arguments()[0].toLowerCase() : "";
            return new ArrayList<>(simpleAuth.getMongoManager().fetchAllUsernames(currentArg));
        });
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>This command can only be run by a player."
            ));
            return;
        }

        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (args.length != 1) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    simpleAuth.getMessages().getString("messages.unregister-usage", "<red>Usage: <yellow>/unregister <username>")
            ));
            return;
        }

        String targetUsername = args[0].toLowerCase();

        CompletableFuture.supplyAsync(() -> simpleAuth.getMongoManager().fetchUsername(targetUsername))
                .thenAccept(authPlayerOpt -> {
                    authPlayerOpt.ifPresentOrElse(authPlayer -> {
                        try {
                            simpleAuth.getMongoManager().unregisterPlayer(authPlayer.getUniqueId());
                        } catch (Exception e) {
                            simpleAuth.getLogger().error("Failed to unregister player: {}", targetUsername, e);
                            player.sendMessage(MiniMessage.miniMessage().deserialize(
                                    simpleAuth.getMessages().getString("messages.unregister-error", "<red>An error occurred while unregistering the player.")
                            ));
                            return;
                        }

                        String successMessage = simpleAuth.getMessages().getString("messages.unregister-success", "<green>Successfully unregistered <username>!");
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                                successMessage,
                                Placeholder.unparsed("username", targetUsername)
                        ));

                        simpleAuth.getProxyServer().getPlayer(authPlayer.getUniqueId()).ifPresent(targetPlayer -> {
                            targetPlayer.disconnect(MiniMessage.miniMessage().deserialize(
                                    simpleAuth.getMessages().getString("messages.kick-message", "<red>You have been unregistered by an administrator.")
                            ));
                        });

                    }, () -> {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                                simpleAuth.getMessages().getString("messages.unregister-not-registered", "<red>This user is not registered!")
                        ));
                    });
                })
                .exceptionally(ex -> {
                    simpleAuth.getLogger().error("Failed to process unregister command for: {}", targetUsername, ex);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            simpleAuth.getMessages().getString("messages.unregister-error", "<red>An error occurred while unregistering the player.")
                    ));
                    return null;
                });

    }
}
