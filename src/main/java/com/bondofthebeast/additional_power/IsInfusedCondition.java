package com.bondofthebeast.additional_power;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.CollarCatalystService;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class IsInfusedCondition {
    public static boolean condition(SerializableData.Instance data, Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return CollarCatalystService.active(player);
        }
        return false;
    }
    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("infused"),
                new SerializableData(),
                IsInfusedCondition::condition
        );

    }
}
