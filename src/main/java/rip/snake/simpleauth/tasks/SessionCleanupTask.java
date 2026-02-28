package rip.snake.simpleauth.tasks;

import rip.snake.simpleauth.SimpleAuth;
import rip.snake.simpleauth.managers.PlayerManager;

public class SessionCleanupTask implements Runnable {

    @Override
    public void run() {
        PlayerManager.CLEANUP_SESSIONS();
    }
}
