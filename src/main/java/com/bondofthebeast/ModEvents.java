package com.bondofthebeast;

import com.bondofthebeast.block.PetBedBlockEntity;
import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModPackets {
    public static final Identifier OPEN_OWNER_GUI = new Identifier(BondOfTheBeast.MOD_ID, "open_owner_gui");
    public static final Identifier OPEN_PET_GUI = new Identifier(BondOfTheBeast.MOD_ID, "open_pet_gui");
    public static final Identifier OPEN_MANAGEMENT_GUI = new Identifier(BondOfTheBeast.MOD_ID, "open_management_gui");
    public static final Identifier OPEN_PET_STATS_GUI = new Identifier(BondOfTheBeast.MOD_ID, "open_pet_stats_gui");
    public static final Identifier OPEN_BED_GUI = new Identifier(BondOfTheBeast.MOD_ID, "open_bed_gui");
    public static final Identifier SIGN_CONTRACT_C2S = new Identifier(BondOfTheBeast.MOD_ID, "sign_contract_c2s");
    public static final Identifier UNLOCK_SKILL_C2S = new Identifier(BondOfTheBeast.MOD_ID, "unlock_skill_c2s");
    public static final Identifier UPDATE_BED_C2S = new Identifier(BondOfTheBeast.MOD_ID, "update_bed_c2s");
    public static final Identifier TOGGLE_PET_STATE_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_pet_state_c2s");
    public static final Identifier TOGGLE_TELEPORT_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_teleport_c2s");
    public static final Identifier TOGGLE_PROTECTION_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_protection_c2s");
    public static final Identifier TOGGLE_AURA_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_aura_c2s");
    public static final Identifier TOGGLE_PACIFIST_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_pacifist_c2s");
    public static final Identifier TOGGLE_VAMPIRIC_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_vampiric_c2s");
    public static final Identifier TOGGLE_NO_BREAK_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_no_break_c2s");
    public static final Identifier TOGGLE_ABSORB_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_absorb_c2s");
    public static final Identifier TOGGLE_NO_INTERACT_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_no_interact_c2s");
    public static final Identifier UPDATE_BLOCK_LISTS_C2S = new Identifier(BondOfTheBeast.MOD_ID, "update_block_lists_c2s");

    private static boolean hasActiveChain(PlayerBondComponent bond, ServerPlayerEntity pet) {
        BlockPos bedPos = bond.getBedPos();
        if (bedPos != null && pet.getWorld().getBlockEntity(bedPos) instanceof PetBedBlockEntity bed) {
            return bed.getChainRadius() > 0 && pet.getUuidAsString().equals(bed.getBoundPetUUID());
        }
        return false;
    }

    private static boolean canOwnerCommand(ServerPlayerEntity owner) {
        try {
            var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(owner);
            if (sscComp != null && sscComp.getCurrentForm() != null) {
                if (sscComp.getCurrentForm().FormID != null && sscComp.getCurrentForm().FormID.getPath().toLowerCase().contains("allay")) return true;
                return sscComp.getCurrentForm().getIndex() < 2;
            }
        } catch (Exception ignored) {}
        return true;
    }

    private static boolean canPetObey(ServerPlayerEntity pet) {
        try {
            var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(pet);
            if (sscComp != null && sscComp.getCurrentForm() != null) {
                if (sscComp.getCurrentForm().FormID != null && sscComp.getCurrentForm().FormID.getPath().toLowerCase().contains("allay")) return false;
                int index = sscComp.getCurrentForm().getIndex();
                boolean isFeral = sscComp.getCurrentForm().getBodyType() == net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType.FERAL;
                return index >= 3 || isFeral;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(SIGN_CONTRACT_C2S, (s, p, h, b, rs) -> s.execute(() -> {
            ItemStack stack = p.getMainHandStack();
            if (stack.getItem() instanceof ContractItem item) item.finalizeContract(stack, p);
        }));

        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_SKILL_C2S, (s, p, h, buf, rs) -> {
            UUID petUuid = buf.readUuid(); String skill = buf.readString();
            s.execute(() -> {
                if (!canOwnerCommand(p)) {
                    p.sendMessage(Text.translatable("text.bondofthebeast.owner_too_wild_to_command").formatted(Formatting.RED), true);
                    return;
                }
                ServerPlayerEntity pet = s.getPlayerManager().getPlayer(petUuid);
                if (pet != null) {
                    var bond = ModComponents.PLAYER_BOND.get(pet);
                    if (bond.hasOwner() && bond.getOwnerUUID().equals(p.getUuidAsString()) && !bond.isSkillUnlocked(skill) && bond.getSkillPoints() > 0) {
                        bond.addSkillPoints(-1); bond.unlockSkill(skill);
                        BondOfTheBeast.grantAdvancement(p, "owner_story/unlock_skill");
                        BondOfTheBeast.grantAdvancement(pet, "pet_story/skill_received");
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_BLOCK_LISTS_C2S, (s, p, h, buf, rs) -> {
            UUID petUuid = buf.readUuid();
            int listType = buf.readInt();
            int size = buf.readInt();
            Set<String> list = new HashSet<>();
            for (int i = 0; i < size; i++) list.add(buf.readString());
            s.execute(() -> {
                if (!canOwnerCommand(p)) {
                    p.sendMessage(Text.translatable("text.bondofthebeast.owner_too_wild_to_command").formatted(Formatting.RED), true);
                    return;
                }
                ServerPlayerEntity pet = s.getPlayerManager().getPlayer(petUuid);
                if (pet != null) {
                    var bond = ModComponents.PLAYER_BOND.get(pet);
                    if (bond.hasOwner() && bond.getOwnerUUID().equals(p.getUuidAsString())) {
                        if (listType == 0) bond.setBlacklistedBlocks(list);
                        else if (listType == 1) bond.setWhitelistedBlocks(list);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_BED_C2S, (s, p, h, buf, rs) -> {
            BlockPos pos = buf.readBlockPos();
            String petUUIDStr = buf.readString();
            int radius = buf.readInt();

            s.execute(() -> {
                if (!canOwnerCommand(p)) {
                    p.sendMessage(Text.translatable("text.bondofthebeast.owner_too_wild_to_command").formatted(Formatting.RED), true);
                    return;
                }

                ServerPlayerEntity pet = null;
                if (!petUUIDStr.isEmpty()) {
                    try {
                        pet = s.getPlayerManager().getPlayer(UUID.fromString(petUUIDStr));
                    } catch (Exception ignored) {}
                }

                // ПРОВЕРКА ЦЕПИ: если включают цепь, питомец обязан быть онлайн и стоять в этом радиусе
                if (radius > 0) {
                    if (pet == null) {
                        p.sendMessage(Text.translatable("text.bondofthebeast.packet.pet_offline_chain").formatted(Formatting.RED), true);
                        return;
                    }
                    double distToBed = pet.getPos().distanceTo(pos.toCenterPos());
                    if (distToBed > radius) {
                        p.sendMessage(Text.translatable("text.bondofthebeast.packet.pet_too_far_for_chain").formatted(Formatting.RED), true);
                        return;
                    }
                }

                if (pet != null) {
                    if (!canPetObey(pet)) {
                        p.sendMessage(Text.translatable("text.bondofthebeast.pet_not_wild_enough").formatted(Formatting.RED), true);
                        return;
                    }
                    double distance = p.getPos().distanceTo(pet.getPos());
                    if (distance > 50.0) {
                        p.sendMessage(Text.translatable("text.bondofthebeast.packet.bed_too_far").formatted(Formatting.RED), true);
                        return;
                    }
                    boolean hasCollar = TrinketsApi.getTrinketComponent(pet).map(c -> c.isEquipped(st -> st.getItem() instanceof CollarItem)).orElse(false);
                    if (!hasCollar) {
                        p.sendMessage(Text.translatable("text.bondofthebeast.packet.no_collar_bed").formatted(Formatting.RED), true);
                        return;
                    }
                    var bond = ModComponents.PLAYER_BOND.get(pet);
                    if (radius > 0 && (bond.isTeleportEnabled() || bond.isAbsorbed())) {
                        p.sendMessage(Text.translatable("text.bondofthebeast.conflict_teleport_absorb").formatted(Formatting.RED), true);
                        return;
                    }
                    BlockPos oldPos = bond.getBedPos();
                    if (oldPos != null && oldPos.equals(pos)) {
                        pet.sendMessage(Text.translatable("text.bondofthebeast.packet.bed_rebound").formatted(Formatting.GOLD), false);
                    } else {
                        if (oldPos != null) {
                            var oldBe = p.getServerWorld().getBlockEntity(oldPos);
                            if (oldBe instanceof PetBedBlockEntity oldBed) oldBed.setBoundPetUUID("");
                        }
                        pet.sendMessage(Text.translatable("text.bondofthebeast.packet.bound_to_new_bed").formatted(Formatting.GOLD), false);
                    }
                    bond.setBedPos(pos);
                    pet.setSpawnPoint(p.getServerWorld().getRegistryKey(), pos.up(), 0.0f, true, true);
                }

                var be = p.getServerWorld().getBlockEntity(pos);
                if (be instanceof PetBedBlockEntity bed) {
                    bed.setBoundPetUUID(petUUIDStr);
                    bed.setChainRadius(radius);
                    bed.markDirty();
                    p.getServerWorld().updateListeners(pos, p.getServerWorld().getBlockState(pos), p.getServerWorld().getBlockState(pos), 3);
                    p.sendMessage(Text.translatable("text.bondofthebeast.packet.bed_setup_success").formatted(Formatting.GREEN), true);
                }
            });
        });

        registerToggle(TOGGLE_PET_STATE_C2S, (b, p, pet) -> b.setSitting(!b.isSitting()));
        registerToggle(TOGGLE_PACIFIST_C2S, (b, p, pet) -> b.setPacifistMode(!b.isPacifistMode()));
        registerToggle(TOGGLE_NO_BREAK_C2S, (b, p, pet) -> {
            boolean newState = !b.isNoBreakMode();
            b.setNoBreakMode(newState);
            if (!newState) {
                b.setNoInteractMode(false);
                b.setPacifistMode(false);
            }
        });
        registerToggle(TOGGLE_TELEPORT_C2S, (b, p, pet) -> {
            if (!b.isTeleportEnabled() && hasActiveChain(b, pet)) {
                p.sendMessage(Text.translatable("text.bondofthebeast.conflict_chain").formatted(Formatting.RED), true);
                return;
            }
            b.setTeleportEnabled(!b.isTeleportEnabled());
        });
        registerToggle(TOGGLE_PROTECTION_C2S, (b, p, pet) -> b.setProtectionMode(!b.isProtectionMode()));
        registerToggle(TOGGLE_AURA_C2S, (b, p, pet) -> b.setAuraEnabled(!b.isAuraEnabled()));
        registerToggle(TOGGLE_VAMPIRIC_C2S, (b, p, pet) -> b.setVampiricMode(!b.isVampiricMode()));
        registerToggle(TOGGLE_NO_INTERACT_C2S, (b, p, pet) -> b.setNoInteractMode(!b.isNoInteractMode()));
        registerToggle(TOGGLE_ABSORB_C2S, (b, p, pet) -> {
            if (!b.isSkillUnlocked("absorb")) return;
            boolean st = !b.isAbsorbed();
            if (st) {
                if (hasActiveChain(b, pet)) {
                    p.sendMessage(Text.translatable("text.bondofthebeast.conflict_chain").formatted(Formatting.RED), true);
                    return;
                }
                if (pet.squaredDistanceTo(p) > 25.0) return;
                b.setAbsorbed(true); b.setSitting(false);
                pet.changeGameMode(GameMode.SPECTATOR); pet.setCameraEntity(p);
                BondOfTheBeast.grantAdvancement(p, "owner_story/absorb");
                BondOfTheBeast.grantAdvancement(pet, "pet_story/absorbed");
            } else {
                b.setAbsorbed(false); pet.changeGameMode(GameMode.SURVIVAL);
                pet.teleport(p.getServerWorld(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
            }
        });
    }

    private static void registerToggle(Identifier id, ToggleHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(id, (s, p, h, b, rs) -> {
            UUID uuid = b.readUuid();
            s.execute(() -> {
                if (!canOwnerCommand(p)) {
                    p.sendMessage(Text.translatable("text.bondofthebeast.owner_too_wild_to_command").formatted(Formatting.RED), true);
                    return;
                }
                ServerPlayerEntity pet = s.getPlayerManager().getPlayer(uuid);
                if (pet != null) {
                    if (!canPetObey(pet)) {
                        p.sendMessage(Text.translatable("text.bondofthebeast.pet_not_wild_enough").formatted(Formatting.RED), true);
                        return;
                    }
                    var bond = ModComponents.PLAYER_BOND.get(pet);
                    if (bond.hasOwner() && bond.getOwnerUUID().equals(p.getUuidAsString())) handler.handle(bond, p, pet);
                }
            });
        });
    }

    private interface ToggleHandler { void handle(PlayerBondComponent bond, ServerPlayerEntity player, ServerPlayerEntity pet); }

    public static void writePetData(PacketByteBuf buf, UUID petUuid, String fallbackName, MinecraftServer server) {
        ServerPlayerEntity onlinePet = server.getPlayerManager().getPlayer(petUuid);
        buf.writeUuid(petUuid);
        if (onlinePet != null) {
            PlayerBondComponent petBond = ModComponents.PLAYER_BOND.get(onlinePet);
            boolean hasCollar = TrinketsApi.getTrinketComponent(onlinePet).map(c -> c.isEquipped(s -> s.getItem() instanceof CollarItem)).orElse(false);
            String nickname = petBond.getPetNickname();
            String username = onlinePet.getGameProfile().getName();
            buf.writeString((nickname != null && !nickname.isEmpty()) ? nickname + "|" + username : username + "|" + username);
            buf.writeBoolean(petBond.isSitting()); buf.writeBoolean(petBond.isTeleportEnabled()); buf.writeBoolean(petBond.isProtectionMode());
            buf.writeBoolean(petBond.isAuraEnabled()); buf.writeBoolean(petBond.isPacifistMode()); buf.writeBoolean(petBond.isVampiricMode());
            buf.writeBoolean(petBond.isNoBreakMode()); buf.writeBoolean(petBond.isAbsorbed()); buf.writeBoolean(petBond.isNoInteractMode());
            buf.writeInt(petBond.getBondLevel()); buf.writeInt(petBond.getBondExperience()); buf.writeBoolean(hasCollar);
            buf.writeInt(petBond.getSkillPoints());
            Set<String> skills = petBond.getUnlockedSkills(); buf.writeInt(skills.size()); for(String s : skills) buf.writeString(s);
            Set<String> black = petBond.getBlacklistedBlocks(); buf.writeInt(black.size()); for(String s : black) buf.writeString(s);
            Set<String> white = petBond.getWhitelistedBlocks(); buf.writeInt(white.size()); for(String s : white) buf.writeString(s);
            buf.writeBoolean(true);
        } else {
            buf.writeString(fallbackName + "|" + fallbackName);
            buf.writeBoolean(false); buf.writeBoolean(false); buf.writeBoolean(false);
            buf.writeBoolean(false); buf.writeBoolean(false); buf.writeBoolean(false);
            buf.writeBoolean(false); buf.writeBoolean(false); buf.writeBoolean(false);
            buf.writeInt(1); buf.writeInt(0);
            buf.writeBoolean(true); // Фейково говорим GUI, что ошейник есть (чтобы пустить в меню выбора)
            buf.writeInt(0);
            buf.writeInt(1); buf.writeString("sit");
            buf.writeInt(0);
            buf.writeInt(0);
            buf.writeBoolean(false);
        }
    }
}