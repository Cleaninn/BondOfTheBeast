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

    public static final Identifier REQUEST_ESCAPE_C2S = new Identifier(BondOfTheBeast.MOD_ID, "request_escape_c2s");
    public static final Identifier OPEN_ARMOR_C2S = new Identifier(BondOfTheBeast.MOD_ID, "open_armor_c2s");
    public static final Identifier TOGGLE_ARMOR_C2S = new Identifier(BondOfTheBeast.MOD_ID, "toggle_armor_c2s");

    private static boolean canOwnerCommand(ServerPlayerEntity owner) { return BondRules.canOwn(owner); }
    private static boolean canPetObey(ServerPlayerEntity pet) { return BondRules.canObey(pet); }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_ESCAPE_C2S, (server, player, network, buf, sender) ->
                server.execute(() -> ForcedTamingService.determination(player)));
        ServerPlayNetworking.registerGlobalReceiver(OPEN_ARMOR_C2S, (server, owner, network, buf, sender) -> {
            UUID id = buf.readUuid();
            server.execute(() -> {
                ServerPlayerEntity pet = controlledPet(owner, id);
                if (pet != null && BondRules.allows(pet, "armor")) PetArmorScreenHandler.open(owner, pet);
            });
        });
        registerToggle(TOGGLE_ARMOR_C2S, "armor", (b, owner, pet) -> {
            b.getForcedBond().armorLocked = !b.getForcedBond().armorLocked;
            ModComponents.PLAYER_BOND.sync(pet);
        });
        ServerPlayNetworking.registerGlobalReceiver(SIGN_CONTRACT_C2S, (server, player, handler, buf, sender) ->
                server.execute(() -> {
                    ItemStack stack = player.getMainHandStack();
                    if (stack.getItem() instanceof ContractItem contract) contract.finalizeContract(stack, player);
                }));
        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_SKILL_C2S, (server, player, handler, buf, sender) -> {
            UUID id = buf.readUuid(); String skill = buf.readString(32);
            server.execute(() -> {
                ServerPlayerEntity pet = controlledPet(player, id);
                if (pet == null) return;
                var bond = ModComponents.PLAYER_BOND.get(pet);
                // Legacy purchase channel: stage unlocks are automatic and never spend points.
                refresh(player, pet);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_BLOCK_LISTS_C2S, (server, player, handler, buf, sender) -> {
            UUID id = buf.readUuid(); int type = buf.readInt(); int size = buf.readInt();
            if (size < 0 || size > 4096 || (type != 0 && type != 1)) return;
            Set<String> blocks = new HashSet<>();
            for (int i = 0; i < size; i++) {
                Identifier key = Identifier.tryParse(buf.readString(256));
                if (key != null && net.minecraft.registry.Registries.BLOCK.containsId(key)) blocks.add(key.toString());
            }
            server.execute(() -> {
                ServerPlayerEntity pet = controlledPet(player, id);
                if (pet == null) return;
                var bond = ModComponents.PLAYER_BOND.get(pet);
                if (!BondRules.allows(pet, "nobreak")) return;
                if (type == 0) bond.setBlacklistedBlocks(blocks); else bond.setWhitelistedBlocks(blocks);
                refresh(player, pet);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_BED_C2S, (server, player, handler, buf, sender) -> {
            BlockPos pos = buf.readBlockPos(); String id = buf.readString(36); int radius = buf.readInt();
            server.execute(() -> updateBed(player, pos, id, radius));
        });
        registerToggle(TOGGLE_PET_STATE_C2S, "sit", (b, p, pet) -> {
            b.setSitting(!b.isSitting());
            b.getForcedBond().controlTicks = b.isSitting() ? 600 : 0;
        });
        registerToggle(TOGGLE_PACIFIST_C2S, "nobreak", (b, p, pet) -> {
            if (b.isNoBreakMode()) b.setPacifistMode(!b.isPacifistMode());
        });
        registerToggle(TOGGLE_NO_BREAK_C2S, "nobreak", (b, p, pet) -> {
            b.setNoBreakMode(!b.isNoBreakMode());
            if (!b.isNoBreakMode()) { b.setNoInteractMode(false); b.setPacifistMode(false); }
        });
        registerToggle(TOGGLE_TELEPORT_C2S, "tp", (b, p, pet) -> {
            if (LeashManager.isTethered(pet)) return;
            b.setTeleportEnabled(!b.isTeleportEnabled());
        });
        registerToggle(TOGGLE_PROTECTION_C2S, "prot", (b, p, pet) -> {
            b.setProtectionMode(!b.isProtectionMode());
        });
        registerToggle(TOGGLE_AURA_C2S, "aura", (b, p, pet) -> {
            b.setAuraEnabled(!b.isAuraEnabled());
        });
        registerToggle(TOGGLE_VAMPIRIC_C2S, "vampiric", (b, p, pet) -> b.setVampiricMode(!b.isVampiricMode()));
        registerToggle(TOGGLE_NO_INTERACT_C2S, "nobreak", (b, p, pet) -> {
            if (b.isNoBreakMode()) b.setNoInteractMode(!b.isNoInteractMode());
        });
        registerToggle(TOGGLE_ABSORB_C2S, "absorb", (b, p, pet) -> {
            if (b.isAbsorbed()) {
                BondService.releaseAbsorption(pet);
                LeashManager.teleportSafely(pet, p.getServerWorld(), p.getPos());
            } else {
                if (pet.getWorld() != p.getWorld() || pet.squaredDistanceTo(p) > 25 ||
                        LeashManager.isTethered(pet) || pet.isSleeping()) return;
                b.setAbsorbedGameMode(pet.interactionManager.getGameMode().getName());
                b.setAbsorbed(true); b.setSitting(false);
                pet.changeGameMode(GameMode.SPECTATOR); pet.setCameraEntity(p);
                BondOfTheBeast.grantAdvancement(p, "owner_story/absorb");
                BondOfTheBeast.grantAdvancement(pet, "pet_story/absorbed");
            }
        });
    }

    private static ServerPlayerEntity controlledPet(ServerPlayerEntity owner, UUID id) {
        if (!canOwnerCommand(owner)) return null;
        ServerPlayerEntity pet = owner.getServer().getPlayerManager().getPlayer(id);
        return pet != null && BondRules.owns(owner, pet) && canPetObey(pet) && BondRules.hasCollar(pet) ? pet : null;
    }

    private static void refresh(ServerPlayerEntity owner, ServerPlayerEntity pet) {
        var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        writePetData(buf, pet.getUuid(), pet.getGameProfile().getName(), owner.getServer());
        ServerPlayNetworking.send(owner, OPEN_PET_STATS_GUI, buf);
    }

    private static void updateBed(ServerPlayerEntity owner, BlockPos pos, String petId, int radius) {
        if (!canOwnerCommand(owner) || radius < 0 || radius > 50 || owner.squaredDistanceTo(pos.toCenterPos()) > 64 ||
                !(owner.getServerWorld().getBlockEntity(pos) instanceof PetBedBlockEntity bed) ||
                bed.getCachedState().get(com.bondofthebeast.block.PetBedBlock.PART) != net.minecraft.block.enums.BedPart.HEAD) return;
        var ownerBond = ModComponents.PLAYER_BOND.get(owner);
        if (!bed.getBoundPetUUID().isEmpty() && !ownerBond.getRegisteredPets().containsKey(bed.getBoundPetUUID())) return;
        ServerPlayerEntity pet = null;
        if (!petId.isEmpty()) {
            try { pet = controlledPet(owner, UUID.fromString(petId)); }
            catch (IllegalArgumentException ignored) { return; }
            if (pet == null || pet.getWorld() != owner.getWorld() || pet.squaredDistanceTo(pos.toCenterPos()) > 2500) {
                owner.sendMessage(Text.translatable("text.bondofthebeast.packet.pet_offline_chain"), true);
                return;
            }
            var bond = ModComponents.PLAYER_BOND.get(pet);
            if (radius > 0 && !BondRules.allows(pet, "sit")) return;
            if (radius > 0 && (bond.isTeleportEnabled() || bond.isAbsorbed() ||
                    bond.getLeashPos() != null || !bond.getLeashHolder().isEmpty())) {
                owner.sendMessage(Text.translatable("text.bondofthebeast.conflict_teleport_absorb"), true);
                return;
            }
        }
        String previousId = bed.getBoundPetUUID();
        if (!previousId.isEmpty()) {
            try {
                ServerPlayerEntity previous = owner.getServer().getPlayerManager().getPlayer(UUID.fromString(previousId));
                if (previous != null && pos.equals(ModComponents.PLAYER_BOND.get(previous).getBedPos())) BondService.clearBed(previous);
            } catch (IllegalArgumentException ignored) {}
        }
        if (pet != null) {
            BondService.clearBed(pet);
            ModComponents.PLAYER_BOND.get(pet).setBedPos(pos);
            pet.setSpawnPoint(owner.getServerWorld().getRegistryKey(), pos.up(), 0, true, true);
        }
        bed.setBoundPetUUID(petId);
        bed.setChainRadius(petId.isEmpty() ? 0 : radius);
        owner.sendMessage(Text.translatable("text.bondofthebeast.packet.bed_setup_success"), true);
    }

    private static void registerToggle(Identifier id, String skill, ToggleHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, owner, network, buf, sender) -> {
            UUID petId = buf.readUuid();
            server.execute(() -> {
                ServerPlayerEntity pet = controlledPet(owner, petId);
                if (pet == null) return;
                var bond = ModComponents.PLAYER_BOND.get(pet);
                if (BondRules.allows(pet, skill)) handler.handle(bond, owner, pet);
                refresh(owner, pet);
            });
        });
    }
    private interface ToggleHandler { void handle(PlayerBondComponent bond, ServerPlayerEntity player, ServerPlayerEntity pet); }

    public static void writePetData(PacketByteBuf buf, UUID petUuid, String fallbackName, MinecraftServer server) {
        ServerPlayerEntity onlinePet = server.getPlayerManager().getPlayer(petUuid);
        if (onlinePet != null) PetPreviewState.get(server).update(onlinePet);
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
            Set<String> skills = new java.util.HashSet<>(); for (String command : java.util.List.of("sit", "tp", "prot", "nobreak", "absorb", "vampiric", "aura", "armor")) if (BondRules.allows(onlinePet, command)) skills.add(command); buf.writeInt(skills.size()); for(String s : skills) buf.writeString(s);
            Set<String> black = petBond.getBlacklistedBlocks(); buf.writeInt(black.size()); for(String s : black) buf.writeString(s);
            Set<String> white = petBond.getWhitelistedBlocks(); buf.writeInt(white.size()); for(String s : white) buf.writeString(s);
            buf.writeBoolean(true);
        } else {
            buf.writeString(fallbackName + "|" + fallbackName);
            buf.writeBoolean(false); buf.writeBoolean(false); buf.writeBoolean(false);
            buf.writeBoolean(false); buf.writeBoolean(false); buf.writeBoolean(false);
            buf.writeBoolean(false); buf.writeBoolean(false); buf.writeBoolean(false);
            buf.writeInt(1); buf.writeInt(0);
            buf.writeBoolean(true); // Теперь фейково говорим GUI, что ошейник есть (чтобы пустить в меню выбора)
            buf.writeInt(0);
            buf.writeInt(1); buf.writeString("sit");
            buf.writeInt(0);
            buf.writeInt(0);
            buf.writeBoolean(false);
        }
        buf.writeInt(onlinePet == null ? -2 : ForcedTamingService.stage(onlinePet));
        var state = onlinePet == null ? new com.bondofthebeast.component.ForcedBondState() : ModComponents.PLAYER_BOND.get(onlinePet).getForcedBond();
        buf.writeInt(onlinePet == null ? 0 : ModComponents.PLAYER_BOND.get(onlinePet).getVoluntaryBond().tier);
        buf.writeBoolean(state.forced); buf.writeBoolean(state.armorLocked); buf.writeInt(state.escapeTicks);
        buf.writeFloat(onlinePet == null ? 0 : net.onixary.shapeShifterCurseFabric.player_form.instinct.RegPlayerInstinctComponent.PLAYER_INSTINCT_COMP.get(onlinePet).instinctValue);
        buf.writeInt(onlinePet == null ? 0 : ModComponents.PLAYER_BOND.get(onlinePet).getVoluntaryBond().activeTicks);
        buf.writeString(PetPreviewState.get(server).form(petUuid));
    }
}
