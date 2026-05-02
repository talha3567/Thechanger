package com.example.mctier;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

public class MCTierClient implements ClientModInitializer {
    private static String targetRank = null;
    private static String currentGamemode = "axe";
    private static String lastQueueCommand = null;
    private static boolean isSearching = false;

    private static final Pattern QUEUE_JOIN_PATTERN = Pattern.compile("(?i)\\[Queue\\] Joined ([a-z ]+) queue");
    private static final Pattern OPPONENT_FOUND_PATTERN = Pattern.compile("(?i)\\[Queue\\] Opponent found: ([a-zA-Z0-9_]+)");
    private static final Pattern MCPVP_MATCH_PATTERN = Pattern.compile("(?i)Starting match against ([a-zA-Z0-9_]+) \\(([a-zA-Z ]+)\\)");

    private static final Map<String, String> GAMEMODE_MAP = new HashMap<>();
    static {
        GAMEMODE_MAP.put("axe pvp", "axe");
        GAMEMODE_MAP.put("crystal pvp", "crystal");
        GAMEMODE_MAP.put("sword pvp", "sword");
        GAMEMODE_MAP.put("uuhc pvp", "uuhc");
        GAMEMODE_MAP.put("pot pvp", "pot");
        // Add more as needed
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("rank")
                .then(ClientCommandManager.argument("tier", StringArgumentType.word())
                    .executes(context -> {
                        String arg = StringArgumentType.getString(context, "tier");
                        if (arg.equalsIgnoreCase("stop")) {
                            isSearching = false;
                            context.getSource().sendFeedback(Text.literal("§cSearch stopped."));
                        } else {
                            targetRank = arg.toLowerCase();
                            context.getSource().sendFeedback(Text.literal("§aTarget rank set to: §6" + targetRank));
                            isSearching = true;
                        }
                        return 1;
                    })));
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String content = message.getString();

            Matcher queueMatcher = QUEUE_JOIN_PATTERN.matcher(content);
            if (queueMatcher.find()) {
                String gmName = queueMatcher.group(1).toLowerCase();
                currentGamemode = GAMEMODE_MAP.getOrDefault(gmName, gmName.split(" ")[0]);
                lastQueueCommand = "queue " + currentGamemode;
            }

            Matcher opponentMatcher = OPPONENT_FOUND_PATTERN.matcher(content);
            if (opponentMatcher.find()) {
                String opponent = opponentMatcher.group(1);
                handleOpponent(opponent);
            }

            Matcher mcpvpMatcher = MCPVP_MATCH_PATTERN.matcher(content);
            if (mcpvpMatcher.find()) {
                String opponent = mcpvpMatcher.group(1);
                String gmName = mcpvpMatcher.group(2).toLowerCase();
                currentGamemode = GAMEMODE_MAP.getOrDefault(gmName, gmName.split(" ")[0]);
                handleOpponent(opponent);
            }
        });
    }

    private void handleOpponent(String name) {
        if (targetRank == null || !isSearching) return;

        MCTiersAPI.getPlayerRank(name, currentGamemode).thenAccept(rank -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (rank != null && rank.equalsIgnoreCase(targetRank)) {
                client.execute(() -> {
                    if (client.player != null) {
                        for (int i = 0; i < 10; i++) {
                            client.player.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 1.0f, 1.0f);
                        }
                    }
                    client.inGameHud.setOverlayMessage(Text.literal("§6§lGELDİ"), false);
                    isSearching = false;
                });
            } else {
                client.execute(() -> {
                    if (client.player != null && client.player.networkHandler != null) {
                        client.player.networkHandler.sendChatCommand("leave");
                        CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(2000);
                                if (lastQueueCommand != null && isSearching) {
                                    client.execute(() -> {
                                        if (client.player != null && client.player.networkHandler != null) {
                                            client.player.networkHandler.sendChatCommand(lastQueueCommand);
                                        }
                                    });
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
            }
        });
    }
}
