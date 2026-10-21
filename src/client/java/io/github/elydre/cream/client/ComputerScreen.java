package io.github.elydre.cream.client;

import io.github.elydre.cream.networking.ComputerClosePayload;
import io.github.elydre.cream.networking.ComputerKeyPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.platform.InputConstants;

public class ComputerScreen extends Screen {
    private static final int FONT_X = 6;
    private static final int FONT_Y = 10;
    private static final int SCREEN_BORDER = 5;
    private static boolean ctrlPressed = false;


    private final BlockPos computerPos;
    private static final FontDescription TERMINAL_FONT =
        new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("cream", "terminal")
        );

    public ComputerScreen(BlockPos computerPos) {
        super(Component.literal("computer"));
        this.computerPos = computerPos;
    }

    @Override
    protected void init() {
        super.init();
        minecraft.textInputManager().startTextInput(this);
    }

    private static int color4bit_to_mc(int color) {
        int[] colors = {
            0xFF000000, // black
            0xFFAA0000, // red
            0xFF00AA00, // green
            0xFFAAAA00, // yellow
            0xFF0000AA, // blue
            0xFFAA00AA, // magenta
            0xFF00AAAA, // cyan
            0xFFAAAAAA, // white
            0xFF555555, // bright black (gray)
            0xFFFF5555, // bright red
            0xFF55FF55, // bright green
            0xFFFFFF55, // bright yellow
            0xFF5555FF, // bright blue
            0xFFFF55FF, // bright magenta
            0xFF55FFFF, // bright cyan
            0xFFFFFFFF  // bright white
        };

        if (color > 15)
            return 0xFFFFFFFF;

        return colors[color];
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        short[] screen = ComputerClientState.getScreen(computerPos);

        // calculate the starting position to center the screen
        int startX = (this.width - 80 * FONT_X) / 2;
        int startY = (this.height - 25 * (FONT_Y+1)) / 2;

        // draw a black rectangle
        graphics.fill(
                startX - SCREEN_BORDER,
                startY - SCREEN_BORDER,
                startX + 80 * FONT_X + SCREEN_BORDER,
                startY + 25 * (FONT_Y+1) + SCREEN_BORDER,
                0xFF000000
        );

        if (screen == null) {
            return;
        }

        for (int y = 0; y < 25; y++) {
            for (int x = 0; x < 80; x++) {

                int index = y * 80 + x;
                char character =  (char) (screen[index] & 0xFF);
                int fgColor = (screen[index] >> 8) & 0x0F;
                int bgColor = (screen[index] >> 12) & 0x0F;

                if (character == 0) {
                    character = ' ';
                }

                Component text =
                        Component.literal(String.valueOf(character))
                                .setStyle(
                                        Style.EMPTY.withFont(TERMINAL_FONT)
                                );

                if (bgColor != 0) {
                    graphics.fill(
                            startX + x * FONT_X,
                            startY + y * (FONT_Y+1),
                            startX + (x + 1) * FONT_X,
                            startY + (y + 1) * (FONT_Y+1),
                            color4bit_to_mc(bgColor)
                    );
                }

                graphics.text(
                        font,
                        text,
                        startX + x * FONT_X,
                        startY + y * (FONT_Y+1),
                        color4bit_to_mc(fgColor),
                        false
                );
            }
        }

        int cursorPosition = ComputerClientState.getCursorPosition(computerPos);
        int cursorX = startX + (cursorPosition % 80) * FONT_X;
        int cursorY = startY + (cursorPosition / 80) * (FONT_Y + 1);
        int cursorColor = 0xFFFF0000;

        for (int y = 0; y < FONT_Y; y++) {
            graphics.fill(
                    cursorX,
                    cursorY + y - 1,
                    cursorX + 1,
                    cursorY + y,
                    cursorColor
            );
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // static variable to save ctrl key state

        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (event.key() == InputConstants.KEY_LCONTROL || event.key() == InputConstants.KEY_RCONTROL) {
            ctrlPressed = true;
            return true;
        }

        char key = switch (event.key()) {
            case InputConstants.KEY_BACKSPACE -> '\b';
            case InputConstants.KEY_DELETE -> 0x7F;
            case InputConstants.KEY_RETURN -> '\r';
            case InputConstants.KEY_TAB -> '\t';
            case InputConstants.KEY_UP -> 'R';
            case InputConstants.KEY_DOWN -> 'Q';
            case InputConstants.KEY_LEFT -> 'P';
            case InputConstants.KEY_RIGHT -> 'O';
            default -> 0;
        };

        if (key == 0) {
            return false;
        }

        for (int i = 3; i <= 4; i++) {
            ClientPlayNetworking.send(
                    new ComputerKeyPayload(
                            computerPos,
                            (i << 16) | (key & 0x7FFF) | (ctrlPressed ? 0x8000 : 0)
                    )
            );
        }

        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int key = event.codepoint();

        if (key == 0) {
            return false;
        }

        for (int i = 1; i <= 2; i++) {
            ClientPlayNetworking.send(
                    new ComputerKeyPayload(
                            computerPos,
                            (i << 16) | (key & 0xFFFF)
                    )
            );
        }

        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == InputConstants.KEY_LCONTROL || event.key() == InputConstants.KEY_RCONTROL) {
            ctrlPressed = false;
        }

        return true;
    }

    @Override
    public void onClose() {
        minecraft.textInputManager().stopTextInput(this);
        minecraft.gui.setScreen(null);

        ClientPlayNetworking.send(new ComputerClosePayload(computerPos));

        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
