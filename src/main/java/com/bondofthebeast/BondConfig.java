package com.bondofthebeast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BondConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BondConfig current = new BondConfig();
    public boolean blockSignedContractsInEnderChests = true;
    public int[] forcedCommandSecondsByStage = {10, 30, 60, 120};

    public static BondConfig get() { return current; }
    public static int durationTicks(int stage) { return current.forcedCommandSecondsByStage[Math.max(0, Math.min(3, stage))] * 20; }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("bondofthebeast.json");
        try {
            if (Files.exists(path)) {
                BondConfig loaded = GSON.fromJson(Files.readString(path), BondConfig.class);
                if (loaded == null) throw new IllegalArgumentException("Empty configuration");
                if (loaded.forcedCommandSecondsByStage == null || loaded.forcedCommandSecondsByStage.length != 4)
                    throw new IllegalArgumentException("forcedCommandSecondsByStage must contain four values");
                int previous = 1;
                for (int i = 0; i < 4; i++) {
                    loaded.forcedCommandSecondsByStage[i] = Math.max(previous, Math.min(3600, loaded.forcedCommandSecondsByStage[i]));
                    previous = loaded.forcedCommandSecondsByStage[i];
                }
                current = loaded;
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(current));
            }
        } catch (Exception e) {
            BondOfTheBeast.LOGGER.error("Cannot load {}. Keeping current settings.", path, e);
        }
    }

    public static void applyServerSettings(boolean block, int[] seconds) {
        BondConfig settings = new BondConfig();
        settings.blockSignedContractsInEnderChests = block;
        settings.forcedCommandSecondsByStage = seconds;
        current = settings;
    }
}
