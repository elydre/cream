package io.github.elydre.cream.blockEntities;

import io.github.elydre.cream.computer.Computer;
import io.github.elydre.cream.computer.RWMem;
import io.github.elydre.cream.ModBlockEntities;
import io.github.elydre.cream.blocks.BlockComputer;
import io.github.elydre.cream.items.FloppyDisk;
import io.github.elydre.cream.networking.ComputerScreenPayload;

import java.util.HashSet;
import java.util.Set;

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
        short[] screenData = new short[80 * 25];

        for (int i = 0; i < screenData.length; i++) {
            screenData[i] = (short) memory.read(RWMem.SCREEN_BASE + i);
        }

        ComputerScreenPayload screenPayload =
                new ComputerScreenPayload(
                        pos,
                    screenData,
                        getComputer().getCursorPosition()
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

        computer.computer.tick(level, pos, state.getValue(BlockComputer.FACING));
        level.updateNeighborsAt(pos, state.getBlock());
        computer.setChanged();

        if (computer.computer.screenNeedsUpdate()) {
            computer.updateScreen();
        }
    }

    public Computer getComputer() {
        return computer;
    }

    public int getRedstoneOutput(net.minecraft.core.Direction direction) {
        return computer.getRedstoneOutput(direction);
    }

    public void addViewer(ServerPlayer player) {
        if (viewers.contains(player)) {
            return;
        }
        viewers.add(player);
        updateScreen();
    }

    public void removeViewer(ServerPlayer player) {
        viewers.remove(player);
    }

    public boolean hasViewer(ServerPlayer player) {
        return viewers.contains(player);
    }

    public boolean hasFloppyDisk() {
        return !floppyDisk.isEmpty();
    }

    public ItemStack ejectFloppyDisk() {
        if (floppyDisk.isEmpty()) {
            return ItemStack.EMPTY;
        }

        computer.halt();

        ItemStack ejectedDisk = floppyDisk;
        floppyDisk = ItemStack.EMPTY;
        setChanged();
        return ejectedDisk;
    }

    public short[] getFloppyDiskData() {
        if (floppyDisk.isEmpty()) {
            return null;
        }

        return FloppyDisk.getProgram(floppyDisk);
    }

    public String insertFloppyDisk(ItemStack stack) {
        if (!floppyDisk.isEmpty()) {
            return "component.cream.floppy.errinsert_already_inserted";
        }

        if (!(stack.getItem() instanceof FloppyDisk)) {
            return "component.cream.floppy.errinsert_no_floppy";
        }

        floppyDisk = stack.copyWithCount(1);
        stack.shrink(1);

        setChanged();

        return computer.loadProgram(getFloppyDiskData());
    }

    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        if (!floppyDisk.isEmpty()) {
            output.store(
                    "FloppyDisk",
                    ItemStack.CODEC,
                    floppyDisk
            );
        }

        computer.save(output.child("Computer"));
    }

    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        floppyDisk = input.read(
                "FloppyDisk",
                ItemStack.CODEC
        ).orElse(ItemStack.EMPTY);

        input.child("Computer").ifPresent(computer::load);
    }
}
