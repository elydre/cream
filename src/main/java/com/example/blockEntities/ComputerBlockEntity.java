package com.example.blockEntities;

import com.example.computer.Computer;
import com.example.computer.RWMem;
import com.example.items.FloppyDisk;
import com.example.networking.ComputerScreenPayload;

import java.util.HashSet;
import java.util.Set;

import com.example.ModBlockEntities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ComputerBlockEntity extends BlockEntity {

    private final Set<ServerPlayer> viewers = new HashSet<>();
    private final Computer computer;
    private final RWMem memory;
    private final BlockPos pos;

    private ItemStack floppyDisk = ItemStack.EMPTY;

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALED_BLOCK_E, pos, state);

        computer = new Computer();
        memory = computer.getMemory();
        this.pos = pos;
    }

    private void updateScreen() {
        byte[] screenData = new byte[80 * 25];

        for (int i = 0; i < screenData.length; i++) {
            screenData[i] = (byte) memory.read(RWMem.SCREEN_BASE + i);
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

        if (computer.computer.screenNeedsUpdate()) {
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

    public boolean hasFloppyDisk() {
        return !floppyDisk.isEmpty();
    }

    public short[] getFloppyDiskData() {
        if (floppyDisk.isEmpty()) {
            return null;
        }

        return FloppyDisk.getProgram(floppyDisk);
    }

    public String insertFloppyDisk(ItemStack stack) {
        if (!floppyDisk.isEmpty()) {
            return "component.aled.floppy_disk.errinsert_already_inserted";
        }

        if (!(stack.getItem() instanceof FloppyDisk)) {
            return "component.aled.floppy_disk.errinsert_no_floppy";
        }

        floppyDisk = stack.copyWithCount(1);
        stack.shrink(1);

        setChanged();

        return computer.loadProgram(getFloppyDiskData());
    }

    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        System.out.println("Saving floppy disk data...");

        if (!floppyDisk.isEmpty()) {
            output.store(
                    "FloppyDisk",
                    ItemStack.CODEC,
                    floppyDisk
            );
        }
    }

    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        System.out.println("Loading floppy disk data...");

        floppyDisk = input.read(
                "FloppyDisk",
                ItemStack.CODEC
        ).orElse(ItemStack.EMPTY);
    }
}
