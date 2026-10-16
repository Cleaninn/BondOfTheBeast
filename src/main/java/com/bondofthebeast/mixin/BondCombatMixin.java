package com.bondofthebeast.mixin;

import com.bondofthebeast.BondRules;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class BondCombatMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void botb$protectBond(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) return;
        if ((Object) this instanceof ServerPlayerEntity target &&
                BondRules.owns(target, attacker)) {
            cir.setReturnValue(false);
            return;
        }
        var bond = ModComponents.PLAYER_BOND.get(attacker);
        if (bond.hasOwner() && BondRules.canObey(attacker) && bond.isPacifistMode()) cir.setReturnValue(false);
    }

}
