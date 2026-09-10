package com.example.client;

import com.example.networking.OpenComputerPayload;
import com.example.networking.ComputerScreenPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {

        ClientPlayNetworking.registerGlobalReceiver(
                OpenComputerPayload.TYPE,
                (payload, context) -> {
                    Minecraft.getInstance().gui.setScreen(
                            new ComputerScreen(payload.pos())
                    );
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ComputerScreenPayload.TYPE,
                (payload, context) -> {

                    // On est dans le contexte réseau.
                    // On repasse sur le thread client avant
                    // de modifier l'état graphique.
                    context.client().execute(() -> {

                        ComputerClientState.updateScreen(
                                payload.computerPos(),
                                payload.screen()
                        );

                    });
                }
        );
    }
}