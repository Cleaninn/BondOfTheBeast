package com.bondofthebeast.component;

import net.minecraft.nbt.NbtCompound;

/** Persistent state of forced taming; SSC remains the source of transformation stages. */
public final class ForcedBondState {
    public boolean forced;
    public String token = "";
    public int warningTicks = -1;
    public boolean catalystWarningFromCookie;
    public int finalWarningTicks = -1;
    public int catalystStages;
    public int escapeTicks;
    public int resistanceCooldownTicks;
    public int lastStage = -99;
    public boolean armorLocked;
    public boolean contractSigned;
    public boolean contractPending;
    public final java.util.Map<String, Integer> commandTicks = new java.util.HashMap<>();

    public NbtCompound write() {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("Forced", forced);
        tag.putString("Token", token);
        tag.putInt("WarningTicks", warningTicks);
        tag.putBoolean("CatalystWarningFromCookie", catalystWarningFromCookie);
        tag.putInt("FinalWarningTicks", finalWarningTicks);
        tag.putInt("CatalystStages", catalystStages);
        tag.putInt("EscapeTicks", escapeTicks);
        tag.putInt("ResistanceCooldownTicks", resistanceCooldownTicks);
        tag.putInt("LastStage", lastStage);
        tag.putBoolean("ArmorLocked", armorLocked);
        tag.putBoolean("ContractSigned", contractSigned);
        tag.putBoolean("ContractPending", contractPending);
        NbtCompound commands = new NbtCompound();
        commandTicks.forEach(commands::putInt);
        tag.put("CommandTicks", commands);
        return tag;
    }

    public void read(NbtCompound tag) {
        forced = tag.getBoolean("Forced");
        token = tag.getString("Token");
        warningTicks = tag.contains("WarningTicks") ? Math.max(-1, Math.min(200, tag.getInt("WarningTicks"))) : -1;
        catalystWarningFromCookie = tag.getBoolean("CatalystWarningFromCookie");
        finalWarningTicks = tag.contains("FinalWarningTicks") ? Math.max(-1, Math.min(200, tag.getInt("FinalWarningTicks"))) : -1;
        catalystStages = tag.getInt("CatalystStages");
        escapeTicks = Math.max(0, Math.min(2400, tag.getInt("EscapeTicks")));
        resistanceCooldownTicks = Math.max(0, Math.min(6000, tag.getInt("ResistanceCooldownTicks")));
        lastStage = tag.contains("LastStage") ? tag.getInt("LastStage") : -99;
        armorLocked = tag.getBoolean("ArmorLocked");
        contractSigned = tag.getBoolean("ContractSigned");
        contractPending = tag.getBoolean("ContractPending") && !contractSigned;
        commandTicks.clear();
        var commands = tag.getCompound("CommandTicks");
        for (String key : commands.getKeys()) {
            int ticks = Math.max(0, Math.min(72000, commands.getInt(key)));
            if (ticks > 0) commandTicks.put(key, ticks);
        }
    }
}
