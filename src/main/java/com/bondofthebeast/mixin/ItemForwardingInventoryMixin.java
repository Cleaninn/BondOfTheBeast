package com.bondofthebeast.mixin;

import com.bondofthebeast.ItemForwardingService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class ItemForwardingInventoryMixin {
    @Shadow @Final public PlayerEntity player;

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void botb$incoming(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || !(player instanceof ServerPlayerEntity pet)) return;
        ItemForwardingService.incoming(pet, stack);
        if (stack.isEmpty()) cir.setReturnValue(true);
    }
}
