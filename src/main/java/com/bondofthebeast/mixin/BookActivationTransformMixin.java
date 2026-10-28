package com.bondofthebeast.mixin;

import com.bondofthebeast.BondRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms;
import net.onixary.shapeShifterCurseFabric.player_form.utils.TransformManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(value = TransformManager.class, remap = false)
public class BookActivationTransformMixin {
    @Inject(method = "startTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$requireBook(PlayerEntity player, IForm form, @Nullable Consumer<TransformManager.PlayerTransformData> onTransformComplete, CallbackInfoReturnable<Boolean> cir) {
        // Keep the introductory book's transition to the enabled human form available.
        if (!BondRules.hasOpenedBook(player) && form != RegPlayerForms.ORIGINAL_SHIFTER && form != RegPlayerForms.ORIGINAL_BEFORE_ENABLE)
            cir.setReturnValue(false);
    }

    @Inject(method = "immediatelyTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$requireBookForDirectForm(PlayerEntity player, IForm form, CallbackInfoReturnable<Boolean> cir) {
        if (!BondRules.hasOpenedBook(player) && form != RegPlayerForms.ORIGINAL_SHIFTER && form != RegPlayerForms.ORIGINAL_BEFORE_ENABLE)
            cir.setReturnValue(false);
    }
}




