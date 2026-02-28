package rip.snake.simpleauth;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import lombok.Getter;
import org.slf4j.Logger;
import rip.snake.simpleauth.commands.*;
import rip.snake.simpleauth.listeners.ChatListener;
import rip.snake.simpleauth.listeners.ConnectionListener;
import rip.snake.simpleauth.listeners.MessagesListener;
import rip.snake.simpleauth.listeners.ServerListener;
import rip.snake.simpleauth.managers.MongoManager;
import rip.snake.simpleauth.managers.ServersManager;
import rip.snake.simpleauth.tasks.SessionCleanupTask;
import rip.snake.simpleauth.utils.ConfigCreator;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "simple-auth",
        name = "SimpleAuth",
        version = "${VERSION}",
        authors = {"iSnakeBuzz_"},
        url = "https://github.com/iSnakeBuzz/SimpleAuth",
        description = "A simple authentication plugin for Velocity."
)
@Getter
public class SimpleAuth {

    private final ProxyServer proxyServer;
    private final Logger logger;

    private final ConfigCreator configCreator;
    private final ServersManager serversManager;
    private final MongoManager mongoManager;

    @Inject
    public SimpleAuth(ProxyServer server, Logger logger, @DataDirectory Path pluginData) {
        this.proxyServer = server;
        this.logger = logger;
        this.configCreator = new ConfigCreator(pluginData);
        this.serversManager = new ServersManager(this);
        this.mongoManager = new MongoManager(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Loading SimpleAuth...");

        this.configCreator.createConfig();
        this.serversManager.loadServers();
        this.mongoManager.connect();

        // Registering listeners
        proxyServer.getEventManager().register(this, new ChatListener(this));
        proxyServer.getEventManager().register(this, new ServerListener(this));
        proxyServer.getEventManager().register(this, new ConnectionListener(this));
        proxyServer.getEventManager().register(this, new MessagesListener(this));

        // Registering commands
        proxyServer.getCommandManager().register("login", new LoginCommand(this));
        proxyServer.getCommandManager().register("register", new RegisterCommand(this));
        proxyServer.getCommandManager().register("changepassword", new ChangePassCommand(this), "changepass", "changepassword");
        proxyServer.getCommandManager().register("lobby", new LobbyCommand(this), "hub", "leave", "l");
        proxyServer.getCommandManager().register("unregister", new UnregisterCommand(this), "removeregister");

        // Registering tasks
        proxyServer.getScheduler()
                .buildTask(this, new SessionCleanupTask())
                .repeat(1, TimeUnit.MINUTES)
                .schedule();
    }

    public YamlDocument getConfig() {
        return configCreator.getConfig();
    }

    public YamlDocument getMessages() {
        return configCreator.getMessages();
    }

}
