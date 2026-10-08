package com.example.commands;

import com.example.ModComponents;
import com.example.components.FloppyProgram;
import com.example.items.FloppyDisk;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class RenameCommand {
    public static int rename(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("command.aled.flash.not_player"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof FloppyDisk)) {
            player.sendSystemMessage(
                    Component.translatable("command.aled.rename.no_floppy")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        FloppyProgram program = stack.get(ModComponents.FLOPPY_PROGRAM);
        if (program == null) {
            player.sendSystemMessage(
                    Component.translatable("command.aled.rename.no_program")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        String programName = StringArgumentType.getString(context, "name");
        stack.set(
                ModComponents.FLOPPY_PROGRAM,
                new FloppyProgram(programName, program.copyData())
        );

        player.sendSystemMessage(
                Component.translatable("command.aled.rename.success", programName)
                        .withStyle(ChatFormatting.GRAY)
        );
        return 1;
    }
}
