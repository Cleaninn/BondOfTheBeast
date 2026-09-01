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

            double maxDistanceSq = 1024.0;

            for (ServerPlayerEntity potentialPet : user.getServer().getPlayerManager().getPlayerList()) {
                PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(potentialPet);

                if (bond.hasOwner() && bond.getOwnerUUID().equals(ownerUuid)) {
                    anyPetsOnline = true;

                    if (user.getWorld().getRegistryKey() == potentialPet.getWorld().getRegistryKey()
                            && user.squaredDistanceTo(potentialPet) <= maxDistanceSq) {

                        potentialPet.teleport((ServerWorld) world, user.getX(), user.getY(), user.getZ(), user.getYaw(), user.getPitch());
                        ((ServerWorld) world).spawnParticles(ParticleTypes.HEART, potentialPet.getX(), potentialPet.getY() + 1, potentialPet.getZ(), 5, 0.5, 0.5, 0.5, 0.1);

                        potentialPet.sendMessage(Text.translatable("text.bondofthebeast.recalled_by_owner").formatted(Formatting.GOLD), true);
                        foundAny = true;
                    } else {
                        tooFar = true;
                    }
                }
            }

            if (foundAny) {
                user.sendMessage(Text.translatable("text.bondofthebeast.recall_success").formatted(Formatting.GREEN), true);
                user.getItemCooldownManager().set(this, 60);
            } else if (!anyPetsOnline) {
                user.sendMessage(Text.translatable("text.bondofthebeast.no_pets_owned").formatted(Formatting.RED), true);
            } else if (tooFar) {
                user.sendMessage(Text.translatable("text.bondofthebeast.too_far_to_hear").formatted(Formatting.RED), true);
            }
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}