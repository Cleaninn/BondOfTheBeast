package com.bondofthebeast.mixin;

import com.bondofthebeast.BondRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBase;
import net.onixary.shapeShifterCurseFabric.player_form.transform.TransformManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TransformManager.class, remap = false)
public abstract class PetTransformProtectionMixin {
    private static boolean blocked(PlayerEntity player, PlayerFormBase form) {
        if (!BondRules.blocksHumanReturn(player, form)) return false;
        if (player instanceof ServerPlayerEntity) player.sendMessage(Text.translatable("text.bondofthebeast.cat_bond_blocks_cure"), true);
        return true;
    }

    @Inject(method = "handleDirectTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$blockHumanTransformation(PlayerEntity player, PlayerFormBase form, boolean cured, CallbackInfo ci) {
        if (blocked(player, form)) ci.cancel();
    }

    @Inject(method = "setFormDirectly", at = @At("HEAD"), cancellable = true)
    private static void botb$blockDirectHumanForm(PlayerEntity player, PlayerFormBase form, CallbackInfo ci) {
        if (blocked(player, form)) ci.cancel();
    }
}
