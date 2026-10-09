package com.bondofthebeast.mixin;

import com.bondofthebeast.ForcedTamingService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class PetArmorLockMixin {
    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void botb$lockedArmor(int slot, int button, SlotActionType action, PlayerEntity player, CallbackInfo ci) {
        if ((Object) this instanceof PlayerScreenHandler && ForcedTamingService.armorLocked(player)) {
            boolean armorSlot = slot >= 5 && slot <= 8;
            boolean quickArmor = action == SlotActionType.QUICK_MOVE && slot >= 0 && slot < ((ScreenHandler) (Object) this).slots.size() &&
                    ((ScreenHandler) (Object) this).slots.get(slot).getStack().getItem() instanceof net.minecraft.item.ArmorItem;
            if (armorSlot || quickArmor) {
                player.currentScreenHandler.sendContentUpdates();
                ci.cancel();
            }
        }
    }
}
