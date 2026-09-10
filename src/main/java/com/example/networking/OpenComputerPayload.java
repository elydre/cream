package com.example.networking;

import com.example.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenComputerPayload(BlockPos pos)
        implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    ExampleMod.MOD_ID,
                    "open_computer"
            );

    public static final CustomPacketPayload.Type<OpenComputerPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenComputerPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenComputerPayload::pos,
                    OpenComputerPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
