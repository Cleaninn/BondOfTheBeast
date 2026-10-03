package com.bondofthebeast.mixin;

import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.item.InfusedCollarItem;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class GoldenAppleEscapeMixin {

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void onEatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!world.isClient() && stack.isOf(Items.GOLDEN_APPLE)) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            var bond = ModComponents.PLAYER_BOND.get(player);

            // Если игрок находится в процессе подавления воли
            if (bond.getTamingState() == 1) {

                TrinketsApi.getTrinketComponent(player).ifPresent(c -> {
                    var necklaceInv = c.getInventory().get("chest").get("necklace");
                    if (necklaceInv != null) {
                        for (int i = 0; i < necklaceInv.size(); i++) {
                            // Ищем пропитанный ошейник и уничтожаем его
                            if (necklaceInv.getStack(i).getItem() instanceof InfusedCollarItem) {
                                necklaceInv.setStack(i, ItemStack.EMPTY);

                                // Сбрасываем статус приручения (цепи рвутся)
                                bond.setTamingState(0);
                                bond.clearOwner();

                                // Звук разбитого стекла/магии и сообщение
                                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.5f, 0.5f);
                                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.8f);
                                player.sendMessage(Text.translatable("text.bondofthebeast.taming_escaped").formatted(Formatting.GREEN), true);
                                break;
                            }
                        }
                    }
                });
            }
        }
    }
}