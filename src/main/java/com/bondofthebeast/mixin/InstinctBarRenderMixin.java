package com.bondofthebeast.mixin;

import com.bondofthebeast.CollarCatalystService;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.utils.FormUtils;
import net.onixary.shapeShifterCurseFabric.player_form.utils.InstinctBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = InstinctBarRenderer.class, remap = false)
public class InstinctBarRenderMixin {
    @WrapOperation(method = "renderInstinctBar", at = @At(value = "INVOKE", target = "Lnet/onixary/shapeShifterCurseFabric/player_form/utils/FormUtils$FlagData;hasFlag(Lnet/onixary/shapeShifterCurseFabric/player_form/IForm;)Z"))
    private static boolean botb$allowhighformInstinctRender(FormUtils.FlagData instance, IForm form, Operation<Boolean> original, @Local(argsOnly = true) PlayerEntity player) {
        return instance == FormUtils.LockInstinct && CollarCatalystService.gradualFinalStage(player)
                ? false : original.call(instance, form);
    }


}
