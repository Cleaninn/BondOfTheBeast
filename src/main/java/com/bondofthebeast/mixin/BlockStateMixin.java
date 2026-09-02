package com.bondofthebeast.mixin;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class BlockStateMixin {

    @Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true)
    private void makeBlocksUnbreakableForPet(PlayerEntity player, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (player != null) {
            try {
                var bond = ModComponents.PLAYER_BOND.get(player);
                if (bond.hasOwner()) {
                    if (pos.equals(bond.getBedPos())) {
                        cir.setReturnValue(0.0F);
                        return;
                    }

                    String blockId = Registries.BLOCK.getId(((AbstractBlock.AbstractBlockState)(Object)this).getBlock()).toString();
                    if (bond.getBlacklistedBlocks().contains(blockId)) {
                        cir.setReturnValue(0.0F);
                    } else if (bond.isNoBreakMode() && !bond.getWhitelistedBlocks().contains(blockId)) {
                        cir.setReturnValue(0.0F);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}