package com.bondofthebeast.mixin;

import com.bondofthebeast.client.BondHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class CollaringHudMixin {
    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void botb$hideItemCaptionWhileCollaring(DrawContext context, CallbackInfo ci) {
        if (BondHud.isCollaringVisible()) ci.cancel();
    }
}
