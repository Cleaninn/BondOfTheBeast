package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** Temporary orders expire independently; a signed contract makes them persistent. */
public final class CommandControl {
    private CommandControl() {}
    public static boolean temporary(PlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        return bond.hasOwner() && bond.getForcedBond().forced && !bond.getForcedBond().contractSigned;
    }
    public static int durationTicks(PlayerEntity pet) {
        return temporary(pet) ? BondConfig.durationTicks(ForcedTamingService.canFinalizeForced(pet) ? 3 : ForcedTamingService.stage(pet)) : 0;
    }
    public static boolean active(PlayerEntity pet, String command) {
        return !temporary(pet) || ModComponents.PLAYER_BOND.get(pet).getForcedBond().commandTicks.getOrDefault(command, 0) > 0;
    }
    public static boolean blockListsActive(PlayerEntity pet) { return active(pet, "blocks"); }
    public static void start(ServerPlayerEntity pet, String command) {
        var state = ModComponents.PLAYER_BOND.get(pet).getForcedBond();
        if (temporary(pet) && enabled(pet, command)) state.commandTicks.put(command, durationTicks(pet));
        else state.commandTicks.remove(command);
        ModComponents.PLAYER_BOND.sync(pet);
    }
    public static boolean enabled(PlayerEntity pet, String command) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        return switch (command) {
            case "sit" -> b.isSitting();
            case "tp" -> b.isTeleportEnabled();
            case "absorb" -> b.isAbsorbed();
            case "inventory" -> b.isItemForwarding();
            case "nobreak" -> b.isNoBreakMode();
            case "interact" -> b.isNoInteractMode();
            case "pacifist" -> b.isPacifistMode();
            case "armor_lock" -> b.getForcedBond().armorLocked;
            case "blocks" -> !b.getBlacklistedBlocks().isEmpty() || !b.getWhitelistedBlocks().isEmpty();
            case "armor_access" -> true;
            default -> false;
        };
    }
    private static void stop(ServerPlayerEntity pet, String command) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        switch (command) {
            case "sit" -> b.setSitting(false);
            case "tp" -> b.setTeleportEnabled(false);
            case "absorb" -> BondService.releaseAbsorption(pet);
            case "inventory" -> b.setItemForwarding(false);
            case "nobreak" -> b.setNoBreakMode(false);
            case "interact" -> b.setNoInteractMode(false);
            case "pacifist" -> b.setPacifistMode(false);
            case "armor_lock" -> b.getForcedBond().armorLocked = false;
        }
    }
    public static void tick(ServerPlayerEntity pet) {
        var state = ModComponents.PLAYER_BOND.get(pet).getForcedBond();
        if (!temporary(pet)) { state.commandTicks.clear(); return; }
        // Migrate active orders from older saves to finite leases.
        for (String command : java.util.List.of("sit", "tp", "absorb", "inventory", "nobreak", "interact", "pacifist", "armor_lock"))
            if (enabled(pet, command)) state.commandTicks.putIfAbsent(command, durationTicks(pet));
        boolean expired = false;
        for (String command : java.util.List.copyOf(state.commandTicks.keySet())) {
            int remaining = Math.min(state.commandTicks.get(command), durationTicks(pet)) - 1;
            if (remaining <= 0) { state.commandTicks.remove(command); stop(pet, command); expired = true; }
            else state.commandTicks.put(command, remaining);
        }
        if (expired) ModComponents.PLAYER_BOND.sync(pet);
    }
    public static void stageChanged(ServerPlayerEntity pet, int previousStage) {
        if (!temporary(pet)) return;
        int difference = durationTicks(pet) - BondConfig.durationTicks(previousStage);
        if (difference > 0) ModComponents.PLAYER_BOND.get(pet).getForcedBond().commandTicks.replaceAll((key, ticks) -> ticks + difference);
    }
}
