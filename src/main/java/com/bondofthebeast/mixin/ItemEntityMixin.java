package com.bondofthebeast.mixin;

import com.bondofthebeast.ModItems;
import com.bondofthebeast.CollarItem;
import com.bondofthebeast.component.ModComponents;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// Целимся в базовый класс Entity
@Mixin(Entity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "discard", at = @At("HEAD"))
    private void onDiscardContract(CallbackInfo ci) {
        // Проверяем, является ли исчезающая сущность выброшенным предметом
        if ((Object) this instanceof ItemEntity itemEntity) {
            if (itemEntity.getWorld().isClient()) return;

            ItemStack stack = itemEntity.getStack();

            // Если предмет исчезает, но внутри всё еще лежит предмет (он не был подобран игроком)
            if (!stack.isEmpty() && stack.isOf(ModItems.FIDELITY_CONTRACT)) {
                NbtCompound nbt = stack.getNbt();
                if (nbt != null && nbt.contains("PetUUID") && nbt.contains("OwnerUUID")) {
                    String petUuidStr = nbt.getString("PetUUID");
                    String ownerUuidStr = nbt.getString("OwnerUUID");
                    String petName = nbt.getString("PetName");

                    if (itemEntity.getServer() == null) return;

                    try {
                        ServerPlayerEntity pet = itemEntity.getServer().getPlayerManager().getPlayer(UUID.fromString(petUuidStr));
                        ServerPlayerEntity owner = itemEntity.getServer().getPlayerManager().getPlayer(UUID.fromString(ownerUuidStr));

                        // Освобождаем питомца
                        if (pet != null) {
                            var petBond = ModComponents.PLAYER_BOND.get(pet);
                            if (petBond.hasOwner() && petBond.getOwnerUUID().equals(ownerUuidStr)) {
                                if (petBond.isAbsorbed()) {
                                    pet.changeGameMode(GameMode.SURVIVAL);
                                    petBond.setAbsorbed(false);
                                }
                                petBond.setBedPos(null);
                                petBond.clearOwner();

                                // Снимаем ошейник с шеи
                                TrinketsApi.getTrinketComponent(pet).ifPresent(c -> {
                                    c.getInventory().values().forEach(g -> g.values().forEach(inv -> {
                                        for (int i = 0; i < inv.size(); i++) {
                                            if (inv.getStack(i).getItem() instanceof CollarItem) {
                                                ItemStack dropped = inv.getStack(i).copy();
                                                if (dropped.hasNbt()) dropped.getNbt().remove("OwnerName");
                                                pet.dropItem(dropped, true);
                                                inv.setStack(i, ItemStack.EMPTY);
                                            }
                                        }
                                    }));
                                });

                                ModComponents.PLAYER_BOND.sync(pet);
                                pet.sendMessage(Text.translatable("text.bondofthebeast.contract_destroyed_pet").formatted(Formatting.DARK_RED), false);
                            }
                        }

                        // Уведомляем хозяина и убираем питомца из его гримуара
                        if (owner != null) {
                            var ownerBond = ModComponents.PLAYER_BOND.get(owner);
                            if (ownerBond.getRegisteredPets().containsKey(petUuidStr)) {
                                ownerBond.removePetFromRegistry(petUuidStr);
                                ModComponents.PLAYER_BOND.sync(owner);
                                owner.sendMessage(Text.translatable("text.bondofthebeast.contract_destroyed_owner", petName).formatted(Formatting.DARK_RED), false);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}