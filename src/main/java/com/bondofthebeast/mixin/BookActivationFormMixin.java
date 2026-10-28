package com.bondofthebeast.mixin;

import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms;
import com.bondofthebeast.BondRules;
import net.minecraft.entity.player.PlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.utils.FormUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FormUtils.class, remap = false)
public abstract class BookActivationFormMixin {
    private static boolean botb$requiresBook(PlayerEntity player, IForm form) {
        return !BondRules.hasOpenedBook(player) &&
                form != RegPlayerForms.ORIGINAL_SHIFTER && form != RegPlayerForms.ORIGINAL_BEFORE_ENABLE;
    }

    @Inject(method = {"setForm", "_setForm"}, at = @At("HEAD"), cancellable = true)
    private static void botb$protectUnactivatedForm(PlayerEntity player, IForm form, CallbackInfo ci) {
        if (botb$requiresBook(player, form)) ci.cancel();
    }

    @Inject(method = "_loadForm", at = @At("HEAD"), cancellable = true)
    private static void botb$protectUnactivatedPowers(PlayerEntity player, IForm form, boolean shouldReload, CallbackInfo ci) {
        if (botb$requiresBook(player, form)) ci.cancel();
    }
}


