package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType;
import net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms;
import net.onixary.shapeShifterCurseFabric.player_form.utils.RegPlayerFormComponent;

import java.util.List;

/** Shared rules used by both server actions and client presentation. */
public final class BondRules {
    public static final List<String> COMMANDS = List.of("sit", "tp", "nobreak", "absorb", "armor", "inventory", "blocks");

    private BondRules() {}

    public static boolean isAllay(PlayerEntity player) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(player);
        return form != null && form.nowForm.getFormID().getPath().contains("allay");
    }

    public static boolean protectedFromForcedTaming(PlayerEntity player) {
        return isAllay(player) || hasDustProtection(player);
    }

    public static boolean hasDustProtection(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(c -> c.isEquipped(ModItems.DUSTGUARD_AMULET)).orElse(false);
    }

    public static boolean blocksHumanReturn(PlayerEntity player, IForm target) {
        return ModComponents.PLAYER_BOND.get(player).hasOwner() &&
                RegPlayerFormComponent.PLAYER_FORM.get(player).nowForm == RegPlayerForms.FERAL_CAT_SP &&
                (target == RegPlayerForms.ORIGINAL_SHIFTER || target == RegPlayerForms.ORIGINAL_BEFORE_ENABLE);
    }

    public static boolean hasOpenedBook(PlayerEntity player) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(player).nowForm;
        return form != null && !form.getFormID().getPath().equals("original_before_enable");
    }

    public static boolean canOwn(PlayerEntity player) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(player).nowForm;
        return !ModComponents.PLAYER_BOND.get(player).hasOwner() && form != null && (isAllay(player) ||
                (form.getBodyType() != PlayerFormBodyType.FERAL &&
                        (form.getFormTier() < 3 || (form.getFormTier() == 3 && hasClarity(player)))));
    }

    public static boolean hasClarity(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(c -> c.isEquipped(stack -> stack.getItem() instanceof NecklaceOfClarity)).orElse(false);
    }

    public static boolean canBePet(PlayerEntity player) {
        if (!hasOpenedBook(player)) return false;
        var form = RegPlayerFormComponent.PLAYER_FORM.get(player).nowForm;
        if (form == null || isAllay(player) || form.getFormTier() < 3) return false;
        var group = form.getFormGroup();
        if (group == null) return form.getFormTier() >= 4;
        for (int next = form.getFormTier() + 1; next <= 5; next++) if (group.getFormList(form.getFormTier()+1) != null) return false;
        return true;
    }

    public static boolean canObey(PlayerEntity player) {
        var bond = ModComponents.PLAYER_BOND.get(player);
        return bond.hasOwner() && !ForcedTamingService.freeWindow(player) &&
                (!CommandControl.temporary(player) || ForcedTamingService.stage(player) >= 0);
    }

    public static int requiredStage(String command) {
        return switch (command) {
            case "sit", "nobreak" -> 1;
            case "tp", "inventory" -> 2;
            case "armor", "absorb" -> 3;
            case "blocks" -> 4;
            default -> 99;
        };
    }

    public static boolean allows(PlayerEntity pet, String command) {
        if (!COMMANDS.contains(command) || !canObey(pet) || !hasCollar(pet)) return false;
        var bond = ModComponents.PLAYER_BOND.get(pet);
        return !CommandControl.temporary(pet) || ForcedTamingService.canFinalizeForced(pet) ||
                ForcedTamingService.stage(pet) >= requiredStage(command);
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
