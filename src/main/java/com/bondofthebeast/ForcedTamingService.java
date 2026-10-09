package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.instinct.InstinctManager;
import net.onixary.shapeShifterCurseFabric.items.RegCustomItem;
import net.onixary.shapeShifterCurseFabric.status_effects.attachment.EffectManager;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public final class ForcedTamingService {
    public static final int WARNING_TICKS = 200;
    public static final int ESCAPE_TICKS = 2400;
    public static final int RESISTANCE_COOLDOWN_TICKS = 6000;
    public static final int COLLARING_TICKS = 40;
    public static final int STUN_TICKS = 50;
    public static final double STUN_DODGE_SPEED = 0.035;
    public static final float INFUSED_RATE_PER_TICK = 0.15f / 20;
    public static final float BED_RATE_PER_TICK = 0.10f / 20;
    private static final Map<UUID, CollaringAttempt> COLLARING = new HashMap<>();
    private ForcedTamingService() {}

    private static final class CollaringAttempt {
        final UUID target;
        final Hand hand;
        final ItemStack collar;
        int ticks;

        CollaringAttempt(UUID target, Hand hand, ItemStack collar) {
            this.target = target;
            this.hand = hand;
            this.collar = collar;
        }
    }

    public static boolean stunned(PlayerEntity player) {
        var slow = player.getStatusEffect(StatusEffects.SLOWNESS);
        return slow != null && slow.getAmplifier() == 99;
    }

    public static int resistanceCooldownSeconds(PlayerEntity player) {
        return (ModComponents.PLAYER_BOND.get(player).getForcedBond().resistanceCooldownTicks + 19) / 20;
    }

    public static boolean sleepingOnPetBed(PlayerEntity player) {
        return player.isSleeping() && player.getSleepingPosition().map(pos ->
                player.getWorld().getBlockState(pos).getBlock() instanceof com.bondofthebeast.block.PetBedBlock).orElse(false);
    }

    public static int stage(PlayerEntity pet) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(pet).getCurrentForm();
        return form == null ? -2 : form.getIndex();
    }

    public static boolean infused(PlayerEntity pet) {
        return TrinketsApi.getTrinketComponent(pet).map(c -> c.isEquipped(
                s -> s.getItem() instanceof com.bondofthebeast.item.InfusedCollarItem)).orElse(false);
    }

    public static boolean freeWindow(PlayerEntity pet) {
        return ModComponents.PLAYER_BOND.get(pet).getForcedBond().escapeTicks > 0 && stage(pet) < 3;
    }

    public static boolean armorLocked(PlayerEntity pet) {
        return BondRules.allows(pet, "armor") &&
                ModComponents.PLAYER_BOND.get(pet).getForcedBond().armorLocked;
    }

    public static ActionResult start(ItemStack stack, PlayerEntity user, ServerPlayerEntity pet, Hand hand) {
        if (pet == user || !BondRules.canOwn(user) || pet.isCreative() || pet.isSpectator()) return ActionResult.FAIL;
        if (!stunned(pet)) return ActionResult.PASS;
        if (!canCollar(user, pet)) return ActionResult.FAIL;
        var attempt = COLLARING.get(user.getUuid());
        if (attempt != null && attempt.target.equals(pet.getUuid()) && attempt.collar == stack && user.isUsingItem())
            return ActionResult.SUCCESS;
        COLLARING.put(user.getUuid(), new CollaringAttempt(pet.getUuid(), hand, stack));
        user.setCurrentHand(hand);
        user.sendMessage(Text.translatable("text.bondofthebeast.collar_hold_start"), true);
        pet.sendMessage(Text.translatable("text.bondofthebeast.collar_dodge_hint"), true);
        return ActionResult.SUCCESS;
    }

    private static boolean canCollar(PlayerEntity user, ServerPlayerEntity pet) {
        if (!user.isAlive() || !pet.isAlive() || user.isSpectator() || pet == user || !BondRules.canOwn(user) ||
                pet.isCreative() || pet.isSpectator() || !stunned(pet) || freeWindow(pet) ||
                user.getWorld() != pet.getWorld() || user.squaredDistanceTo(pet) > 16 || !user.canSee(pet)) return false;
        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (bond.hasOwner() || stage(pet) > 3) return false;
        var component = TrinketsApi.getTrinketComponent(pet);
        if (component.isEmpty()) return false;
        var group = component.get().getInventory().get("chest");
        if (group == null || group.get("necklace") == null || group.get("necklace").size() == 0) return false;
        Vec3d eyes = user.getEyePos();
        return pet.getBoundingBox().expand(0.1).raycast(eyes,
                eyes.add(user.getRotationVec(1).multiply(4))).isPresent();
    }

    public static void cancelCollaring(PlayerEntity user) {
        COLLARING.remove(user.getUuid());
    }

    public static void tickCollaring(ServerPlayerEntity user) {
        var attempt = COLLARING.get(user.getUuid());
        if (attempt == null) return;
        var pet = user.getServer().getPlayerManager().getPlayer(attempt.target);
        if (pet == null || !user.isUsingItem() || user.getActiveHand() != attempt.hand ||
                user.getStackInHand(attempt.hand) != attempt.collar || attempt.collar.isEmpty() || !canCollar(user, pet)) {
            cancelCollaring(user);
            user.stopUsingItem();
            user.sendMessage(Text.translatable("text.bondofthebeast.collar_hold_interrupted"), true);
            return;
        }
        if (++attempt.ticks < COLLARING_TICKS) return;
        cancelCollaring(user);
        equipForced(attempt.collar, user, pet);
        user.stopUsingItem();
    }

    private static void equipForced(ItemStack stack, ServerPlayerEntity user, ServerPlayerEntity pet) {
        var inventory = TrinketsApi.getTrinketComponent(pet).orElseThrow().getInventory().get("chest").get("necklace");
        ItemStack displaced = inventory.getStack(0).copy();
        ItemStack collar = stack.copy();
        collar.setCount(1);
        collar.getOrCreateNbt().putString("OwnerName", user.getName().getString());
        inventory.setStack(0, collar);
        if (!displaced.isEmpty()) pet.getInventory().offerOrDrop(displaced);
        var bond = ModComponents.PLAYER_BOND.get(pet);
        bond.setOwner(user.getUuidAsString(), user.getName().getString());
        var forced = bond.getForcedBond();
        forced.forced = true;
        forced.token = UUID.randomUUID().toString();
        forced.lastStage = stage(pet);
        bond.setTamingState(stage(pet) >= 3 ? 2 : 1);
        ModComponents.PLAYER_BOND.get(user).addPetToRegistry(pet.getUuidAsString(), pet.getGameProfile().getName());
        if (!user.isCreative()) stack.decrement(1);
        pet.sendMessage(Text.translatable("text.bondofthebeast.forced_start"), false);
        user.sendMessage(Text.translatable("text.bondofthebeast.taming_started"), true);
        ModComponents.PLAYER_BOND.sync(pet);
    }

    public static boolean escapeApple(ServerPlayerEntity pet, ItemStack food) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (!bond.hasOwner() || !bond.getForcedBond().forced || stage(pet) >= 3) return false;
        return food.isOf(Items.ENCHANTED_GOLDEN_APPLE) || (stage(pet) < 2 && food.isOf(Items.GOLDEN_APPLE));
    }

    public static void determination(ServerPlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        var forced = bond.getForcedBond();
        if (!bond.hasOwner() || !forced.forced || stage(pet) >= 3 || forced.escapeTicks > 0) return;
        if (forced.resistanceCooldownTicks > 0) {
            pet.sendMessage(Text.translatable("gui.bondofthebeast.resistance_cooldown", resistanceCooldownSeconds(pet)), true);
            return;
        }
        forced.escapeTicks = ESCAPE_TICKS;
        forced.resistanceCooldownTicks = RESISTANCE_COOLDOWN_TICKS;
        forced.escapeCharge = 0;
        forced.finalWarningTicks = -1;
        var instinct = net.onixary.shapeShifterCurseFabric.player_form.instinct.RegPlayerInstinctComponent.PLAYER_INSTINCT_COMP.get(pet);
        instinct.instinctValue = Math.min(95, instinct.instinctValue);
        net.onixary.shapeShifterCurseFabric.player_form.instinct.RegPlayerInstinctComponent.PLAYER_INSTINCT_COMP.sync(pet);
        bond.setSitting(false);
        bond.setTeleportEnabled(false);
        bond.setNoBreakMode(false);
        bond.setNoInteractMode(false);
        bond.setPacifistMode(false);
        forced.armorLocked = false;
        LeashManager.detach(pet);
        BondService.clearBed(pet);
        BondService.releaseAbsorption(pet);
        var slow = pet.getStatusEffect(StatusEffects.SLOWNESS);
        if (slow != null && slow.getAmplifier() == 99) pet.removeStatusEffect(StatusEffects.SLOWNESS);
        pet.removeStatusEffect(StatusEffects.BLINDNESS);
        pet.sendMessage(Text.translatable("text.bondofthebeast.escape_window"), false);
        ModComponents.PLAYER_BOND.sync(pet);
    }

    public static boolean isCatalyst(ItemStack stack) {
        return stack.isOf(RegCustomItem.CATALYST) || stack.isOf(RegCustomItem.POWERFUL_CATALYST);
    }

    public static ActionResult feedCatalyst(PlayerEntity owner, ServerPlayerEntity pet, ItemStack stack) {
        if (!isCatalyst(stack)) return ActionResult.PASS;
        if (!BondRules.owns(owner, pet) || !BondRules.canOwn(owner) || !BondRules.hasCollar(pet) || freeWindow(pet)) return ActionResult.FAIL;
        int stage = stage(pet);
        if (stage < -1 || stage >= 3) return ActionResult.FAIL;
        var state = ModComponents.PLAYER_BOND.get(pet).getForcedBond();
        int bit = 1 << (stage + 2);
        if ((state.catalystStages & bit) != 0) {
            owner.sendMessage(Text.translatable("text.bondofthebeast.catalyst_stage_used"), true);
            return ActionResult.FAIL;
        }
        if (stage < 0 && !EffectManager.hasTransformativeEffect(pet)) return ActionResult.FAIL;
        state.catalystStages |= bit;
        if (stage < 0) {
            state.warningTicks = WARNING_TICKS;
        } else {
            InstinctManager.applyImmediateEffect(pet, "botb_owner_cookie", 25);
        }
        if (!owner.isCreative()) stack.decrement(1);
        pet.getHungerManager().add(2, 0.1f);
        owner.sendMessage(Text.translatable("text.bondofthebeast.catalyst_given"), true);
        ModComponents.PLAYER_BOND.sync(pet);
        return ActionResult.SUCCESS;
    }

    private static void issueContract(ServerPlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        var state = bond.getForcedBond();
        if (state.contractSigned) return;
        if (state.token.isEmpty()) state.token = UUID.randomUUID().toString();
        ItemStack contract = new ItemStack(ModItems.FIDELITY_CONTRACT);
        var tag = contract.getOrCreateNbt();
        tag.putString("PetUUID", pet.getUuidAsString());
        tag.putString("PetName", pet.getGameProfile().getName());
        tag.putString("OwnerUUID", bond.getOwnerUUID());
        tag.putString("OwnerName", bond.getOwnerName());
        tag.putString("ForcedBondToken", state.token);
        tag.putInt("CustomModelData", 1);
        state.contractSigned = true;
        var owner = BondRules.owner(pet);
        if (owner != null) owner.getInventory().offerOrDrop(contract); else pet.dropItem(contract, false);
        pet.sendMessage(Text.translatable("text.bondofthebeast.forced_contract"), false);
        if (owner != null) owner.sendMessage(Text.translatable("text.bondofthebeast.forced_contract_owner"), false);
        BondOfTheBeast.grantAdvancement(pet, "pet_story/root");
    }

    public static void tick(ServerPlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        var state = bond.getForcedBond();
        if (!bond.hasOwner()) return;
        int stage = stage(pet);
        if (state.escapeTicks > 0) state.escapeTicks--;
        if (state.resistanceCooldownTicks > 0) state.resistanceCooldownTicks--;
        if (state.forced && stage < 3 && state.escapeTicks == 0 && state.resistanceCooldownTicks == 0) {
            state.escapeCharge = pet.isSneaking() ? state.escapeCharge + 1 : 0;
            if (state.escapeCharge >= 60) determination(pet);
        } else state.escapeCharge = 0;
        if (state.controlTicks > 0 && --state.controlTicks == 0) bond.setSitting(false);
        if (bond.isAbsorbed() && ++state.absorbedTicks >= 1200) BondService.releaseAbsorption(pet);
        if (!bond.isAbsorbed()) state.absorbedTicks = 0;
        if (!state.forced) return;
        if (bond.isProtectionMode()) bond.setProtectionMode(false);
        if (bond.isAuraEnabled()) bond.setAuraEnabled(false);
        if (bond.isVampiricMode()) bond.setVampiricMode(false);
        if (bond.isTeleportEnabled()) bond.setTeleportEnabled(false);
        if (bond.isAbsorbed()) BondService.releaseAbsorption(pet);
        if (!pet.isAlive()) return;
        if (!BondRules.hasCollar(pet) && stage < 3) { BondService.release(pet); return; }
        if (state.lastStage != stage) {
            state.lastStage = stage;
            bond.setTamingState(stage >= 3 ? 2 : 1);
            state.warningTicks = -1;
            state.finalWarningTicks = -1;
            pet.sendMessage(Text.translatable("text.bondofthebeast.stage_changed", stage), false);
        }
        if (stage >= 3) issueContract(pet);
        if (stage == 2 && state.finalWarningTicks >= 0 && !freeWindow(pet)) {
            if (state.finalWarningTicks > 0) {
                state.finalWarningTicks--;
                if (state.finalWarningTicks % 20 == 0) {
                    pet.sendMessage(Text.translatable("text.bondofthebeast.final_countdown", (state.finalWarningTicks + 19) / 20), true);
                    pet.getServerWorld().spawnParticles(ParticleTypes.WITCH, pet.getX(), pet.getY() + 1, pet.getZ(), 8, 0.3, 0.5, 0.3, 0);
                }
            } else {
                var form = RegPlayerFormComponent.PLAYER_FORM.get(pet).getCurrentForm();
                state.finalWarningTicks = -1;
                if (form.getGroup() != null && form.getGroup().hasForm(3)) {
                    net.onixary.shapeShifterCurseFabric.player_form.instinct.RegPlayerInstinctComponent.PLAYER_INSTINCT_COMP.get(pet).instinctValue = 0;
                    net.onixary.shapeShifterCurseFabric.player_form.transform.TransformManager.handleDirectTransform(pet, form.getGroup().getForm(3), false);
                }
            }
        }
        if (stage < 0 && EffectManager.hasTransformativeEffect(pet) && !freeWindow(pet) &&
                (infused(pet) || state.warningTicks >= 0)) {
            if (state.warningTicks < 0) state.warningTicks = WARNING_TICKS;
            if (state.warningTicks > 0) {
                state.warningTicks--;
                if (state.warningTicks % 20 == 0) {
                    pet.getServerWorld().spawnParticles(ParticleTypes.WITCH, pet.getX(), pet.getY() + 1, pet.getZ(), 6, 0.3, 0.5, 0.3, 0);
                    pet.sendMessage(Text.translatable("text.bondofthebeast.curse_countdown", (state.warningTicks + 19) / 20), true);
                }
            } else {
                EffectManager.ActiveTransformativeEffect(pet);
                state.warningTicks = -1;
            }
        } else if (!EffectManager.hasTransformativeEffect(pet)) state.warningTicks = -1;
        if (pet.age % 20 == 0) ModComponents.PLAYER_BOND.sync(pet);
    }
}
