package com.example;

import com.example.commands.FlashCommand;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public class ModCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                    dispatcher.register(
                        Commands.literal("aled")
                            .then(Commands.literal("flash")
                                .then(Commands.argument("url", StringArgumentType.string())
                                    .executes(FlashCommand::flash)
                                )
                            )
                    );
                }
        );
    }
}
