package com.bondofthebeast.mixin;

import com.bondofthebeast.BondRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBase;
import net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms;
import net.onixary.shapeShifterCurseFabric.player_form.transform.TransformManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TransformManager.class, remap = false)
public class BookActivationTransformMixin {
    @Inject(method = "handleDirectTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$requireBook(PlayerEntity player, PlayerFormBase form, boolean cured, CallbackInfo ci) {
        // Keep the introductory book's transition to the enabled human form available.
        if (!BondRules.hasOpenedBook(player) && form != RegPlayerForms.ORIGINAL_SHIFTER && form != RegPlayerForms.ORIGINAL_BEFORE_ENABLE)
            ci.cancel();
    }

    @Inject(method = "handleProgressiveTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$requireBookForProgression(ServerPlayerEntity player, boolean moon, CallbackInfo ci) {
        if (!BondRules.hasOpenedBook(player)) ci.cancel();
    }

    @Inject(method = "handleMoonEndTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$requireBookForMoon(PlayerEntity player, CallbackInfo ci) {
        if (!BondRules.hasOpenedBook(player)) ci.cancel();
    }
}
