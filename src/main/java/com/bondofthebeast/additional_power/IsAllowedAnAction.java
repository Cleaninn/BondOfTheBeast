package com.bondofthebeast.additional_power;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.component.ModComponents;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.calio.data.SerializableData;
import com.bondofthebeast.BondRules;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class IsAllowedAnAction {

    public static boolean condition(SerializableData.Instance data, Entity entity) {
        String command = data.getString("command");
        if (entity instanceof PlayerEntity player) {
            var bond = ModComponents.PLAYER_BOND.get(entity);
            if (bond.hasOwner()) {
                if (command.equals("pacifism")){
                   return bond.isPacifistMode();
                }
                return BondRules.allows(player, command);
            }
        }
        return false;
    }
    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("is_command_active"),
                new SerializableData().add("command", SerializableDataTypes.STRING),
                HasOwnerCondition::condition
        );

    }
}
