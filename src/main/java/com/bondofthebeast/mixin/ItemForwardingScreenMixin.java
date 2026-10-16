package com.bondofthebeast.mixin;

import com.bondofthebeast.ItemForwardingService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ItemForwardingScreenMixin {
    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void botb$beforeClick(int slot, int button, SlotActionType action, PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity pet) ItemForwardingService.tick(pet);
    }

    @Inject(method = "onSlotClick", at = @At("RETURN"))
    private void botb$afterClick(int slot, int button, SlotActionType action, PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity pet) ItemForwardingService.tick(pet);
    }
}
