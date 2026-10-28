package com.bondofthebeast.mixin;

import com.bondofthebeast.BondRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.utils.TransformManager;
import net.onixary.shapeShifterCurseFabric.player_form.utils.InstinctUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(value = TransformManager.class, remap = false)
public abstract class PetTransformProtectionMixin {
    @Shadow @Final private static java.util.HashMap<java.util.UUID, TransformManager.PlayerTransformData> playerData;

    private static boolean blocked(PlayerEntity player, IForm form) {
        if (!BondRules.blocksHumanReturn(player, form)) return false;
        if (player instanceof ServerPlayerEntity) player.sendMessage(Text.translatable("text.bondofthebeast.cat_bond_blocks_cure"), true);
        return true;
    }

    @Inject(method = "startTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$blockHumanTransformation(PlayerEntity player, IForm form, @Nullable Consumer<TransformManager.PlayerTransformData> onTransformComplete, CallbackInfoReturnable<Boolean> cir) {
        if (blocked(player, form)) cir.setReturnValue(false);
    }

    @Inject(method = "immediatelyTransform", at = @At("HEAD"), cancellable = true)
    private static void botb$blockDirectHumanForm(PlayerEntity player, IForm form, CallbackInfoReturnable<Boolean> cir) {
        if (blocked(player, form)) cir.setReturnValue(false);
    }

    @Inject(method = "immediatelyTransform", at = @At("RETURN"))
    private static void botb$finishDirectTransform(PlayerEntity player, IForm form, CallbackInfoReturnable<Boolean> cir) {
        // An immediate change skips SSC's animation-end callback, which normally releases this lock.
        var data = playerData.get(player.getUuid());
        if (cir.getReturnValue() && data != null && data.transformTimer < 0)
            InstinctUtils.playerInstinctLock.remove(player.getUuid());
    }
}
