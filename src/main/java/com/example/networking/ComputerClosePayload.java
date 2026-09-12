package com.example.networking;

import com.example.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ComputerClosePayload(
        BlockPos computerPos
) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    ExampleMod.MOD_ID,
                    "close_computer"
            );

    public static final Type<ComputerClosePayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerClosePayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ComputerClosePayload::computerPos,
                    ComputerClosePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
