package com.bondofthebeast.mixin;

import com.bondofthebeast.ForcedTamingService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public class PetArmorUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void botb$ownerOnly(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (ForcedTamingService.armorLocked(player)) cir.setReturnValue(TypedActionResult.fail(player.getStackInHand(hand)));
    }
}
