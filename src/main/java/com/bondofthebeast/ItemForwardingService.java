package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Forward new inventory arrivals, retaining old items and all overflow at the pet. */
public final class ItemForwardingService {
    private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();
    private ItemForwardingService() {}

    private record Key(Item item, NbtCompound nbt) {
        static Key of(ItemStack stack) { return new Key(stack.getItem(), stack.hasNbt() ? stack.getNbt().copy() : null); }
    }
    private record Entry(Key key, int count) {}
    private record Snapshot(ServerPlayerEntity player, Map<ItemStack, Entry> entries, Map<Key, Integer> counts) {}

    private static ServerPlayerEntity recipient(ServerPlayerEntity pet) {
        if (!pet.isAlive() || pet.isCreative() || pet.isSpectator() ||
                !ModComponents.PLAYER_BOND.get(pet).isItemForwarding() || !BondRules.allows(pet, "inventory")) return null;
        var owner = BondRules.owner(pet);
        return owner != null && owner != pet && owner.isAlive() && !owner.isSpectator() && BondRules.canOwn(owner) ? owner : null;
    }

    /** Never use creative-mode insertStack, which can discard overflow when the inventory is full. */
    private static int transfer(ServerPlayerEntity owner, ItemStack source) {
        int before = source.getCount();
        var inventory = owner.getInventory();
        for (ItemStack destination : inventory.main) {
            if (!source.isEmpty() && !destination.isEmpty() && ItemStack.canCombine(destination, source)) {
                int count = Math.min(source.getCount(), Math.max(0, Math.min(destination.getMaxCount(), inventory.getMaxCountPerStack()) - destination.getCount()));
                destination.increment(count);
                source.decrement(count);
            }
        }
        for (int slot = 0; slot < inventory.main.size() && !source.isEmpty(); slot++) {
            if (inventory.main.get(slot).isEmpty()) inventory.main.set(slot, source.split(Math.min(source.getMaxCount(), inventory.getMaxCountPerStack())));
        }
        int moved = before - source.getCount();
        if (moved > 0) {
            inventory.markDirty();
            owner.currentScreenHandler.sendContentUpdates();
        }
        return moved;
    }

    /** Handles pickups and give/offer calls before the incoming stack reaches pet slots. */
    public static void incoming(ServerPlayerEntity pet, ItemStack stack) {
        var owner = recipient(pet);
        if (owner == null || stack.isEmpty()) return;
        // Internal inventory/crafting stacks must retain their original ownership.
        for (ItemStack existing : stacks(pet)) if (existing == stack) return;
        transfer(owner, stack);
    }

    public static void baseline(ServerPlayerEntity pet) { SNAPSHOTS.put(pet.getUuid(), snapshot(pet)); }
    public static void forget(ServerPlayerEntity pet) { SNAPSHOTS.remove(pet.getUuid()); }

    /** Include cursor and personal crafting inputs so rearranging old belongings is not a new arrival. */
    private static List<ItemStack> stacks(ServerPlayerEntity pet) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < pet.getInventory().size(); i++) result.add(pet.getInventory().getStack(i));
        result.add(pet.currentScreenHandler.getCursorStack());
        var crafting = new IdentityHashMap<CraftingInventory, Boolean>();
        for (ScreenHandler handler : new ScreenHandler[]{pet.playerScreenHandler, pet.currentScreenHandler}) {
            for (var slot : handler.slots) {
                if (slot.inventory instanceof CraftingInventory inventory && crafting.put(inventory, true) == null) {
                    for (int i = 0; i < inventory.size(); i++) result.add(inventory.getStack(i));
                }
            }
        }
        return result;
    }

    private static Snapshot snapshot(ServerPlayerEntity pet) {
        Map<ItemStack, Entry> entries = new IdentityHashMap<>();
        Map<Key, Integer> counts = new HashMap<>();
        for (ItemStack stack : stacks(pet)) {
            if (stack.isEmpty() || entries.containsKey(stack)) continue;
            Key key = Key.of(stack);
            entries.put(stack, new Entry(key, stack.getCount()));
            counts.merge(key, stack.getCount(), Integer::sum);
        }
        return new Snapshot(pet, entries, counts);
    }

    /** Captures container transfers and recipes that bypass PlayerInventory.insertStack. */
    public static void tick(ServerPlayerEntity pet) {
        if (!ModComponents.PLAYER_BOND.get(pet).isItemForwarding()) {
            forget(pet);
            return;
        }
        var owner = recipient(pet);
        Snapshot previous = SNAPSHOTS.get(pet.getUuid());
        if (owner == null || previous == null || previous.player != pet) {
            baseline(pet);
            return;
        }
        List<ItemStack> current = stacks(pet);
        Map<Key, Integer> retained = new HashMap<>(previous.counts);
        // Durability, map data, or an item ticking in place is a mutation, not an incoming item.
        for (ItemStack stack : current) {
            Entry old = previous.entries.get(stack);
            if (old == null || stack.isEmpty()) continue;
            Key key = Key.of(stack);
            if (!key.equals(old.key)) {
                retained.merge(old.key, -old.count, Integer::sum);
                retained.merge(key, old.count, Integer::sum);
            }
        }
        boolean changed = false;
        for (ItemStack stack : current) {
            if (stack.isEmpty()) continue;
            Key key = Key.of(stack);
            int keep = Math.min(stack.getCount(), Math.max(0, retained.getOrDefault(key, 0)));
            retained.put(key, retained.getOrDefault(key, 0) - keep);
            int added = stack.getCount() - keep;
            if (added <= 0) continue;
            ItemStack incoming = stack.copyWithCount(added);
            int moved = transfer(owner, incoming);
            if (moved > 0) {
                stack.decrement(moved);
                changed = true;
            }
        }
        if (changed) {
            pet.getInventory().markDirty();
            pet.currentScreenHandler.sendContentUpdates();
        }
        baseline(pet);
    }
}
