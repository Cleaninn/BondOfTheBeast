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
                    net.minecraft.block.BlockState state = world.getBlockState(pos);

                    if (state.getBlock() instanceof com.bondofthebeast.block.PetBedBlock) {
                        BlockPos headPos = state.get(com.bondofthebeast.block.PetBedBlock.PART) == net.minecraft.block.enums.BedPart.HEAD ? pos : pos.offset(state.get(com.bondofthebeast.block.PetBedBlock.FACING));
                        if (world.getBlockEntity(headPos) instanceof com.bondofthebeast.block.PetBedBlockEntity bed) {
                            if (player.getUuidAsString().equals(bed.getBoundPetUUID())) {
                                cir.setReturnValue(0.0F);
                                return;
                            }
                        }
                    }

                    String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
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