package com.bondofthebeast.mixin;

import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.CollarCatalystService;
import com.bondofthebeast.component.ModComponents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.utils.FormUtils;
import net.onixary.shapeShifterCurseFabric.player_form.utils.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.utils.InstinctUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;


@Mixin(value = InstinctUtils.class, remap = false)
public class ForcedInstinctMixin {
    @Inject(method = "getBaseInstinctRate", at = @At("RETURN"), cancellable = true)
    private static void botb$slowReservoir(PlayerEntity player, CallbackInfoReturnable<Float> cir) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(player).nowForm;
        if (form == null || FormUtils.NoInstinct.hasFlag(form)) return;
        if (ForcedTamingService.freeWindow(player)) {
            cir.setReturnValue(0.0f);
            return;
        }
        float rate = cir.getReturnValue();
        if (FormUtils.LockInstinct.hasFlag(form) && CollarCatalystService.gradualFinalStage(player)) rate = 0;
        if (player.isAlive() && !player.isCreative() && !player.isSpectator()) {
            if (ForcedTamingService.sleepingOnPetBed(player)) rate += ForcedTamingService.BED_RATE_PER_TICK;
        }
        cir.setReturnValue(rate);
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/onixary/shapeShifterCurseFabric/player_form/utils/InstinctUtils;calcRate(Ljava/util/HashMap;Z)F"))
    private static float botb$pauseResistanceEffects(HashMap<net.minecraft.util.Identifier, InstinctUtils.InstinctEffect> effects,
                                                   boolean checkExist, Operation<Float> original, @Local ServerPlayerEntity player) {
        return ForcedTamingService.freeWindow(player) ? 0 : original.call(effects, checkExist);
    }
    @Inject(method = "checkThreshold", at = @At("HEAD"), cancellable = true)
    private static void botb$finalStage(ServerPlayerEntity player, float instinctValue, CallbackInfo ci) {
        if (ForcedTamingService.freeWindow(player)) { ci.cancel(); return; }
        if (!CollarCatalystService.gradualFinalStage(player)) return;
        if (instinctValue >= 100) {
            var state = ModComponents.PLAYER_BOND.get(player).getForcedBond();
            if (state.finalWarningTicks < 0) state.finalWarningTicks = ForcedTamingService.WARNING_TICKS;
        }
        ci.cancel();
    }
}
