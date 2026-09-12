package com.example.client;

import com.example.networking.ComputerClosePayload;
import com.example.networking.ComputerKeyPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

public class ComputerScreen extends Screen {
    private static final int FONT_X = 6;
    private static final int FONT_Y = 10;
    private static final int SCREEN_BORDER = 5;

    private final BlockPos computerPos;
    private static final FontDescription TERMINAL_FONT =
        new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("aled", "terminal")
        );

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
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        byte[] screen = ComputerClientState.getScreen(computerPos);


        // calculate the starting position to center the screen
        int startX = (this.width - 80 * FONT_X) / 2;
        int startY = (this.height - 25 * FONT_Y) / 2;

        // draw a black rectangle
        graphics.fill(
                startX - SCREEN_BORDER,
                startY - SCREEN_BORDER,
                startX + 80 * FONT_X + SCREEN_BORDER,
                startY + 25 * FONT_Y + SCREEN_BORDER,
                0xFF000000
        );

        if (screen == null) {
            System.out.println("ComputerScreen: screen is null for computer at " + computerPos);
            return;
        }

        for (int y = 0; y < 25; y++) {
            for (int x = 0; x < 80; x++) {

                int index = y * 80 + x;
                char character =  (char) (screen[index] & 0xFF);

                if (character == 0) {
                    character = ' ';
                }

                Component text =
                        Component.literal(String.valueOf(character))
                                .setStyle(
                                        Style.EMPTY.withFont(TERMINAL_FONT)
                                );

                graphics.text(
                        font,
                        text,
                        startX + x * FONT_X,
                        startY + y * FONT_Y,
                        0xFFFFFFFF,
                        false
                );
            }
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
