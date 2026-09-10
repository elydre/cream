package com.example.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class ComputerScreen extends Screen {

    private final BlockPos computerPos;

    public ComputerScreen(BlockPos computerPos) {
        super(Component.literal("aledblock"));
        this.computerPos = computerPos;
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

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
