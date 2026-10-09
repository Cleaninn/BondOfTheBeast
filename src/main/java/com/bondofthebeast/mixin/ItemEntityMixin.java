package com.bondofthebeast.mixin;

import com.bondofthebeast.BondService;
import com.bondofthebeast.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void botb$releaseContract(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) return;
        if (!((Object) this instanceof ItemEntity entity) || entity.getWorld().isClient || entity.getServer() == null) return;
        var stack = entity.getStack();
        if (stack.isEmpty() || !stack.isOf(ModItems.FIDELITY_CONTRACT) || !stack.hasNbt()) return;
        var nbt = stack.getNbt();
        if (nbt.contains("PetUUID") && nbt.contains("OwnerUUID"))
            BondService.releaseContract(entity.getServer(), nbt.getString("PetUUID"), nbt.getString("OwnerUUID"), nbt.getString("ForcedBondToken"));
    }
}
