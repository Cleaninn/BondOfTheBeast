package com.bondofthebeast.block;

import com.bondofthebeast.BondOfTheBeast;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static BlockEntityType<PetBedBlockEntity> PET_BED_BLOCK_ENTITY;

    public static void registerBlockEntities() {
        PET_BED_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(BondOfTheBeast.MOD_ID, "pet_bed"),
                FabricBlockEntityTypeBuilder.create(PetBedBlockEntity::new, ModBlocks.PET_BED).build(null));
    }
}