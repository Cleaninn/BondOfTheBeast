package com.bondofthebeast;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.event.GameEvent;

import java.util.HashSet;

public final class PetFeedingEffects {
    private PetFeedingEffects() {}

    // Call after feeding is accepted, before consuming the owner's last item.
    public static void play(ServerPlayerEntity pet, ItemStack food) {
        if (food.isEmpty()) return;
        ItemStack bite = food.copyWithCount(1);
        var viewers = new HashSet<>(PlayerLookup.tracking(pet));
        viewers.add(pet);
        for (var viewer : viewers) {
            var buffer = PacketByteBufs.create();
            buffer.writeUuid(pet.getUuid());
            buffer.writeItemStack(bite);
            ServerPlayNetworking.send(viewer, ModPackets.PET_FEEDING_S2C, buffer);
        }
        pet.emitGameEvent(GameEvent.EAT);
    }
}
