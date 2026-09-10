package com.example.networking;

import com.example.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.ByteBufCodecs;

public record ComputerScreenPayload(
        BlockPos computerPos,
        byte[] screen
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

                    ByteBufCodecs.BYTE_ARRAY,
                    ComputerScreenPayload::screen,

                    ComputerScreenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
