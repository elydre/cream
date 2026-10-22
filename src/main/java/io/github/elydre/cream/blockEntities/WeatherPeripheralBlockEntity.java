package io.github.elydre.cream.blockEntities;

import io.github.elydre.cream.ModBlockEntities;
import io.github.elydre.cream.computer.PeripheralDevice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WeatherPeripheralBlockEntity extends BlockEntity implements PeripheralDevice {
    public static final int TYPE = 1;

    public WeatherPeripheralBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEATHER_PERIPHERAL, pos, state);
    }

    @Override
    public int getPeripheralType() {
        return TYPE;
    }

    @Override
    public int read(int port) {
        Level level = getLevel();
        if (level == null) {
            return 0;
        }

        return switch (port) {
            case 1 -> level.isRaining() ? 1 : 0;
            case 2 -> level.isThundering() ? 1 : 0;
            case 3 -> (int) (level.getBiome(worldPosition).value().getBaseTemperature() * 1000.0f);
            default -> 0;
        };
    }
}