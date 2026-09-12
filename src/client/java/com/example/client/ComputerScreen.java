package com.example.client;

import com.example.networking.ComputerClosePayload;
import com.example.networking.ComputerKeyPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;

import org.lwjgl.glfw.GLFW;

public class ComputerScreen extends Screen {

    private final BlockPos computerPos;

    public ComputerScreen(BlockPos computerPos) {
        super(Component.literal("aledblock"));
        this.computerPos = computerPos;
    }

    @Override
    protected void init() {
        super.init();

        ScreenKeyboardEvents.beforeKeyPress(this)
                .register(this::onKeyPress);

        ScreenKeyboardEvents.beforeCharType(this)
                .register(this::onCharType);

        ScreenKeyboardEvents.beforeKeyRelease(this)
                .register(this::onKeyRelease);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        byte[] screen =
            ComputerClientState.getScreen(computerPos);

        if (screen == null) {
            return;
        }

        for (int y = 0; y < 25; y++) {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < 80; x++) {
                if (screen[y * 80 + x] == 0) {
                    line.append(' ');
                } else {
                    line.append((char) screen[y * 80 + x]);
                }
            }

            graphics.text(
                    font,
                    line.toString(),
                    10,
                    10 + y * font.lineHeight,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    private void onKeyPress(Screen screen, KeyEvent event) {
        char key = switch (event.key()) {
            case GLFW.GLFW_KEY_BACKSPACE -> '\b';
            case GLFW.GLFW_KEY_ENTER -> '\r';
            case GLFW.GLFW_KEY_TAB -> '\t';
            default -> 0;
        };

        if (key == 0) {
            return;
        }

        for (int i = 1; i <= 2; i++) {
            ClientPlayNetworking.send(
                    new ComputerKeyPayload(
                            computerPos,
                            (i << 16) | (key & 0xFFFF)
                    )
            );
        }
    }

    private void onCharType(Screen screen, CharacterEvent event) {
        String text = event.codepointAsString();

        if (text.isEmpty()) {
            return;
        }

        int key = text.charAt(0);

        for (int i = 1; i <= 2; i++) {
            ClientPlayNetworking.send(
                    new ComputerKeyPayload(
                            computerPos,
                            (i << 16) | (key & 0xFFFF)
                    )
            );
        }
    }

    private void onKeyRelease(Screen screen, KeyEvent event) {
        // todo
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);

        ClientPlayNetworking.send(new ComputerClosePayload(computerPos));

        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
