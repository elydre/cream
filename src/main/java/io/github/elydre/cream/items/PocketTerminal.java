package io.github.elydre.cream.items;

import io.github.elydre.cream.ModComponents;
import io.github.elydre.cream.ModItems;
import io.github.elydre.cream.blockEntities.ComputerBlockEntity;
import io.github.elydre.cream.networking.ComputerOpenPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class PocketTerminal extends Item {
    public PocketTerminal() {
        super(new Item.Properties()
                .setId(ModItems.keyOfItem("pocket_terminal"))
                .stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof ComputerBlockEntity)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            context.getItemInHand().set(
                    ModComponents.POCKET_TERMINAL_COMPUTER,
                    context.getClickedPos().immutable()
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos computerPos = stack.get(ModComponents.POCKET_TERMINAL_COMPUTER);
        if (computerPos == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(computerPos) instanceof ComputerBlockEntity computer) {
            computer.addViewer(serverPlayer);
            ServerPlayNetworking.send(serverPlayer, new ComputerOpenPayload(computerPos));
        }
        return InteractionResult.SUCCESS;
    }
}