package com.bondofthebeast;

import com.bondofthebeast.client.*;
import com.bondofthebeast.component.ModComponents;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BondOfTheBeastClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TrinketRendererRegistry.registerRenderer(ModItems.COLLAR, new CollarRenderer());

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_OWNER_GUI, (c, h, b, rs) -> c.execute(() -> c.setScreen(new ContractScreen(false))));
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_PET_GUI, (c, h, b, rs) -> c.execute(() -> c.setScreen(new ContractScreen(true))));

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_MANAGEMENT_GUI, (c, h, b, rs) -> {
            int count = b.readInt();
            List<StaffMainScreen.PetData> pets = new ArrayList<>();
            for (int i = 0; i < count; i++) pets.add(readPet(b));
            c.execute(() -> c.setScreen(new StaffMainScreen(pets)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_PET_STATS_GUI, (c, h, b, rs) -> {
            StaffMainScreen.PetData data = readPet(b);
            c.execute(() -> c.setScreen(new StaffPetScreen(null, data)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_BED_GUI, (c, h, b, rs) -> {
            BlockPos pos = b.readBlockPos();
            String petUUID = b.readString();
            int radius = b.readInt();
            int petCount = b.readInt();
            Map<String, PetBedScreen.PetOption> pets = new HashMap<>();
            for (int i = 0; i < petCount; i++) {
                String uuid = b.readString();
                String name = b.readString();
                boolean hasCollar = b.readBoolean();
                pets.put(uuid, new PetBedScreen.PetOption(name, hasCollar));
            }
            c.execute(() -> c.setScreen(new PetBedScreen(pos, petUUID, radius, pets)));
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (bond.hasOwner()) {
                net.minecraft.block.BlockState state = world.getBlockState(pos);

                // Проверяем, не бьет ли питомец любую из частей своей лежанки
                if (state.getBlock() instanceof com.bondofthebeast.block.PetBedBlock) {
                    BlockPos headPos = state.get(com.bondofthebeast.block.PetBedBlock.PART) == net.minecraft.block.enums.BedPart.HEAD ? pos : pos.offset(state.get(com.bondofthebeast.block.PetBedBlock.FACING));
                    if (world.getBlockEntity(headPos) instanceof com.bondofthebeast.block.PetBedBlockEntity bed) {
                        if (player.getUuidAsString().equals(bed.getBoundPetUUID())) return ActionResult.FAIL;
                    }
                }

                String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
                if (bond.getBlacklistedBlocks().contains(blockId)) return ActionResult.FAIL;
                if (bond.isNoBreakMode() && !bond.getWhitelistedBlocks().contains(blockId)) return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    private StaffMainScreen.PetData readPet(PacketByteBuf b) {
        UUID uuid = b.readUuid(); String name = b.readString();
        boolean sitting = b.readBoolean(); boolean tp = b.readBoolean(); boolean prot = b.readBoolean(); boolean aura = b.readBoolean();
        boolean pacifist = b.readBoolean(); boolean vampiric = b.readBoolean(); boolean noBreak = b.readBoolean(); boolean absorbed = b.readBoolean();
        boolean noInteract = b.readBoolean();
        int level = b.readInt(); int exp = b.readInt(); boolean collar = b.readBoolean();
        int skillPoints = b.readInt(); int skillsSize = b.readInt();
        Set<String> unlockedSkills = new HashSet<>(); for (int i = 0; i < skillsSize; i++) unlockedSkills.add(b.readString());
        int blackSize = b.readInt(); Set<String> black = new HashSet<>(); for (int i = 0; i < blackSize; i++) black.add(b.readString());
        int whiteSize = b.readInt(); Set<String> white = new HashSet<>(); for (int i = 0; i < whiteSize; i++) white.add(b.readString());
        boolean isOnline = b.readBoolean();
        return new StaffMainScreen.PetData(uuid, name, sitting, tp, prot, aura, pacifist, vampiric, noBreak, absorbed, noInteract, level, exp, collar, skillPoints, unlockedSkills, black, white, isOnline);
    }
}