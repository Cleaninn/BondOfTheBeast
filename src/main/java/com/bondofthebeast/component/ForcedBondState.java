package com.bondofthebeast.component;

import net.minecraft.nbt.NbtCompound;

/** Persistent state of forced taming; SSC remains the source of transformation stages. */
public final class ForcedBondState {
    public boolean forced;
    public String token = "";
    public int warningTicks = -1;
    public int finalWarningTicks = -1;
    public int catalystStages;
    public int escapeTicks;
    public int resistanceCooldownTicks;
    public int escapeCharge;
    public int controlTicks;
    public int absorbedTicks;
    public int lastStage = -99;
    public boolean armorLocked;
    public boolean contractSigned;

    public NbtCompound write() {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("Forced", forced);
        tag.putString("Token", token);
        tag.putInt("WarningTicks", warningTicks);
        tag.putInt("FinalWarningTicks", finalWarningTicks);
        tag.putInt("CatalystStages", catalystStages);
        tag.putInt("EscapeTicks", escapeTicks);
        tag.putInt("ResistanceCooldownTicks", resistanceCooldownTicks);
        tag.putInt("ControlTicks", controlTicks);
        tag.putInt("AbsorbedTicks", absorbedTicks);
        tag.putInt("LastStage", lastStage);
        tag.putBoolean("ArmorLocked", armorLocked);
        tag.putBoolean("ContractSigned", contractSigned);
        return tag;
    }

    public void read(NbtCompound tag) {
        forced = tag.getBoolean("Forced");
        token = tag.getString("Token");
        warningTicks = tag.contains("WarningTicks") ? Math.max(-1, Math.min(200, tag.getInt("WarningTicks"))) : -1;
        finalWarningTicks = tag.contains("FinalWarningTicks") ? Math.max(-1, Math.min(200, tag.getInt("FinalWarningTicks"))) : -1;
        catalystStages = tag.getInt("CatalystStages");
        escapeTicks = Math.max(0, Math.min(2400, tag.getInt("EscapeTicks")));
        resistanceCooldownTicks = Math.max(0, Math.min(6000, tag.getInt("ResistanceCooldownTicks")));
        controlTicks = Math.max(0, Math.min(600, tag.getInt("ControlTicks")));
        absorbedTicks = Math.max(0, Math.min(1200, tag.getInt("AbsorbedTicks")));
        lastStage = tag.contains("LastStage") ? tag.getInt("LastStage") : -99;
        armorLocked = tag.getBoolean("ArmorLocked");
        contractSigned = tag.getBoolean("ContractSigned");
        escapeCharge = 0;
    }
}
