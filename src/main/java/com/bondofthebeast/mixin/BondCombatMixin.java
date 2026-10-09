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
                (BondRules.owns(attacker, target) || BondRules.owns(target, attacker))) {
            cir.setReturnValue(false);
            return;
        }
        var bond = ModComponents.PLAYER_BOND.get(attacker);
        if (bond.hasOwner() && BondRules.canObey(attacker) && bond.isPacifistMode()) cir.setReturnValue(false);
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void botb$vampirism(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || amount <= 0 || !(source.getAttacker() instanceof ServerPlayerEntity pet)) return;
        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (!bond.hasOwner() || !BondRules.canObey(pet) || !bond.isVampiricMode() ||
                !BondRules.allows(pet, "vampiric") || !BondRules.hasCollar(pet)) return;
        float heal = Math.min(2, amount * 0.2f);
        pet.heal(heal);
        var owner = BondRules.owner(pet);
        if (owner != null && owner.getWorld() == pet.getWorld() && owner.squaredDistanceTo(pet) <= 144) owner.heal(heal * 0.5f);
    }
}
