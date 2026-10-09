package com.bondofthebeast.mixin;

import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.instinct.InstinctTicker;
import net.onixary.shapeShifterCurseFabric.player_form.instinct.PlayerInstinctComponent;
import net.onixary.shapeShifterCurseFabric.player_form.transform.TransformManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InstinctTicker.class, remap = false)
public class ForcedInstinctMixin {
    @Inject(method = "calculateCurrentRate", at = @At("RETURN"), cancellable = true)
    private static void botb$slowReservoir(PlayerEntity player, PlayerInstinctComponent comp, CallbackInfoReturnable<Float> cir) {
        int stage = ForcedTamingService.stage(player);
        float rate = cir.getReturnValue();
        boolean forced = ModComponents.PLAYER_BOND.get(player).getForcedBond().forced;
        if (stage >= 0 && stage < 3 && ForcedTamingService.sleepingOnPetBed(player) && player.isAlive() &&
                !player.isCreative() && !player.isSpectator()) rate += ForcedTamingService.BED_RATE_PER_TICK;
        if (!forced) { cir.setReturnValue(rate); return; }
        // SSC fills the native stage-2 bar immediately. Forced bonds need a real escape period.
        if (stage == 2) rate -= 100;
        if (ForcedTamingService.freeWindow(player)) rate = 0;
        else if (stage >= 0 && stage < 3 && ForcedTamingService.infused(player))
            rate += ForcedTamingService.INFUSED_RATE_PER_TICK;
        cir.setReturnValue(rate);
    }

    @Inject(method = "checkThreshold", at = @At("HEAD"), cancellable = true)
    private static void botb$finalStage(ServerPlayerEntity player, PlayerInstinctComponent comp, CallbackInfo ci) {
        if (!ModComponents.PLAYER_BOND.get(player).getForcedBond().forced || ForcedTamingService.stage(player) != 2) return;
        if (comp.instinctValue >= 100 && !ForcedTamingService.freeWindow(player)) {
            var group = RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm().getGroup();
            if (group != null && group.hasForm(3)) {
                var state = ModComponents.PLAYER_BOND.get(player).getForcedBond();
                if (state.finalWarningTicks < 0) state.finalWarningTicks = ForcedTamingService.WARNING_TICKS;
            }
        }
        ci.cancel();
    }
}
