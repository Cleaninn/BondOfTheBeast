package com.bondofthebeast.additional_power;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.component.ModComponents;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.calio.data.SerializableData;
import com.bondofthebeast.BondRules;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class IsAllowedAnAction {

    public static boolean condition(SerializableData.Instance data, Entity entity) {
        if (!(entity instanceof PlayerEntity player)) return false;
        var bond = ModComponents.PLAYER_BOND.get(player);
        String command = data.getString("command");
        String skill = command.equals("pacifism") ? "nobreak" : command;
        if (!BondRules.allows(player, skill)) return false;
        return switch (command) {
            case "pacifism" -> bond.isPacifistMode();
            case "sit" -> bond.isSitting();
            case "tp" -> bond.isTeleportEnabled();
            case "nobreak" -> bond.isNoBreakMode();
            case "armor" -> bond.getForcedBond().armorLocked;
            case "inventory" -> bond.isItemForwarding();
            default -> false;
        };
    }
    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("is_command_active"),
                new SerializableData().add("command", SerializableDataTypes.STRING),
               IsAllowedAnAction::condition
        );

    }
}
