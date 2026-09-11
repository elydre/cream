package com.example.commands;

import com.example.ModComponents;
import com.example.components.FloppyProgram;

import com.example.items.FloppyDisk;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class FlashCommand {
    private static final int MAX_PROGRAM_SIZE = (64 * 2 * 1024 + 20) * 2; // (Xmem + RWmem + header) * 2 bytes per word

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

    public static int flash(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

        URI uri = getUri(context, player);

        if (uri == null) {
            return 0;
        }

        if (!isHoldingFloppyDisk(player)) {
            sendErrorToPlayer(null, player, "command.aled.flash.no_floppy");
            return 0;
        }

        player.sendSystemMessage(Component.translatable("command.aled.flash.downloading").withStyle(ChatFormatting.GRAY));
        downloadProgram(server, player, uri);

        return 1;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.aled.flash.not_player"));
            return null;
        }
    }

    private static URI getUri(
            CommandContext<CommandSourceStack> context,
            ServerPlayer player
    ) {
        String url = StringArgumentType.getString(context, "url");
        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            sendErrorToPlayer(null, player, "command.aled.flash.invalid_url");
            return null;
        }

        String scheme = uri.getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            sendErrorToPlayer(null, player, "command.aled.flash.invalid_url");
            return null;
        }

        return uri;
    }

    private static boolean isHoldingFloppyDisk(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof FloppyDisk;
    }

    private static void downloadProgram(
            MinecraftServer server,
            ServerPlayer player,
            URI uri
    ) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> handleResponse(server, player, response))
                .exceptionally(error -> {
                    sendErrorToPlayer(server, player, "command.aled.flash.download_error");
                    return null;
                });
    }

    private static void handleResponse(
            MinecraftServer server,
            ServerPlayer player,
            HttpResponse<byte[]> response
    ) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            sendErrorToPlayer(server, player, "command.aled.flash.download_error");
            return;
        }

        byte[] downloadedProgram = response.body();

        if (downloadedProgram.length > MAX_PROGRAM_SIZE) {
            sendErrorToPlayer(server, player, "command.aled.flash.program_too_large");
            return;
        }

        if (downloadedProgram.length % 2 != 0 || downloadedProgram.length == 0) {
            sendErrorToPlayer(server, player, "command.aled.flash.invalid_program_size");
            return;
        }

        short[] program = new short[downloadedProgram.length / 2];
        for (int i = 0; i < program.length; i++) {
            program[i] = (short) ((downloadedProgram[i * 2] & 0xFF) | ((downloadedProgram[i * 2 + 1] & 0xFF) << 8));
        }

        flashProgram(server, player, program);
    }

    private static void flashProgram(
            MinecraftServer server,
            ServerPlayer player,
            short[] program
    ) {
        server.execute(() -> {
            ItemStack currentStack = player.getMainHandItem();

            if (!(currentStack.getItem() instanceof FloppyDisk)) {
                sendErrorToPlayer(server, player, "command.aled.flash.no_floppy");
                return;
            }

            currentStack.set(
                    ModComponents.FLOPPY_PROGRAM,
                    new FloppyProgram("URL", program)
            );

            player.sendSystemMessage(Component.translatable("command.aled.flash.success", program.length).withStyle(ChatFormatting.GRAY));
        });
    }

    private static void sendErrorToPlayer(
            MinecraftServer server,
            ServerPlayer player,
            String message
    ) {
        if (server == null) {
            player.sendSystemMessage(
                    Component.translatable(message).withStyle(ChatFormatting.RED)
            );
            return;
        }
        server.execute(() ->
                player.sendSystemMessage(
                        Component.translatable(message).withStyle(ChatFormatting.RED)
                )
        );
    }
}
