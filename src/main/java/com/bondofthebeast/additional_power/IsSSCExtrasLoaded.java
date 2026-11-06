package com.bondofthebeast.additional_power;

import com.bondofthebeast.BondOfTheBeast;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.calio.data.SerializableData;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.entity.Entity;

public class IsSSCExtrasLoaded {
    public static boolean condition(SerializableData.Instance data, Entity entity) {
        return FabricLoaderImpl.INSTANCE.isModLoaded("ssc-extras");
    }
    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("extras_loaded"),
                new SerializableData(),
                IsSSCExtrasLoaded::condition
        );
    }
}
