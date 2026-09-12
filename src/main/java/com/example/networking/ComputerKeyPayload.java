package com.example.networking;

import com.example.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ComputerKeyPayload(
        BlockPos computerPos,
        int key
) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    ExampleMod.MOD_ID,
                    "computer_key"
            );

    public static final Type<ComputerKeyPayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerKeyPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ComputerKeyPayload::computerPos,

                    ByteBufCodecs.VAR_INT,
                    ComputerKeyPayload::key,

                    ComputerKeyPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
