package com.bondofthebeast.mixin;

import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.CollarCatalystService;
import com.bondofthebeast.component.ModComponents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.utils.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.utils.InstinctUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.UUID;


@Mixin(value = InstinctUtils.class, remap = false)
public class ForcedInstinctMixin {

    @WrapOperation(method = "serverTick", at = @At(value = "FIELD", target = "Lnet/onixary/shapeShifterCurseFabric/player_form/utils/InstinctUtils;playerInstinctLock:Ljava/util/HashMap;"))
    private static HashMap<UUID, Boolean> botb$slowReservoir(Operation<HashMap<UUID, Boolean>> original, @Local ServerPlayerEntity player) {
        if (ForcedTamingService.infused(player) && !ForcedTamingService.freeWindow(player)) {
            HashMap hash = original.call();
            hash.put(player.getUuid(), false);
            return hash;
        }
        return original.call();
    }

    @Inject(method = "checkThreshold", at = @At("HEAD"), cancellable = true)
    private static void botb$finalStage(ServerPlayerEntity player, float instinctValue, CallbackInfo ci) {
        if (!CollarCatalystService.gradualFinalStage(player)) return;
        if (instinctValue >= 100 && !ForcedTamingService.freeWindow(player)) {
            var group = RegPlayerFormComponent.PLAYER_FORM.get(player).nowForm.getFormGroup();
            if (group != null && group.getFormList(3) != null) {
                var state = ModComponents.PLAYER_BOND.get(player).getForcedBond();
                if (state.finalWarningTicks < 0) state.finalWarningTicks = ForcedTamingService.WARNING_TICKS;
            }
        }
        ci.cancel();
    }
}
