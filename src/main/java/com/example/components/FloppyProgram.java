package com.example.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.List;
import java.util.function.Consumer;

public record FloppyProgram(String author, byte[] data) implements TooltipProvider {
    private static final Codec<byte[]> BYTE_ARRAY_CODEC =
            Codec.list(Codec.BYTE)
                    .xmap(
                            list -> {
                                byte[] bytes = new byte[list.size()];

                                for (int i = 0; i < list.size(); i++) {
                                    bytes[i] = list.get(i);
                                }

                                return bytes;
                            },
                            bytes -> {
                                List<Byte> list =
                                        new java.util.ArrayList<>(bytes.length);

                                for (byte b : bytes) {
                                    list.add(b);
                                }

                                return list;
                            }
                    );

    public static final Codec<FloppyProgram> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.STRING
                                    .fieldOf("author")
                                    .forGetter(FloppyProgram::author),

                            BYTE_ARRAY_CODEC
                                    .fieldOf("data")
                                    .forGetter(FloppyProgram::data)
                    ).apply(
                            instance,
                            FloppyProgram::new
                    )
            );



    public byte[] copyData() {
        return data.clone();
    }

    @Override
    public void addToTooltip(
            TooltipContext context,
            Consumer<Component> tooltip,
            TooltipFlag flag,
            DataComponentGetter components
    ) {
        tooltip.accept(
                Component.translatable(
                        "component.aled.floppy_disk.author",
                        author
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltip.accept(
                Component.translatable(
                        "component.aled.floppy_disk.program_size",
                        data.length
                ).withStyle(ChatFormatting.GRAY)
        );
    }
}
