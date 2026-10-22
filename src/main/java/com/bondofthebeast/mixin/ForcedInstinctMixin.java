package com.bondofthebeast.mixin;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.CollarCatalystService;
import com.bondofthebeast.component.ModComponents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.utils.FormUtils;
import net.onixary.shapeShifterCurseFabric.player_form.utils.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.utils.InstinctUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.UUID;


@Mixin(value = InstinctUtils.class, remap = false)
public class ForcedInstinctMixin {
    @WrapOperation(method = "serverTick", at = @At(value = "FIELD", target = "Lnet/onixary/shapeShifterCurseFabric/player_form/utils/InstinctUtils;playerInstinctLock:Ljava/util/HashMap;"))
    private static HashMap<UUID, Boolean> botb$slowReservoir(Operation<HashMap<UUID, Boolean>> original, @Local(name = "player") ServerPlayerEntity player) {
        if (ForcedTamingService.infused(player)) {
            HashMap<UUID, Boolean> hash = original.call();
            hash.put(player.getUuid(), false);
            return hash;
        }
        return original.call();
    }
    @WrapOperation(method = "getBaseInstinctRate", at = @At(value = "INVOKE", target = "Lnet/onixary/shapeShifterCurseFabric/player_form/utils/FormUtils$FlagData;hasFlag(Lnet/onixary/shapeShifterCurseFabric/player_form/IForm;)Z"))
    private static boolean botb$allowhighformInstinct(FormUtils.FlagData instance, IForm form, Operation<Boolean> original, @Local(name = "player") PlayerEntity player) {
        if (ForcedTamingService.infused(player)) {
            return false;
        }
        return original.call(instance, form);
    }
    @Inject(method = "checkThreshold", at = @At("HEAD"), cancellable = true)
    private static void botb$finalStage(ServerPlayerEntity player, float instinctValue, CallbackInfo ci) {
        if (!CollarCatalystService.gradualFinalStage(player)) return;
        if (instinctValue >= 100 && !ForcedTamingService.freeWindow(player)) {
            var group = RegPlayerFormComponent.PLAYER_FORM.get(player).nowForm.getFormGroup();
            if (group != null && group.getFormList(4) != null) {
                var state = ModComponents.PLAYER_BOND.get(player).getForcedBond();
                if (state.finalWarningTicks < 0) state.finalWarningTicks = ForcedTamingService.WARNING_TICKS;
            }
        }
        ci.cancel();
    }
}
