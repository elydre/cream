package io.github.elydre.cream.client;

import io.github.elydre.cream.networking.ComputerOpenPayload;
import io.github.elydre.cream.networking.ComputerScreenPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class CreamMainClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {

        ClientPlayNetworking.registerGlobalReceiver(
                ComputerOpenPayload.TYPE,
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
                                payload.screen(),
                                payload.cursorPosition()
                        );

                    });
                }
        );
    }
}
