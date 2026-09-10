package com.example.blockEntities;

import com.example.computer.Computer;
import com.example.computer.Memory;
import com.example.networking.ComputerScreenPayload;

import java.util.HashSet;
import java.util.Set;

import com.example.ModBlockEntities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ComputerBlockEntity extends BlockEntity {

    private final Set<ServerPlayer> viewers = new HashSet<>();
    private final Computer computer;
    private final Memory memory;
    private final BlockPos pos;

    public ComputerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.ALED_BLOCK_E,
                pos,
                state
        );

        computer = new Computer();
        memory = computer.getMemory();
        this.pos = pos;
    }

    private void updateScreen() {
        byte[] screenData = new byte[80 * 25];

        for (int i = 0; i < screenData.length; i++) {
            screenData[i] = (byte) memory.read(Memory.SCREEN_BASE + i);
        }

        ComputerScreenPayload screenPayload =
                new ComputerScreenPayload(
                        pos,
                        screenData
                );

        for (ServerPlayer viewer : viewers) {
            ServerPlayNetworking.send(
                    viewer,
                    screenPayload
            );
        }
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            ComputerBlockEntity computer
    ) {
        if (level.isClientSide()) {
            return;
        }

        computer.computer.tick();

        if (computer.computer.getCpu().screenNeedsUpdate()) {
            computer.updateScreen();
        }
    }

    public Computer getComputer() {
        return computer;
    }

    public void addViewer(ServerPlayer player) {
        if (viewers.contains(player)) {
            return;
        }
        viewers.add(player);
    }

    public void removeViewer(ServerPlayer player) {
        viewers.remove(player);
    }
}
