package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.onixary.shapeShifterCurseFabric.player_form.utils.PlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.utils.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.utils.InstinctUtils;
import net.onixary.shapeShifterCurseFabric.items.RegCustomItem;
import net.onixary.shapeShifterCurseFabric.player_form.utils.TransformManager;
import net.onixary.shapeShifterCurseFabric.status_effects.attachment.EffectManager;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
        int missedAimTicks;

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
        var form = RegPlayerFormComponent.PLAYER_FORM.get(pet).nowForm;
        if (form != null && form.getFormID().getPath().contains("allay")) return 0;
        if (form.getFormID().getPath().contains("Feral")) return 4;
        return form == null ? -2 : form.getFormTier();
    }

    public static boolean infused(PlayerEntity pet) {
        return TrinketsApi.getTrinketComponent(pet).map(c -> c.isEquipped(
                s -> s.getItem() instanceof com.bondofthebeast.item.InfusedCollarItem)).orElse(false);
    }

    public static boolean freeWindow(PlayerEntity pet) {
        var state = ModComponents.PLAYER_BOND.get(pet).getForcedBond();
        return state.escapeTicks > 0 && !state.contractSigned;
    }

    public static boolean armorLocked(PlayerEntity pet) {
        return BondRules.allows(pet, "armor") &&
                ModComponents.PLAYER_BOND.get(pet).getForcedBond().armorLocked;
    }

    public static ActionResult start(ItemStack stack, PlayerEntity user, ServerPlayerEntity pet, Hand hand) {
        if (pet == user || !BondRules.canOwn(user) || !BondRules.hasOpenedBook(pet) || BondRules.protectedFromForcedTaming(pet) || pet.isCreative() || pet.isSpectator()) return ActionResult.FAIL;
        if (!stunned(pet)) return ActionResult.PASS;
        if (!canCollar(user, pet) || !aimsAt(user, pet)) return ActionResult.FAIL;
        var attempt = COLLARING.get(user.getUuid());
        if (attempt != null && attempt.target.equals(pet.getUuid()) && ItemStack.areEqual(attempt.collar, stack) && user.isUsingItem())
            return ActionResult.SUCCESS;
        COLLARING.put(user.getUuid(), new CollaringAttempt(pet.getUuid(), hand, stack));
        user.setCurrentHand(hand);
        sendProgress(user, pet, 0);
        return ActionResult.SUCCESS;
    }

    private static boolean canCollar(PlayerEntity user, ServerPlayerEntity pet) {
        if (!user.isAlive() || !pet.isAlive() || user.isSpectator() || pet == user || !BondRules.canOwn(user) ||
                !BondRules.hasOpenedBook(pet) || BondRules.protectedFromForcedTaming(pet) || pet.isCreative() || pet.isSpectator() || freeWindow(pet) ||
                user.getWorld() != pet.getWorld() || user.squaredDistanceTo(pet) > 16 || !user.canSee(pet)) return false;
        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (bond.hasOwner()) return false;
        var component = TrinketsApi.getTrinketComponent(pet);
        if (component.isEmpty()) return false;
        var group = component.get().getInventory().get("chest");
        if (group == null || group.get("necklace") == null || group.get("necklace").size() == 0) return false;
        return true;
    }

    public static boolean aimsAt(PlayerEntity user, PlayerEntity pet) {
        Vec3d eyes = user.getEyePos();
        return pet.getBoundingBox().expand(0.25).raycast(eyes,
                eyes.add(user.getRotationVec(1).multiply(4))).isPresent();
    }

    public static boolean isCollaring(PlayerEntity user) {
        return COLLARING.containsKey(user.getUuid());
    }

    private static void sendProgress(PlayerEntity user, ServerPlayerEntity pet, int ticks) {
        for (var player : new ServerPlayerEntity[]{user instanceof ServerPlayerEntity holder ? holder : null, pet}) {
            if (player == null || !net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, ModPackets.COLLARING_PROGRESS_S2C)) continue;
            var buffer = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            buffer.writeInt(ticks);
            buffer.writeBoolean(player == pet);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, ModPackets.COLLARING_PROGRESS_S2C, buffer);
        }
    }

    public static void cancelCollaring(PlayerEntity user) {
        var attempt = COLLARING.remove(user.getUuid());
        if (attempt != null && user instanceof ServerPlayerEntity serverUser)
            sendProgress(user, serverUser.getServer().getPlayerManager().getPlayer(attempt.target), -1);
    }

    public static void tickCollaring(ServerPlayerEntity user) {
        var attempt = COLLARING.get(user.getUuid());
        if (attempt == null) return;
        var pet = user.getServer().getPlayerManager().getPlayer(attempt.target);
        if (pet == null || !user.isUsingItem() || user.getActiveHand() != attempt.hand ||
                !ItemStack.areEqual(user.getStackInHand(attempt.hand), attempt.collar) || attempt.collar.isEmpty() || !canCollar(user, pet)) {
            cancelCollaring(user);
            user.stopUsingItem();
            return;
        }
        if (!aimsAt(user, pet)) {
            if (++attempt.missedAimTicks > 4) { cancelCollaring(user); user.stopUsingItem(); }
            return;
        }
        attempt.missedAimTicks = 0;
        if (attempt.ticks % 2 == 0) sendProgress(user, pet, attempt.ticks);
        if (++attempt.ticks < COLLARING_TICKS) return;
        cancelCollaring(user);
        equipAfterHold(user.getStackInHand(attempt.hand), user, pet);
        user.stopUsingItem();
    }

    private static void equipAfterHold(ItemStack stack, ServerPlayerEntity user, ServerPlayerEntity pet) {
        if (!CollarItem.equipToPet(pet, stack, user.getName().getString(), true)) return;
        CollarItem.sealEquipped(pet);
        if (!user.isCreative()) stack.decrement(1);
        var bond = ModComponents.PLAYER_BOND.get(pet);
        bond.setOwner(user.getUuidAsString(), user.getName().getString());
        var forced = bond.getForcedBond();
        forced.forced = true;
        forced.token = UUID.randomUUID().toString();
        forced.lastStage = stage(pet);
        bond.setTamingState(1);
        ModComponents.PLAYER_BOND.get(user).addPetToRegistry(pet.getUuidAsString(), pet.getGameProfile().getName());
        ModComponents.PLAYER_BOND.sync(pet);
    }

    public static boolean canFinalizeForced(PlayerEntity pet) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(pet).nowForm;
        return BondRules.canBePet(pet) && form != null &&
                form.getBodyType() == net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType.FERAL;
    }

    public static ActionResult startFromContract(ItemStack contract, ServerPlayerEntity user, ServerPlayerEntity pet) {
        if (!user.isAlive() || !pet.isAlive() || user.isSpectator() || pet == user || !BondRules.canOwn(user) ||
                !BondRules.hasOpenedBook(pet) || BondRules.isAllay(pet) || pet.isCreative() || pet.isSpectator() || freeWindow(pet) || !BondRules.hasCollar(pet) ||
                user.getWorld() != pet.getWorld() || user.squaredDistanceTo(pet) > 16 || !user.canSee(pet)) return ActionResult.FAIL;
        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (!BondRules.owns(user, pet) || !bond.getForcedBond().forced || !canFinalizeForced(pet)) return ActionResult.FAIL;
        var forced = bond.getForcedBond();
        if (forced.contractSigned || forced.contractPending) return ActionResult.FAIL;
        if (user.isCreative() && user.getInventory().getEmptySlot() < 0) return ActionResult.FAIL;
        if (!user.isCreative()) contract.decrement(1);
        forced.contractPending = true;
        ModComponents.PLAYER_BOND.sync(pet);
        // Defer issuance: the vanilla interaction can clear the newly emptied hand afterwards.
        return ActionResult.SUCCESS;
    }

    public static boolean escapeApple(ServerPlayerEntity pet, ItemStack food) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (bond.hasOwner() && (!bond.getForcedBond().forced || bond.getForcedBond().contractSigned)) return false;
        if (!bond.getForcedBond().forced && !CollarItem.hasForcedCollar(pet)) return false;
        return food.isOf(Items.ENCHANTED_GOLDEN_APPLE) || (stage(pet) < 2 && food.isOf(Items.GOLDEN_APPLE));
    }

    public static void determination(ServerPlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        var forced = bond.getForcedBond();
        if (!bond.hasOwner() || !forced.forced || forced.contractSigned || forced.escapeTicks > 0) return;
        if (forced.resistanceCooldownTicks > 0) {
            pet.sendMessage(Text.translatable("gui.bondofthebeast.resistance_cooldown", resistanceCooldownSeconds(pet)), true);
            return;
        }
        forced.escapeTicks = ESCAPE_TICKS;
        forced.resistanceCooldownTicks = RESISTANCE_COOLDOWN_TICKS;
        forced.commandTicks.clear();
        forced.finalWarningTicks = -1;
        var instinct = PlayerFormComponent.COMPONENT.get(pet);
        instinct.instinctValue = Math.min(95, instinct.instinctValue);
        bond.setSitting(false);
        bond.setTeleportEnabled(false);
        bond.setItemForwarding(false);
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
        ModComponents.PLAYER_BOND.sync(pet);
    }

    public static boolean isCatalyst(ItemStack stack) {
        return stack.isOf(RegCustomItem.CATALYST) || stack.isOf(RegCustomItem.POWERFUL_CATALYST) || stack.getItem() instanceof com.bondofthebeast.item.CatalystCookieItem;
    }

    public static ActionResult feedCatalyst(PlayerEntity owner, ServerPlayerEntity pet, ItemStack stack) {
        if (!isCatalyst(stack)) return ActionResult.PASS;
        boolean cookieItem = stack.getItem() instanceof com.bondofthebeast.item.CatalystCookieItem;
        if (!BondRules.hasOpenedBook(pet) || !BondRules.owns(owner, pet) || !BondRules.canOwn(owner) || !BondRules.hasCollar(pet) || freeWindow(pet)) return ActionResult.FAIL;
        int stage = stage(pet);
        if (stage < -1 || stage >= 4) return ActionResult.FAIL;
        var state = ModComponents.PLAYER_BOND.get(pet).getForcedBond();
        if (stack.getItem() instanceof com.bondofthebeast.item.CatalystCookieItem cookie && cookie.isEcho()) {
            var form = RegPlayerFormComponent.PLAYER_FORM.get(pet).nowForm;
            if (stage != 3 || form.getFormGroup() == null || form.getFormGroup().getFormList(4) == null) {
                return ActionResult.FAIL;
            }
            long now = pet.getServer().getOverworld().getTime();
            long remaining = state.thirdStageReachedAt < 0 ? 12000 : Math.max(0, 12000 - (now - state.thirdStageReachedAt));
            if (remaining > 0) {
                return ActionResult.FAIL;
            }
            net.onixary.shapeShifterCurseFabric.player_form.utils.TransformManager.startTransform(pet, form.getFormGroup().getFormList(3).get(0), new Consumer<TransformManager.PlayerTransformData>() {
                @Override
                public void accept(TransformManager.PlayerTransformData playerTransformData) {

                }
            });
            PetFeedingEffects.play(pet, stack);
            if (!owner.isCreative()) stack.decrement(1);
            pet.getHungerManager().add(2, 0.1f);
            return ActionResult.SUCCESS;
        }
        int bit = 1 << (stage + 2);
        if ((state.catalystStages & bit) != 0) {
            if (!cookieItem)
                owner.sendMessage(Text.translatable("text.bondofthebeast.catalyst_stage_used"), true);
            return ActionResult.FAIL;
        }
        if (stage < 0 && !EffectManager.hasTransformativeEffect(pet)) return ActionResult.FAIL;
        state.catalystStages |= bit;
        if (stage < 0) {
            state.warningTicks = WARNING_TICKS;
            state.catalystWarningFromCookie = true;
        } else {
            if (cookieItem)
                InstinctUtils.addInstinctEffect(pet, BondOfTheBeast.identifier("FORM_USE_CATALYST"), 0.975f, 40, false);
            //else InstinctUtils.addInstinctEffect(pet, BondOfTheBeast.identifier("botb_owner_cookie"), true);
        }
        if (cookieItem) PetFeedingEffects.play(pet, stack);
        if (!owner.isCreative()) stack.decrement(1);
        pet.getHungerManager().add(2, 0.1f);
        if (!cookieItem)
            owner.sendMessage(Text.translatable("text.bondofthebeast.catalyst_given"), true);
        ModComponents.PLAYER_BOND.sync(pet);
        return ActionResult.SUCCESS;
    }

    private static void issueContract(ServerPlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        var state = bond.getForcedBond();
        if (state.contractSigned) return;
        var owner = BondRules.owner(pet);
        if (owner != null && owner.isCreative() && owner.getInventory().getEmptySlot() < 0) return;
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
        state.contractPending = false;
        bond.setTamingState(2);
        if (owner != null) owner.getInventory().offerOrDrop(contract); else pet.dropItem(contract, false);
        pet.sendMessage(Text.translatable("text.bondofthebeast.forced_contract"), false);
        if (owner != null) owner.sendMessage(Text.translatable("text.bondofthebeast.forced_contract_owner"), false);
        BondOfTheBeast.grantAdvancement(pet, "pet_story/root");
    }

    public static void tick(ServerPlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        var state = bond.getForcedBond();
        int stage = stage(pet);
        long now = pet.getServer().getOverworld().getTime();
        if (stage == 2) {
            if (state.thirdStageReachedAt < 0 || state.thirdStageReachedAt > now) {
                state.thirdStageReachedAt = now;
                ModComponents.PLAYER_BOND.sync(pet);
            }
        } else state.thirdStageReachedAt = -1;
        if (!bond.hasOwner()) return;
        if (state.escapeTicks > 0) state.escapeTicks--;
        if (state.resistanceCooldownTicks > 0) state.resistanceCooldownTicks--;
        CommandControl.tick(pet);
        if (!state.forced) return;
        if (!pet.isAlive()) return;
        if (!BondRules.hasCollar(pet) && !state.contractSigned) { BondService.release(pet); return; }
        if (state.lastStage != stage) {
            CommandControl.stageChanged(pet, state.lastStage);
            int previousStage = state.lastStage;
            state.lastStage = stage;
            bond.setTamingState(state.contractSigned ? 2 : 1);
            state.warningTicks = -1;
            state.catalystWarningFromCookie = false;
            state.finalWarningTicks = -1;
            if (stage > previousStage && stage >= 0 && !state.contractSigned) ModPackets.notifyStage(pet);
        }
        if (state.contractPending) issueContract(pet);
        if (pet.age % 20 == 0) ModComponents.PLAYER_BOND.sync(pet);
    }
}
