package com.bondofthebeast.additional_power;


import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.factory.condition.BiEntityConditions;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.entity.Entity;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import net.minecraft.util.Pair;

public class IsOwnerCondition {
    public static boolean condition(SerializableData.Instance data, Pair<Entity, Entity> ActorAndTarget) {
        Entity potential_owner = ActorAndTarget.getLeft();
        Entity potential_pet = ActorAndTarget.getRight();
        if ((potential_owner instanceof PlayerEntity) && (potential_pet instanceof PlayerEntity)) {
            var bond = ModComponents.PLAYER_BOND.get(potential_pet);
            return bond.getOwnerUUID().equals(potential_owner.getUuid().toString());
        }
        return false;
    }
    public static ConditionFactory<Pair<Entity,Entity>>  getFactory() {
        return new ConditionFactory<>(
                Apoli.identifier("is_owner"),
                new SerializableData(),
                IsOwnerCondition::condition
        );

    }
}
