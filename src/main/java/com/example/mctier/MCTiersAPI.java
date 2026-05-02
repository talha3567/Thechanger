package com.example.mctier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class MCTiersAPI {
    private static final String API_URL = "https://api.mctiers.com/v2/player/";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<String> getPlayerRank(String name, String gamemode) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + name))
                .header("Accept", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        if (json.has("rankings")) {
                            JsonObject rankings = json.getAsJsonObject("rankings");
                            if (rankings.has(gamemode)) {
                                JsonObject ranking = rankings.getAsJsonObject(gamemode);
                                int tier = ranking.get("tier").getAsInt();
                                int pos = ranking.get("pos").getAsInt();
                                String prefix = (pos == 0) ? "ht" : "lt";
                                return prefix + tier;
                            }
                        }
                    }
                    return null;
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }
}
