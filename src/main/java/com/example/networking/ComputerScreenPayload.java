package com.example.networking;

import com.example.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.ArrayList;

public record ComputerScreenPayload(
        BlockPos computerPos,
        short[] screen
) implements CustomPacketPayload {
    public static final Type<ComputerScreenPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            ExampleMod.MOD_ID,
                            "computer_screen"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerScreenPayload> CODEC =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                ComputerScreenPayload::computerPos,

                ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.SHORT)
                    .map(
                        values -> {
                            short[] screen = new short[values.size()];
                            for (int index = 0; index < values.size(); index++) {
                                screen[index] = values.get(index);
                            }
                            return screen;
                        },
                        screen -> {
                            ArrayList<Short> values = new ArrayList<>(screen.length);
                            for (short value : screen) {
                                values.add(value);
                            }
                            return values;
                        }
                    ),
                ComputerScreenPayload::screen,
                ComputerScreenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
