package com.example;

import com.example.blockEntities.ComputerBlockEntity;
import com.example.networking.ComputerClosePayload;
import com.example.networking.ComputerKeyPayload;
import com.example.networking.ComputerOpenPayload;
import com.example.networking.ComputerScreenPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;


public class ModNetworking {
    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(
                ComputerOpenPayload.TYPE,
                ComputerOpenPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                ComputerScreenPayload.TYPE,
                ComputerScreenPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                ComputerClosePayload.TYPE,
                ComputerClosePayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                ComputerKeyPayload.TYPE,
                ComputerKeyPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
            ComputerKeyPayload.TYPE,
            (payload, context) -> {

                ServerPlayer player = context.player();

                context.server().execute(() -> {

                    if (!(player.level().getBlockEntity(payload.computerPos())
                            instanceof ComputerBlockEntity computer)) {
                        return;
                    }

                    computer.getComputer()
                            .keyPress(payload.key());
                });
            }
        );

        ServerPlayNetworking.registerGlobalReceiver(
            ComputerClosePayload.TYPE,
            (payload, context) -> {

                ServerPlayer player = context.player();

                context.server().execute(() -> {

                    if (!(player.level().getBlockEntity(payload.computerPos())
                            instanceof ComputerBlockEntity computer)) {
                        return;
                    }

                    computer.removeViewer(player);
                });
            }
        );
    }
}
