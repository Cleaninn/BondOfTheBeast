package com.bondofthebeast.mixin;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.ForcedTamingService;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.utils.FormUtils;
import net.onixary.shapeShifterCurseFabric.player_form.utils.InstinctBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InstinctBarRenderer.class)
public class InstinctBarRenderMixin {
    @WrapOperation(method = "renderInstinctBar", at = @At(value = "INVOKE", target = "Lnet/onixary/shapeShifterCurseFabric/player_form/utils/FormUtils$FlagData;hasFlag(Lnet/onixary/shapeShifterCurseFabric/player_form/IForm;)Z"))
    private static boolean botb$allowhighformInstinctRender(FormUtils.FlagData instance, IForm form, Operation<Boolean> original, @Local(name = "player") PlayerEntity player) {
        if (ForcedTamingService.infused(player)) {
            return false;
        }
        BondOfTheBeast.LOGGER.info("def Instinct{}", original.call(instance, form));
        return original.call(instance, form);
    }


}
