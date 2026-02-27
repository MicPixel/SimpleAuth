package rip.snake.simpleauth.utils;

import com.google.gson.JsonElement;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MojangAPI {

    // Interface to define how each API endpoint operates
    private interface APIEndpoint {
        String getName();

        // Returns UUID if found, null if definitely cracked/not found, throws Exception if rate-limited/down
        UUID fetch(String username) throws Exception;
    }

    // Pool of APIs ordered by priority. Mojang is the official source, others are fallbacks.
    private static final List<APIEndpoint> ENDPOINTS = Arrays.asList(new APIEndpoint() {
        @Override
        public String getName() {
            return "Mojang";
        }

        @Override
        public UUID fetch(String username) throws Exception {
            HttpURLConnection conn = createConnection("https://api.mojang.com/users/profiles/minecraft/" + username);
            int code = conn.getResponseCode();
            if (code == 200) {
                JsonElement json = GsonUtils.parseJson(new InputStreamReader(conn.getInputStream()), JsonElement.class);
                return formatIdToUUID(json.getAsJsonObject().get("id").getAsString());
            } else if (code == 404 || code == 204) {
                return null; // Definitive "Does not exist"
            }
            throw new RuntimeException("HTTP " + code); // Triggers failover (e.g., 429 Rate Limit)
        }
    }, new APIEndpoint() {
        @Override
        public String getName() {
            return "Ashcon";
        }

        @Override
        public UUID fetch(String username) throws Exception {
            HttpURLConnection conn = createConnection("https://api.ashcon.app/mojang/v2/user/" + username);
            int code = conn.getResponseCode();
            if (code == 200) {
                JsonElement json = GsonUtils.parseJson(new InputStreamReader(conn.getInputStream()), JsonElement.class);
                // Ashcon already formats the UUID with hyphens
                return UUID.fromString(json.getAsJsonObject().get("uuid").getAsString());
            } else if (code == 404) {
                return null;
            }
            throw new RuntimeException("HTTP " + code);
        }
    }, new APIEndpoint() {
        @Override
        public String getName() {
            return "PlayerDB";
        }

        @Override
        public UUID fetch(String username) throws Exception {
            HttpURLConnection conn = createConnection("https://playerdb.co/api/player/minecraft/" + username);
            int code = conn.getResponseCode();
            if (code == 200) {
                JsonElement json = GsonUtils.parseJson(new InputStreamReader(conn.getInputStream()), JsonElement.class);
                if (json.getAsJsonObject().get("success").getAsBoolean()) {
                    String id = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("player").get("id").getAsString();
                    return UUID.fromString(id);
                }
                return null;
            } else if (code == 400 || code == 404) {
                return null;
            }
            throw new RuntimeException("HTTP " + code);
        }
    }, new APIEndpoint() {
        @Override
        public String getName() {
            return "Minetools";
        }

        @Override
        public UUID fetch(String username) throws Exception {
            HttpURLConnection conn = createConnection("https://api.minetools.eu/uuid/" + username);
            int code = conn.getResponseCode();
            if (code == 200) {
                JsonElement json = GsonUtils.parseJson(new InputStreamReader(conn.getInputStream()), JsonElement.class);
                if (json.getAsJsonObject().has("id") && !json.getAsJsonObject().get("id").isJsonNull()) {
                    return formatIdToUUID(json.getAsJsonObject().get("id").getAsString());
                }
                return null;
            } else if (code == 404) {
                return null;
            }
            throw new RuntimeException("HTTP " + code);
        }
    });

    /**
     * Helper method to configure the connection.
     * We use short timeouts to prevent the proxy from freezing if an API goes completely offline.
     */
    private static HttpURLConnection createConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2500); // 2.5 seconds timeout to connect
        conn.setReadTimeout(2500);    // 2.5 seconds timeout to read data
        conn.setRequestProperty("User-Agent", "SimpleAuth-Proxy-Failover");
        return conn;
    }

    public static Optional<UUID> fetchUsername(String username) {
        for (APIEndpoint endpoint : ENDPOINTS) {
            try {
                UUID result = endpoint.fetch(username);

                // If the result is null, the API successfully confirmed the player does NOT exist.
                // We do NOT failover, we instantly return empty to save processing time.
                if (result == null) {
                    return Optional.empty();
                }

                // UUID found!
                return Optional.of(result);

            } catch (Exception e) {
                // The API failed (Rate Limit, Timeout, 500 Server Error).
                // We log it to the console and let the loop continue to the next API in the list.
                System.err.println("[SimpleAuth API] " + endpoint.getName() + " failed for " + username + " -> " + e.getMessage() + ". Failing over to next API...");
            }
        }

        // If ALL APIs in the pool failed (e.g. your proxy lost internet connection, or all APIs are down)
        // We log a critical error and default the player to offline mode so they can still join.
        System.err.println("[SimpleAuth API] FATAL: All API fallbacks failed for " + username + ". Defaulting to offline mode.");
        return Optional.empty();
    }

    private static UUID formatIdToUUID(String idStr) {
        StringBuilder uuidStr = new StringBuilder(idStr);
        uuidStr.insert(20, '-');
        uuidStr.insert(16, '-');
        uuidStr.insert(12, '-');
        uuidStr.insert(8, '-');
        return UUID.fromString(uuidStr.toString());
    }
}
