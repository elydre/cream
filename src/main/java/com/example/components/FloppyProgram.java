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

public record FloppyProgram(String author, short[] data) implements TooltipProvider {
        private static final Codec<short[]> SHORT_ARRAY_CODEC = Codec.list(Codec.SHORT)
                .xmap(
                    list -> {
                        short[] shorts = new short[list.size()];

                        for (int i = 0; i < list.size(); i++) {
                            shorts[i] = list.get(i);
                        }

                        return shorts;
                    },
                    shorts -> {
                        List<Short> list = new java.util.ArrayList<>(shorts.length);

                        for (short value : shorts) {
                            list.add(value);
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

                            SHORT_ARRAY_CODEC
                                    .fieldOf("data")
                                    .forGetter(FloppyProgram::data)
                    ).apply(
                            instance,
                            FloppyProgram::new
                    )
            );
        public short[] copyData() {
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
