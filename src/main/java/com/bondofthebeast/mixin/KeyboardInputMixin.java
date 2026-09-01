package com.bondofthebeast.mixin;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void blockSittingInput(boolean slowDown, float f, CallbackInfo ci) {
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player != null) {
            var bond = ModComponents.PLAYER_BOND.get(player);

            // Блокируем управление ТОЛЬКО если питомец сидит или поглощен в посох
            if (bond.isSitting() || bond.isAbsorbed()) {
                KeyboardInput input = (KeyboardInput) (Object) this;
                input.movementForward = 0.0f;
                input.movementSideways = 0.0f;
                input.pressingForward = false;
                input.pressingBack = false;
                input.pressingLeft = false;
                input.pressingRight = false;
                input.jumping = false;

                if (bond.isAbsorbed()) {
                    input.sneaking = false;
                } else {
                    input.sneaking = true;
                }
            }
        }
    }
}