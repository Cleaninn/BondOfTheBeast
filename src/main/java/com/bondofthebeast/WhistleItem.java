package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class WhistleItem extends Item {

    public WhistleItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            String ownerUuid = user.getUuidAsString();
            boolean foundAny = false;
            boolean tooFar = false;
            boolean anyPetsOnline = false;
            boolean triggerLongCooldown = false;

            for (ServerPlayerEntity potentialPet : user.getServer().getPlayerManager().getPlayerList()) {
                PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(potentialPet);

                if (bond.hasOwner() && bond.getOwnerUUID().equals(ownerUuid)) {
                    if (!BondRules.canOwn(user) || !BondRules.canObey(potentialPet) ||
                            !BondRules.hasCollar(potentialPet) || LeashManager.isTethered(potentialPet) || bond.isAbsorbed()) continue;
                    anyPetsOnline = true;

                    boolean sameDimension = potentialPet.getWorld() == world;
                    double distance = potentialPet.distanceTo(user);
                    boolean canRecall = BondRules.allows(potentialPet, "tp") && potentialPet.getWorld() == world && potentialPet.squaredDistanceTo(user) <= 64 * 64;

                    if (canRecall) {
                        if (!LeashManager.teleportSafely(potentialPet, (ServerWorld) world, user.getPos())) continue;
                        bond.setSitting(false);
                        ((ServerWorld) world).spawnParticles(ParticleTypes.HEART, potentialPet.getX(), potentialPet.getY() + 1, potentialPet.getZ(), 5, 0.5, 0.5, 0.5, 0.1);

                        potentialPet.sendMessage(Text.translatable("text.bondofthebeast.recalled_by_owner").formatted(Formatting.GOLD), true);
                        foundAny = true;

                        // Если телепорт сработал на огромное расстояние или между мирами - взводим триггер КД
                        if (!sameDimension || distance > 1000.0) {
                            triggerLongCooldown = true;
                        }
                    } else {
                        tooFar = true;
                    }
                }
            }

            if (foundAny) {
                user.sendMessage(Text.translatable("text.bondofthebeast.recall_success").formatted(Formatting.GREEN), true);

                // Проверяем, нужен ли долгий откат
                if (triggerLongCooldown) {
                    user.getItemCooldownManager().set(this, 20 * 60 * 20); // 20 минут (24000 тиков)
                    user.sendMessage(Text.translatable("text.bondofthebeast.recall_cooldown_long").formatted(Formatting.YELLOW), false);
                } else {
                    user.getItemCooldownManager().set(this, 60); // Стандартный кулдаун (3 секунды)
                }
            } else if (!anyPetsOnline) {
                user.sendMessage(Text.translatable("text.bondofthebeast.no_pets_owned").formatted(Formatting.RED), true);
            } else if (tooFar) {
                user.sendMessage(Text.translatable("text.bondofthebeast.too_far_to_hear").formatted(Formatting.RED), true);
            }
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}