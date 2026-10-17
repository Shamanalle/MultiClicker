package io.github.shamanalle.multiclicker.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.shamanalle.multiclicker.MultiClicker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Posts messages to a Discord webhook. Requests are sent in the background, one at a time, so the
 * game never waits for the network and Discord's rate limit is not hit by a burst of events.
 */
public final class Webhook {
    public static final int INFO = 0x5865F2;
    public static final int SUCCESS = 0x43B581;
    public static final int WARNING = 0xFAA61A;
    public static final int DANGER = 0xF04747;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static HttpClient client;
    /** The previous request; the next one starts after it, whatever its outcome. */
    private static CompletableFuture<Integer> queue = CompletableFuture.completedFuture(0);

    private Webhook() {
    }

    /** Discord webhook addresses (also the older discordapp.com and the canary/ptb hosts). */
    public static boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return "https".equals(uri.getScheme())
                    && (host.equals("discord.com") || host.endsWith(".discord.com") || host.equals("discordapp.com"))
                    && uri.getPath() != null && uri.getPath().startsWith("/api/webhooks/");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Sends an embed. The future completes with the HTTP status, or {@code -1} when the request
     * failed; it never completes exceptionally.
     */
    public static synchronized CompletableFuture<Integer> send(String url, String title, String text, int color,
                                                               String footer) {
        if (!isValidUrl(url)) {
            return CompletableFuture.completedFuture(-1);
        }
        JsonObject embed = new JsonObject();
        embed.addProperty("title", limit(title, 256));
        if (!text.isEmpty()) {
            embed.addProperty("description", limit(text, 4000));
        }
        embed.addProperty("color", color);
        if (!footer.isEmpty()) {
            JsonObject footerJson = new JsonObject();
            footerJson.addProperty("text", limit(footer, 2048));
            embed.add("footer", footerJson);
        }
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        JsonObject body = new JsonObject();
        body.addProperty("username", "MultiClicker");
        body.add("embeds", embeds);
        JsonObject mentions = new JsonObject();
        mentions.add("parse", new JsonArray()); // never ping @everyone from a player or item name
        body.add("allowed_mentions", mentions);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent", "MultiClicker")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        queue = queue.handle((previous, error) -> 0)
                .thenCompose(ignored -> client().sendAsync(request, HttpResponse.BodyHandlers.discarding()))
                .thenApply(HttpResponse::statusCode)
                .exceptionally(error -> {
                    MultiClicker.LOGGER.warn("Discord webhook request failed: {}", error.toString());
                    return -1;
                });
        return queue;
    }

    private static synchronized HttpClient client() {
        if (client == null) {
            client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        }
        return client;
    }

    private static String limit(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
