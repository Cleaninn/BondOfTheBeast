package com.bondofthebeast;

import com.bondofthebeast.block.PetBedBlockEntity;
import com.bondofthebeast.component.ModComponents;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.instinct.InstinctManager;
import net.onixary.shapeShifterCurseFabric.player_form.transform.TransformManager;

import java.util.UUID;

public final class BondService {
    private BondService() {}

    public static void register() {
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity pet) {
                LeashManager.detach(pet);
                releaseAbsorption(pet);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            PendingBondsState.get(server).apply(player);
            var b = ModComponents.PLAYER_BOND.get(player);
            if (b.isAbsorbed()) releaseAbsorption(player);
            if (!b.getLeashHolder().isEmpty()) LeashManager.detach(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ForcedTamingService.cancelCollaring(handler.player);
            if (ModComponents.PLAYER_BOND.get(handler.player).isAbsorbed()) releaseAbsorption(handler.player);
            if (!ModComponents.PLAYER_BOND.get(handler.player).getLeashHolder().isEmpty()) LeashManager.detach(handler.player);
            for (ServerPlayerEntity pet : server.getPlayerManager().getPlayerList()) {
                if (pet == handler.player) continue;
                var b = ModComponents.PLAYER_BOND.get(pet);
                if (b.getLeashHolder().equals(handler.player.getUuidAsString())) LeashManager.detach(pet);
                if (b.isAbsorbed() && BondRules.owns(handler.player, pet)) releaseAbsorption(pet);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity pet : server.getPlayerManager().getPlayerList()) {
                ForcedTamingService.tickCollaring(pet);
                var b = ModComponents.PLAYER_BOND.get(pet);
                LeashManager.tick(pet);
                if (!BondRules.allows(pet, "sit") && b.isSitting()) b.setSitting(false);
                if (b.isSitting() && b.getForcedBond().controlTicks <= 0) b.getForcedBond().controlTicks = 600;
                if (!BondRules.allows(pet, "nobreak")) {
                    if (b.isNoBreakMode()) b.setNoBreakMode(false);
                    if (b.isNoInteractMode()) b.setNoInteractMode(false);
                    if (b.isPacifistMode()) b.setPacifistMode(false);
                }
                if (!BondRules.allows(pet, "tp") && b.isTeleportEnabled()) b.setTeleportEnabled(false);
                if (!BondRules.allows(pet, "armor") && b.getForcedBond().armorLocked) {
                    b.getForcedBond().armorLocked = false;
                    ModComponents.PLAYER_BOND.sync(pet);
                }
                var bedWorld = LeashManager.dimension(pet, b.getBedDimension());
                if (b.getBedPos() != null && bedWorld != null && bedWorld.isChunkLoaded(b.getBedPos())) {
                    if (!(bedWorld.getBlockEntity(b.getBedPos()) instanceof PetBedBlockEntity bed) ||
                            !pet.getUuidAsString().equals(bed.getBoundPetUUID())) clearBed(pet);
                }
                if (b.isAbsorbed()) {
                    ServerPlayerEntity owner = BondRules.owner(pet);
                    if (owner == null || owner.getWorld() != pet.getWorld() || !BondRules.allows(pet, "absorb"))
                        releaseAbsorption(pet);
                }
                ForcedTamingService.tick(pet);
                VoluntaryBondService.tick(pet);
                if (b.hasOwner() && pet.age % 20 == 0) PetPreviewState.get(server).update(pet);

            }
        });
    }

    public static void clearBed(ServerPlayerEntity pet) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        var world = LeashManager.dimension(pet, b.getBedDimension());
        var pos = b.getBedPos();
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof PetBedBlockEntity bed &&
                pet.getUuidAsString().equals(bed.getBoundPetUUID())) {
            bed.setBoundPetUUID("");
            bed.setChainRadius(0);
        }
        if (pos != null && pos.up().equals(pet.getSpawnPointPosition()) &&
                pet.getSpawnPointDimension().getValue().toString().equals(b.getBedDimension()))
            pet.setSpawnPoint(pet.getSpawnPointDimension(), null, 0, false, false);
        b.setBedPos(null);
    }

    public static void releaseAbsorption(ServerPlayerEntity pet) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        if (!b.isAbsorbed()) return;
        b.getForcedBond().absorbedTicks = 0;
        b.setAbsorbed(false);
        pet.setCameraEntity(pet);
        GameMode previous = GameMode.byName(b.getAbsorbedGameMode(), GameMode.SURVIVAL);
        pet.changeGameMode(previous);
    }

    public static void removeCollars(ServerPlayerEntity pet) {
        TrinketsApi.getTrinketComponent(pet).ifPresent(c -> c.getInventory().values().forEach(g ->
                g.values().forEach(inv -> {
                    for (int i = 0; i < inv.size(); i++) {
                        if (inv.getStack(i).getItem() instanceof CollarItem) {
                            ItemStack item = inv.getStack(i).copy();
                            if (item.hasNbt()) item.getNbt().remove("OwnerName");
                            inv.setStack(i, ItemStack.EMPTY);
                            pet.dropItem(item, false);
                        }
                    }
                })));
    }

    public static void release(ServerPlayerEntity pet) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        String owner = b.getOwnerUUID();
        if (!owner.isEmpty()) PendingBondsState.get(pet.getServer()).removeFromOwner(pet.getServer(), owner, pet.getUuidAsString());
        LeashManager.detach(pet);
        releaseAbsorption(pet);
        clearBed(pet);
        removeCollars(pet);
        b.clearOwner();
        b.clearSkills();
        b.setBondLevel(1);
        b.setBondExperience(0);
        b.setSkillPoints(0);
    }

    public static void releaseContract(MinecraftServer server, String petId, String ownerId, String token) {
        try {
            var pet = server.getPlayerManager().getPlayer(UUID.fromString(petId));
            if (pet == null) { PendingBondsState.get(server).queueContractRelease(petId, ownerId, token); return; }
            var bond = ModComponents.PLAYER_BOND.get(pet);
            var state = bond.getForcedBond();
            if (!bond.getOwnerUUID().equals(ownerId)) return;
            if (state.forced ? state.token.isEmpty() || !state.token.equals(token) : !token.isEmpty()) return;
            release(pet);
        } catch (IllegalArgumentException invalid) { BondOfTheBeast.LOGGER.warn("Ignoring invalid contract UUID"); }
    }

    public static void release(MinecraftServer server, String petId, String ownerId) {
        try {
            var pet = server.getPlayerManager().getPlayer(UUID.fromString(petId));
            if (pet == null) PendingBondsState.get(server).queueRelease(petId, ownerId);
            else if (ModComponents.PLAYER_BOND.get(pet).getOwnerUUID().equals(ownerId)) release(pet);
            PendingBondsState.get(server).removeFromOwner(server, ownerId, petId);
        } catch (IllegalArgumentException e) {
            BondOfTheBeast.LOGGER.warn("Ignoring invalid bond UUID");
        }
    }
}
