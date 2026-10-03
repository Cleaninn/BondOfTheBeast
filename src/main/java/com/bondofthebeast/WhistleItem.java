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
                    anyPetsOnline = true;

                    int bondLevel = bond.getBondLevel();
                    boolean sameDimension = user.getWorld().getRegistryKey() == potentialPet.getWorld().getRegistryKey();
                    double distanceSq = sameDimension ? user.squaredDistanceTo(potentialPet) : Double.MAX_VALUE;
                    double distance = sameDimension ? Math.sqrt(distanceSq) : Double.MAX_VALUE;

                    boolean canRecall = false;

                    // Увеличенные требования по уровням связи
                    if (bondLevel >= 15) {
                        canRecall = true; // Межизмерение
                    } else if (bondLevel >= 10) {
                        canRecall = sameDimension; // Бесконечное расстояние в своём измерении
                    } else if (bondLevel >= 8) {
                        canRecall = sameDimension && distance <= 1000.0;
                    } else if (bondLevel >= 6) {
                        canRecall = sameDimension && distance <= 256.0;
                    } else if (bondLevel >= 4) {
                        canRecall = sameDimension && distance <= 128.0;
                    } else if (bondLevel >= 2) {
                        canRecall = sameDimension && distance <= 64.0;
                    } else {
                        canRecall = sameDimension && distance <= 32.0;
                    }

                    if (canRecall) {
                        potentialPet.teleport((ServerWorld) world, user.getX(), user.getY(), user.getZ(), user.getYaw(), user.getPitch());
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