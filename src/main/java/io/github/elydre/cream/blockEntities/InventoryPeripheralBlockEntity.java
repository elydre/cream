package io.github.elydre.cream.blockEntities;

import io.github.elydre.cream.ModBlockEntities;
import io.github.elydre.cream.computer.PeripheralDevice;
import io.github.elydre.cream.computer.RWMem;

import java.util.function.IntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class InventoryPeripheralBlockEntity extends BlockEntity implements PeripheralDevice {
    public static final int TYPE = 2;
    private static final int ITEM_NAME_MEMORY_SIZE = 64;

    private int cursor;
    private int memoryAddress;
    private int lastTransfer;
    private RWMem memory;
    private IntFunction<PeripheralDevice> peripheralResolver;

    public InventoryPeripheralBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INVENTORY_PERIPHERAL, pos, state);
    }

    @Override
    public int getPeripheralType() {
        return TYPE;
    }

    @Override
    public void updateContext(RWMem memory, IntFunction<PeripheralDevice> peripheralResolver) {
        this.memory = memory;
        this.peripheralResolver = peripheralResolver;
    }

    @Override
    public int read(int port) {
        Container inventory = getInventory();
        return switch (port) {
            case 1 -> ITEM_NAME_MEMORY_SIZE;
            case 0x11 -> writeItemName(inventory);
            case 0x12 -> getItemCount(inventory);
            case 0x13 -> inventory == null ? 0 : inventory.getContainerSize();
            case 0x31 -> lastTransfer;
            default -> 0;
        };
    }

    @Override
    public void write(int port, int value) {
        switch (port) {
            case 2 -> memoryAddress = value;
            case 0x10 -> setCursor(value);
            case 0x20 -> setTarget(value);
            case 0x30 -> transfer(value);
            default -> {
            }
        }
    }

    private Container getInventory() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }

        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (blockEntity instanceof Container container) {
                return container;
            }
        }
        return null;
    }

    private void setCursor(int value) {
        Container inventory = getInventory();
        int size = inventory == null ? 0 : inventory.getContainerSize();
        cursor = size == 0 ? 0 : Math.max(0, Math.min(size - 1, value));
    }

    private int getItemCount(Container inventory) {
        if (inventory == null || cursor >= inventory.getContainerSize()) {
            return 0;
        }
        return inventory.getItem(cursor).getCount();
    }

    private int writeItemName(Container inventory) {
        if (memory == null || inventory == null || cursor >= inventory.getContainerSize()) {
            return 0;
        }

        ItemStack stack = inventory.getItem(cursor);
        if (stack.isEmpty()) {
            return 0;
        }

        // String name = stack.getHoverName().getString();
        // use the id instead of the display name
        String name = stack.getItem().toString();

        int written = Math.min(name.length(), ITEM_NAME_MEMORY_SIZE);
        for (int index = 0; index < written; index++) {
            memory.write(memoryAddress + index, name.charAt(index));
        }
        return written;
    }

    private void setTarget(int device) {
        targetDevice = peripheralResolver == null ? null : peripheralResolver.apply(device);
    }

    private void transfer(int amount) {
        lastTransfer = 0;
        if (amount <= 0 || targetDevice == null || !targetDevice.isAvailable() || targetDevice == this
                || !(targetDevice instanceof InventoryPeripheralBlockEntity target)) {
            return;
        }

        Container sourceInventory = getInventory();
        Container targetInventory = target.getInventory();
        if (sourceInventory == null || targetInventory == null
                || cursor >= sourceInventory.getContainerSize()
                || target.cursor >= targetInventory.getContainerSize()) {
            return;
        }

        ItemStack source = sourceInventory.getItem(cursor);
        ItemStack destination = targetInventory.getItem(target.cursor);
        if (source.isEmpty()) {
            return;
        }

        int freeSpace;
        if (destination.isEmpty()) {
            freeSpace = source.getMaxStackSize();
        } else if (destination.getItem() == source.getItem()) {
            freeSpace = destination.getMaxStackSize() - destination.getCount();
        } else {
            return;
        }

        int moved = Math.min(amount, Math.min(source.getCount(), freeSpace));
        if (moved <= 0) {
            return;
        }

        if (destination.isEmpty()) {
            targetInventory.setItem(target.cursor, source.copyWithCount(moved));
        } else {
            destination.grow(moved);
            targetInventory.setItem(target.cursor, destination);
        }
        source.shrink(moved);
        sourceInventory.setItem(cursor, source);
        targetInventory.setChanged();
        sourceInventory.setChanged();
        lastTransfer = moved;
    }

    private PeripheralDevice targetDevice;
}