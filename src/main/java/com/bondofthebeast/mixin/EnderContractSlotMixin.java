package com.bondofthebeast.mixin;

import com.bondofthebeast.BondConfig;
import com.bondofthebeast.ContractStorageRules;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class EnderContractSlotMixin {
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void botb$keepContractsAccessible(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (((Slot) (Object) this).inventory instanceof EnderChestInventory &&
                BondConfig.get().blockSignedContractsInEnderChests && ContractStorageRules.containsSignedContract(stack))
            cir.setReturnValue(false);
    }
}
