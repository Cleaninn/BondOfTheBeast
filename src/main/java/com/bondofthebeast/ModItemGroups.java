package com.bondofthebeast;

import com.bondofthebeast.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup BOND_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(BondOfTheBeast.MOD_ID, "bond_group"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.COLLAR))
                    .displayName(Text.translatable("itemgroup.bondofthebeast.bond_group"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.FIDELITY_CONTRACT);
                        entries.add(ModItems.COMMAND_SCEPTER);
                        entries.add(ModItems.WHISTLE);
                        entries.add(ModItems.COLLAR);
                        entries.add(ModItems.PET_TREAT);
                        entries.add(ModBlocks.PET_BED);
                    })
                    .build());

    public static void registerItemGroups() {
        BondOfTheBeast.LOGGER.info("Registering Item Groups for " + BondOfTheBeast.MOD_ID);
    }
}