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
        var command = data.get("command");
        if (entity instanceof PlayerEntity) {
            var bond = ModComponents.PLAYER_BOND.get(entity);
            if (bond.hasOwner()) {
                return BondRules.allows((PlayerEntity) entity, (String) command);
            }
        }
        return true;
    }
    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("allowedAnAction"),
                new SerializableData().add("contion", SerializableDataTypes.STRING),
                HasOwnerCondition::condition
        );

    }
}
