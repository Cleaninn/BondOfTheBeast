package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Contract commands mature automatically; ordinary restrictions belong to the owner immediately. */
public final class VoluntaryBondService {
    private VoluntaryBondService() {}

    public static int ticksForTier(int tier) {
        return switch (tier) {
            case 1 -> 36000; // 30 minutes
            case 2 -> 144000; // 2 hours total
            case 3 -> 432000; // 6 hours total
            default -> 0;
        };
    }

    public static int requiredTier(String command) {
        return switch (command) {
            case "sit", "nobreak", "armor" -> 0;
            case "tp", "prot" -> 1;
            case "aura", "vampiric" -> 2;
            case "absorb" -> 3;
            default -> 99;
        };
    }

    public static int remainingSeconds(int tier, int ticks) {
        return tier >= 3 ? 0 : Math.max(0, (ticksForTier(tier + 1) - ticks + 19) / 20);
    }

    public static void tick(ServerPlayerEntity pet) {
        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (!bond.hasOwner() || bond.getForcedBond().forced || !pet.isAlive() || pet.isSpectator() || pet.isCreative() ||
                !BondRules.hasCollar(pet)) return;
        var state = bond.getVoluntaryBond();
        state.activeTicks = Math.min(ticksForTier(3), Math.max(state.activeTicks, ticksForTier(state.tier)) + 1);
        int previous = state.tier;
        while (state.tier < 3 && state.activeTicks >= ticksForTier(state.tier + 1)) state.tier++;
        if (previous != state.tier) {
            pet.sendMessage(Text.translatable("text.bondofthebeast.bond_matured", Text.translatable("gui.bondofthebeast.trust." + state.tier)), false);
            ServerPlayerEntity owner = BondRules.owner(pet);
            if (owner != null) owner.sendMessage(Text.translatable("text.bondofthebeast.bond_matured", Text.translatable("gui.bondofthebeast.trust." + state.tier)), false);
            if (state.tier == 3) {
                BondOfTheBeast.grantAdvancement(pet, "pet_story/bond_level");
                if (owner != null) BondOfTheBeast.grantAdvancement(owner, "owner_story/bond_level");
            }
            ModComponents.PLAYER_BOND.sync(pet);
        } else if (state.tier < 3 && pet.age % 20 == 0) ModComponents.PLAYER_BOND.sync(pet);
    }
}
