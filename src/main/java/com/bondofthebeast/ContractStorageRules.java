package com.bondofthebeast;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;

public final class ContractStorageRules {
    private ContractStorageRules() {}

    public static boolean containsSignedContract(ItemStack stack) { return containsSignedContract(stack, 0); }

    private static boolean containsSignedContract(ItemStack stack, int depth) {
        if (!stack.hasNbt()) return false;
        var tag = stack.getNbt();
        if (stack.getItem() instanceof ContractItem && !tag.getString("OwnerUUID").isEmpty() && !tag.getString("PetUUID").isEmpty()) return true;
        // Inspect vanilla shulker boxes and bundles as well as the item itself.
        for (var container : java.util.List.of(tag, tag.getCompound("BlockEntityTag"))) {
            var items = container.getList("Items", NbtElement.COMPOUND_TYPE);
            if (!items.isEmpty() && depth >= 16) return true;
            for (int i = 0; i < items.size(); i++)
                if (containsSignedContract(ItemStack.fromNbt(items.getCompound(i)), depth + 1)) return true;
        }
        return false;
    }
}
