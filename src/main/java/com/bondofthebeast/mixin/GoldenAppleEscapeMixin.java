package com.bondofthebeast.mixin;

import com.bondofthebeast.BondService;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class GoldenAppleEscapeMixin {
    @Inject(method = "eatFood", at = @At("HEAD"))
    private void botb$escape(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof ServerPlayerEntity player &&
                com.bondofthebeast.ForcedTamingService.escapeApple(player, stack)) {
            BondService.release(player);
            player.sendMessage(Text.translatable("text.bondofthebeast.taming_escaped"), false);
        }
    }
}
