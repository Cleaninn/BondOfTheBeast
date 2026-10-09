package com.bondofthebeast.component;

import net.minecraft.nbt.NbtCompound;

/** Persisted duration of a contract bond; independent of SSC transformation. */
public final class VoluntaryBondState {
    public int tier;
    public int activeTicks;

    public NbtCompound write() {
        NbtCompound tag = new NbtCompound();
        tag.putInt("Tier", tier);
        tag.putInt("ActiveTicks", activeTicks);
        return tag;
    }

    public void read(NbtCompound tag) {
        tier = Math.max(0, Math.min(3, tag.getInt("Tier")));
        activeTicks = tag.contains("ActiveTicks") ? Math.max(0, Math.min(432000, tag.getInt("ActiveTicks"))) :
                com.bondofthebeast.VoluntaryBondService.ticksForTier(tier);
    }
}
