package com.bondofthebeast.component;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.util.Identifier;

public class ModComponents implements EntityComponentInitializer {
    public static final ComponentKey<PlayerBondComponent> PLAYER_BOND =
            ComponentRegistry.getOrCreate(new Identifier("bondofthebeast", "player_bond"), PlayerBondComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(PLAYER_BOND, PlayerBondComponentImpl::new, RespawnCopyStrategy.ALWAYS_COPY);
    }
}