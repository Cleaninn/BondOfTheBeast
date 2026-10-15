package com.bondofthebeast.additional_power;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.ModItems;
import dev.emi.trinkets.api.TrinketsApi;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class HasEnergySourceCondition {
    private HasEnergySourceCondition() {}

    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(BondOfTheBeast.identifier("has_energy_source"), new SerializableData(),
                (data, entity) -> entity instanceof PlayerEntity player && TrinketsApi.getTrinketComponent(player)
                        .map(component -> component.isEquipped(ModItems.ENERGY_SOURCE_RING)).orElse(false));
    }
}
