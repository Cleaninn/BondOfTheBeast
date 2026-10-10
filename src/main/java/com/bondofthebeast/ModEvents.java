package com.bondofthebeast;

import com.bondofthebeast.block.PetBedBlock;
import com.bondofthebeast.block.PetBedBlockEntity;
import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.UUID;

public class ModEvents {
    private static int tickCounter = 0;
    public static boolean canPetObey(PlayerEntity pet) {
        return BondRules.canObey(pet);
    }

    public static void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof PlayerEntity pet)) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            var bond = ModComponents.PLAYER_BOND.get(pet);

            if (bond.hasOwner() && bond.getOwnerUUID().equals(player.getUuidAsString())) {
                ItemStack foodStack = player.getStackInHand(hand);
                if (ForcedTamingService.isCatalyst(foodStack)) {
                    if (world.isClient) return ActionResult.SUCCESS;
                    return ForcedTamingService.feedCatalyst(player, (ServerPlayerEntity) pet, foodStack);
                }


                if (player.isSneaking() && foodStack.isEmpty() && bond.getLeashPos() == null && bond.getLeashHolder().isEmpty()) {
                    if (world.isClient) return ActionResult.SUCCESS;
                    if (bond.getForcedBond().forced && ForcedTamingService.stage(pet) >= 3) {
                        player.sendMessage(Text.translatable("text.bondofthebeast.destroy_contract_first"), true);
                        return ActionResult.FAIL;
                    }
                    if (bond.getForcedBond().forced && pet instanceof ServerPlayerEntity sp) {
                        BondService.release(sp);
                        return ActionResult.SUCCESS;
                    }
                    return TrinketsApi.getTrinketComponent(pet).map(c -> {
                        boolean rem = false;
                        for (var g : c.getInventory().values()) for (var inv : g.values()) for (int i = 0; i < inv.size(); i++) {
                            if (inv.getStack(i).getItem() instanceof CollarItem) {
                                ItemStack droppedCollar = inv.getStack(i).copy();
                                if (droppedCollar.hasNbt()) droppedCollar.getNbt().remove("OwnerName");
                                player.getInventory().offerOrDrop(droppedCollar);
                                inv.setStack(i, ItemStack.EMPTY);
                                rem = true;
                            }
                        }
                        if (rem) {
                            if (bond.isAbsorbed() && pet instanceof ServerPlayerEntity sp) {
                                BondService.releaseAbsorption(sp);
                            }
                            if (pet instanceof ServerPlayerEntity sp) { BondService.clearBed(sp); LeashManager.detach(sp); }
                            bond.setPetNickname(null);
                            player.sendMessage(Text.translatable("text.bondofthebeast.collar_removed_owner", pet.getName().getString()).formatted(Formatting.AQUA), true);
                            pet.sendMessage(Text.translatable("text.bondofthebeast.collar_removed_pet", player.getName().getString()).formatted(Formatting.AQUA), true);
                            return ActionResult.SUCCESS;
                        }
                        return ActionResult.PASS;
                    }).orElse(ActionResult.PASS);
                }

                if (!foodStack.isEmpty() && foodStack.isOf(ModItems.PET_TREAT)) {
                    if (world.isClient) return ActionResult.SUCCESS;
                    if (!player.getAbilities().creativeMode) foodStack.decrement(1);
                    pet.getHungerManager().add(6, 0.6f);

                    ModComponents.PLAYER_BOND.sync(pet);
                    ModComponents.PLAYER_BOND.sync(player);
                    world.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.1f);
                    if (pet instanceof ServerPlayerEntity sp) sp.getServerWorld().spawnParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + 1.2, pet.getZ(), 12, 0.4, 0.4, 0.4, 0.15);
                    String petName = bond.getPetNickname() != null ? bond.getPetNickname() : pet.getGameProfile().getName();
                    player.sendMessage(Text.translatable("text.bondofthebeast.event.feed_treat_owner", petName).formatted(Formatting.AQUA), true);
                    pet.sendMessage(Text.translatable("text.bondofthebeast.event.feed_treat_pet", player.getGameProfile().getName()).formatted(Formatting.AQUA), true);
                    if (player instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "owner_story/treat");
                    if (pet instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "pet_story/treat");
                    return ActionResult.SUCCESS;
                }

                if (!foodStack.isEmpty() && foodStack.getItem().isFood()) {
                    if (!pet.getHungerManager().isNotFull()) {
                        if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.event.pet_not_hungry").formatted(Formatting.YELLOW), true);
                        return ActionResult.SUCCESS;
                    }
                    if (world.isClient) return ActionResult.SUCCESS;
                    var foodComponent = foodStack.getItem().getFoodComponent();
                    if (foodComponent != null) {
                        int hungerValue = foodComponent.getHunger();
                        if (!player.getAbilities().creativeMode) foodStack.decrement(1);
                        pet.getHungerManager().add(hungerValue, foodComponent.getSaturationModifier());

                        ModComponents.PLAYER_BOND.sync(pet);
                        ModComponents.PLAYER_BOND.sync(player);
                        world.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        world.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5f, 1.0f);
                        if (pet instanceof ServerPlayerEntity sp) sp.getServerWorld().spawnParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + 1.2, pet.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
                        String petName = bond.getPetNickname() != null ? bond.getPetNickname() : pet.getGameProfile().getName();
                        player.sendMessage(Text.translatable("text.bondofthebeast.event.feed_food_owner", petName).formatted(Formatting.GREEN), true);
                        pet.sendMessage(Text.translatable("text.bondofthebeast.event.feed_food_pet", player.getGameProfile().getName()).formatted(Formatting.GOLD), true);
                        if (player instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "owner_story/treat");
                        if (pet instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "pet_story/treat");
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            var bond = ModComponents.PLAYER_BOND.get(sp);
            if (entity instanceof PlayerEntity targetPlayer) {
                if (bond.hasOwner() && bond.getOwnerUUID().equals(targetPlayer.getUuidAsString())) {
                    sp.sendMessage(Text.translatable("text.bondofthebeast.damage_blocked_pet").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
                var targetBond = ModComponents.PLAYER_BOND.get(targetPlayer);
                if (targetBond.hasOwner() && targetBond.getOwnerUUID().equals(sp.getUuidAsString())) {
                    sp.sendMessage(Text.translatable("text.bondofthebeast.damage_blocked_owner").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
            }
            if (bond.hasOwner() && bond.isPacifistMode() && canPetObey(sp)) {
                sp.sendMessage(Text.translatable("text.bondofthebeast.command.pacifist_warning").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            if (!(entity instanceof LivingEntity targetLiving)) return ActionResult.PASS;
            for (ServerPlayerEntity p : sp.getServer().getPlayerManager().getPlayerList()) {
                PlayerBondComponent pBond = ModComponents.PLAYER_BOND.get(p);
                if (pBond.hasOwner() && pBond.getOwnerUUID().equals(sp.getUuidAsString()) && pBond.isProtectionMode() && BondRules.allows(p, "prot") && canPetObey(p)) {
                    targetLiving.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, false));
                }
            }
            return ActionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (!BondRules.allows(player, "nobreak")) return ActionResult.PASS;
            net.minecraft.block.BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof PetBedBlock) {
                BlockPos headPos = state.get(PetBedBlock.PART) == net.minecraft.block.enums.BedPart.HEAD ? pos : pos.offset(state.get(PetBedBlock.FACING));
                if (world.getBlockEntity(headPos) instanceof PetBedBlockEntity bed) {
                    if (player.getUuidAsString().equals(bed.getBoundPetUUID())) {
                        if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.cannot_break_own_bed").formatted(Formatting.RED), true);
                        return ActionResult.FAIL;
                    }
                }
            }
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            if (bond.getBlacklistedBlocks().contains(blockId)) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.blacklisted_warning").formatted(Formatting.DARK_RED), true);
                return ActionResult.FAIL;
            }
            if (bond.isNoBreakMode() && !bond.getWhitelistedBlocks().contains(blockId)) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.soft_paws_warning").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (!BondRules.allows(player, "nobreak")) return true;
            if (state.getBlock() instanceof PetBedBlock) {
                BlockPos headPos = state.get(PetBedBlock.PART) == net.minecraft.block.enums.BedPart.HEAD ? pos : pos.offset(state.get(PetBedBlock.FACING));
                if (world.getBlockEntity(headPos) instanceof PetBedBlockEntity bed) {
                    if (player.getUuidAsString().equals(bed.getBoundPetUUID())) {
                        if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.cannot_break_own_bed").formatted(Formatting.RED), true);
                        return false;
                    }
                }
            }
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            if (bond.getBlacklistedBlocks().contains(blockId)) return false;
            if (bond.isNoBreakMode() && !bond.getWhitelistedBlocks().contains(blockId)) return false;
            return true;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (bond.hasOwner() && canPetObey(player) && world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.cannot_sleep_here_human").formatted(Formatting.YELLOW), true);
                return ActionResult.FAIL;
            }
            if (!BondRules.allows(player, "nobreak")) return ActionResult.PASS;
            String blockId = Registries.BLOCK.getId(world.getBlockState(hitResult.getBlockPos()).getBlock()).toString();
            if (bond.getBlacklistedBlocks().contains(blockId)) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.blacklisted_interact_warning").formatted(Formatting.DARK_RED), true);
                return ActionResult.FAIL;
            }
            if (bond.isNoInteractMode()) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.event.interact_blocked").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {

            // --- 1. НОВАЯ ЛОГИКА: СИСТЕМА ПОДАВЛЕНИЯ ВОЛИ (Срабатывает 2 раза в секунду) ---
            if (++tickCounter >= 20) {
                tickCounter = 0;
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    var ownerBond = ModComponents.PLAYER_BOND.get(player);
                    if (!ownerBond.hasOwner() || !canPetObey(player) || !BondRules.hasCollar(player)) continue;

                    ServerPlayerEntity owner = BondRules.owner(player);
                    if (owner == null || owner.getWorld() != player.getWorld()) continue;

                    if (ownerBond.isAbsorbed()) {
                        if (player.interactionManager.getGameMode() != GameMode.SPECTATOR) player.changeGameMode(GameMode.SPECTATOR);
                        if (player.getCameraEntity() != owner) player.setCameraEntity(owner);
                        continue;
                    }

                    double d = player.squaredDistanceTo(owner);
                    if (!ownerBond.isSitting() && ownerBond.isTeleportEnabled() && BondRules.allows(player, "tp") && !LeashManager.isTethered(player) && d > 400.0 && d <= 10000.0) {
                        tryTeleportPet(player, owner);
                    }

                    if (d <= 144.0 && BondRules.allows(player, "aura") && ownerBond.isAuraEnabled()) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true));
                        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, true));
                    }

                    if (BondRules.allows(player, "prot") && ownerBond.isProtectionMode()) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, false, true));
                    }
                }
            }


        });
    }

    private static void tryTeleportPet(ServerPlayerEntity pet, ServerPlayerEntity owner) {
        ServerWorld world = owner.getServerWorld();
        BlockPos ownerPos = owner.getBlockPos();
        Random random = pet.getRandom();
        for (int i = 0; i < 10; i++) {
            int dx = random.nextBetween(-2, 2);
            int dy = random.nextBetween(-1, 1);
            int dz = random.nextBetween(-2, 2);
            BlockPos targetPos = ownerPos.add(dx, dy, dz);
            if (canTeleportTo(targetPos, world)) {
                pet.teleport(world, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, pet.getYaw(), pet.getPitch());
                pet.setVelocity(Vec3d.ZERO);
                pet.fallDistance = 0.0f;
                world.spawnParticles(ParticleTypes.PORTAL, targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5, 10, 0.2, 0.5, 0.2, 0.1);
                return;
            }
        }
    }

    private static boolean canTeleportTo(BlockPos pos, ServerWorld world) {
        net.minecraft.block.BlockState floor = world.getBlockState(pos.down());
        net.minecraft.block.BlockState feet = world.getBlockState(pos);
        net.minecraft.block.BlockState head = world.getBlockState(pos.up());
        if (floor.getCollisionShape(world, pos.down()).isEmpty()) return false;
        if (!feet.getCollisionShape(world, pos).isEmpty() || !head.getCollisionShape(world, pos.up()).isEmpty()) return false;
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) return false;
        if (floor.getBlock() == net.minecraft.block.Blocks.MAGMA_BLOCK || floor.getBlock() == net.minecraft.block.Blocks.CAMPFIRE) return false;
        return true;
    }
}
