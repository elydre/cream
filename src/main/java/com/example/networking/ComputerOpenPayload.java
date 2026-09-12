package com.example.networking;

import com.example.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ComputerOpenPayload(BlockPos pos)
        implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    ExampleMod.MOD_ID,
                    "open_computer"
            );

    public static final CustomPacketPayload.Type<ComputerOpenPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerOpenPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ComputerOpenPayload::pos,
                    ComputerOpenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
