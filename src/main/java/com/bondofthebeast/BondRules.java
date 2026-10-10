package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;

import java.util.Map;
import java.util.Objects;

/** Shared rules used by both server actions and client presentation. */
public final class BondRules {
    public static final Map<String, String> SKILL_PARENTS = Map.of(
            "sit", "", "tp", "sit", "prot", "sit", "nobreak", "sit",
            "absorb", "tp", "vampiric", "prot", "aura", "nobreak");

    private BondRules() {}

    public static boolean canOwn(PlayerEntity player) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm();
        return !ModComponents.PLAYER_BOND.get(player).hasOwner() && form != null && (form.FormID.getPath().contains("allay") ||
                (form.getBodyType() != PlayerFormBodyType.FERAL &&
                        (form.getIndex() < 2 || (form.getIndex() == 2 && hasClarity(player)))));
    }

    public static boolean hasClarity(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(c -> c.isEquipped(stack -> stack.getItem() instanceof NecklaceOfClarity)).orElse(false);
    }

    public static boolean canBePet(PlayerEntity player) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm();
        if (form == null || form.FormID.getPath().contains("allay") || form.getIndex() < 2) return false;
        var group = form.getGroup();
        if (group == null) return form.getIndex() >= 3;
        for (int next = form.getIndex() + 1; next <= 5; next++) if (group.hasForm(next)) return false;
        return true;
    }

    public static boolean canObey(PlayerEntity player) {
        var bond = ModComponents.PLAYER_BOND.get(player);
        return bond.hasOwner() && !ForcedTamingService.freeWindow(player) &&
                (!bond.getForcedBond().forced || ForcedTamingService.stage(player) >= 1);
    }

    public static int requiredStage(String command) {
        return switch (command) {
            case "sit", "nobreak" -> 2;
            case "armor" -> 3;
            default -> 99;
        };
    }

    public static boolean allows(PlayerEntity pet, String command) {
        if (!canObey(pet) || !hasCollar(pet)) return false;
        var bond = ModComponents.PLAYER_BOND.get(pet);
        return bond.getForcedBond().forced ? ForcedTamingService.stage(pet) >= requiredStage(command) :
                canBePet(pet) && bond.getVoluntaryBond().tier >= VoluntaryBondService.requiredTier(command);
    }

    public static boolean hasCollar(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(c -> c.isEquipped(stack -> stack.getItem() instanceof CollarItem)).orElse(false);
    }

    public static boolean owns(PlayerEntity owner, PlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        return owner != pet && bond.hasOwner() && bond.getOwnerUUID().equals(owner.getUuidAsString());
    }

    public static ServerPlayerEntity owner(ServerPlayerEntity pet) {
        try {
            return pet.getServer().getPlayerManager().getPlayer(
                    java.util.UUID.fromString(ModComponents.PLAYER_BOND.get(pet).getOwnerUUID()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
