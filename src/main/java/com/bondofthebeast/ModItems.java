package com.bondofthebeast;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item COLLAR = registerItem("collar", new CollarItem(new FabricItemSettings().maxCount(1)));
    public static final Item FIDELITY_CONTRACT = registerItem("fidelity_contract", new ContractItem(new FabricItemSettings().maxCount(1)));
    public static final Item WHISTLE = registerItem("whistle", new WhistleItem(new FabricItemSettings().maxCount(1)));

    public static final Item PET_TREAT = registerItem("pet_treat", new Item(new FabricItemSettings().maxCount(64)));

    public static final Item COMMAND_SCEPTER = registerItem("command_scepter",
            new CommandScepterItem(new FabricItemSettings().maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(BondOfTheBeast.MOD_ID, name), item);
    }

    public static void registerModItems() {
        BondOfTheBeast.LOGGER.info("Registering Mod Items for " + BondOfTheBeast.MOD_ID);
    }
}