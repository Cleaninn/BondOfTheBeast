package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Delivers releases and registry cleanup when an offline player returns. */
public final class PendingBondsState extends PersistentState {
    private final Map<String, String> releases = new HashMap<>();
    private final Map<String, String> contractReleases = new HashMap<>();
    private final Map<String, String> removals = new HashMap<>();

    public static PendingBondsState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                PendingBondsState::read, PendingBondsState::new, "bondofthebeast_pending");
    }
    private static PendingBondsState read(NbtCompound tag) {
        var state = new PendingBondsState();
        NbtCompound releases = tag.getCompound("Releases");
        for (String key : releases.getKeys()) state.releases.put(key, releases.getString(key));
        NbtCompound contracts = tag.getCompound("ContractReleases");
        for (String key : contracts.getKeys()) state.contractReleases.put(key, contracts.getString(key));
        NbtCompound oldTokens = tag.getCompound("ContractTokens");
        for (String key : oldTokens.getKeys()) {
            String owner = state.releases.remove(key);
            if (owner != null) state.contractReleases.put(key + "/" + oldTokens.getString(key), owner);
        }
        NbtCompound removals = tag.getCompound("Removals");
        for (String key : removals.getKeys()) state.removals.put(key, removals.getString(key));
        return state;
    }
    @Override public NbtCompound writeNbt(NbtCompound tag) {
        NbtCompound r = new NbtCompound(); releases.forEach(r::putString); tag.put("Releases", r);
        NbtCompound o = new NbtCompound(); removals.forEach(o::putString); tag.put("Removals", o);
        NbtCompound t = new NbtCompound(); contractReleases.forEach(t::putString); tag.put("ContractReleases", t);
        return tag;
    }
    public void queueRelease(String pet, String owner) { releases.put(pet, owner); contractReleases.keySet().removeIf(k -> k.startsWith(pet + "/")); markDirty(); }
    public void queueContractRelease(String pet, String owner, String token) {
        contractReleases.put(pet + "/" + token, owner); markDirty();
    }
    public void removeFromOwner(MinecraftServer server, String owner, String pet) {
        try {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(UUID.fromString(owner));
            if (player != null) ModComponents.PLAYER_BOND.get(player).removePetFromRegistry(pet);
            else { removals.put(owner + "/" + pet, pet); markDirty(); }
        } catch (IllegalArgumentException ignored) {}
    }
    public void apply(ServerPlayerEntity player) {
        String owner = releases.remove(player.getUuidAsString());
        if (owner != null && owner.equals(ModComponents.PLAYER_BOND.get(player).getOwnerUUID())) BondService.release(player);
        var contractKeys = contractReleases.keySet().stream().filter(k -> k.startsWith(player.getUuidAsString() + "/")).toList();
        for (String key : contractKeys) {
            String signer = contractReleases.remove(key);
            String token = key.substring(player.getUuidAsString().length() + 1);
            var bond = ModComponents.PLAYER_BOND.get(player);
            var state = bond.getForcedBond();
            boolean valid = state.forced ? !state.token.isEmpty() && state.token.equals(token) : token.isEmpty();
            if (valid && signer.equals(bond.getOwnerUUID())) BondService.release(player);
        }
        var keys = removals.keySet().stream().filter(k -> k.startsWith(player.getUuidAsString() + "/")).toList();
        for (String key : keys) ModComponents.PLAYER_BOND.get(player).removePetFromRegistry(removals.remove(key));
        markDirty();
    }
}
