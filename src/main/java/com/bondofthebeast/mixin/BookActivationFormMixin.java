package com.bondofthebeast.mixin;

import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBase;
import net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms;
import net.onixary.shapeShifterCurseFabric.player_form.ability.PlayerFormComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerFormComponent.class, remap = false)
public abstract class BookActivationFormMixin {
    @Shadow public abstract PlayerFormBase getCurrentForm();

    @Inject(method = "setCurrentForm", at = @At("HEAD"), cancellable = true)
    private void botb$protectUnactivatedForm(PlayerFormBase form, CallbackInfo ci) {
        // Save loading and component synchronization read NBT directly, so existing forms stay intact.
        if (getCurrentForm() == RegPlayerForms.ORIGINAL_BEFORE_ENABLE &&
                form != RegPlayerForms.ORIGINAL_SHIFTER && form != RegPlayerForms.ORIGINAL_BEFORE_ENABLE)
            ci.cancel();
    }
}
