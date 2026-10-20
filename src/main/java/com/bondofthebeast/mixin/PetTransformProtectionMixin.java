package com.bondofthebeast.mixin;

import com.bondofthebeast.BondRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.utils.TransformManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(value = TransformManager.class, remap = false)
public abstract class PetTransformProtectionMixin {
    private static boolean blocked(PlayerEntity player, IForm form) {
        if (!BondRules.blocksHumanReturn(player, form)) return false;
        if (player instanceof ServerPlayerEntity) player.sendMessage(Text.translatable("text.bondofthebeast.cat_bond_blocks_cure"), true);
        return true;
    }

    @Inject(method = "startTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$blockHumanTransformation(PlayerEntity player, IForm form, @Nullable Consumer<TransformManager.PlayerTransformData> onTransformComplete, CallbackInfoReturnable<Boolean> cir) {
        if (blocked(player, form)) cir.cancel();
    }

    @Inject(method = "immediatelyTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$blockDirectHumanForm(PlayerEntity player, IForm form, CallbackInfoReturnable<Boolean> cir) {
        if (blocked(player, form)) cir.cancel();
    }
}
