package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModCommands {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        var rootCommand = CommandManager.literal("bondofthebeast").requires(source -> source.hasPermissionLevel(2));

        rootCommand.then(CommandManager.literal("info").then(CommandManager.argument("target", EntityArgumentType.player()).executes(ModCommands::showDetailedInfo)));
        rootCommand.then(CommandManager.literal("status").then(CommandManager.argument("target", EntityArgumentType.player()).executes(ModCommands::showStatus)));
        rootCommand.then(CommandManager.literal("set").then(CommandManager.argument("pet", EntityArgumentType.player()).then(CommandManager.argument("owner", EntityArgumentType.player()).executes(ModCommands::setBond))));
        rootCommand.then(CommandManager.literal("clear").then(CommandManager.argument("target", EntityArgumentType.player()).executes(ModCommands::clearBond)));
        rootCommand.then(CommandManager.literal("staffint").then(CommandManager.argument("target", EntityArgumentType.player()).executes(ModCommands::openStaffInterfaceWithTarget)).executes(ModCommands::openStaffInterface));

        var xpCommand = CommandManager.literal("xp");
        xpCommand.then(CommandManager.literal("add").then(CommandManager.argument("target", EntityArgumentType.player()).then(CommandManager.argument("amount", IntegerArgumentType.integer(1)).executes(ModCommands::addBondXp))));
        xpCommand.then(CommandManager.literal("set").then(CommandManager.argument("target", EntityArgumentType.player()).then(CommandManager.argument("level", IntegerArgumentType.integer(1, 100)).executes(ModCommands::setBondLevel))));
        xpCommand.then(CommandManager.literal("points").then(CommandManager.argument("target", EntityArgumentType.player()).then(CommandManager.argument("amount", IntegerArgumentType.integer()).executes(ModCommands::addSkillPointsCommand))));

        rootCommand.then(xpCommand);
        dispatcher.register(rootCommand);
    }

    private static void syncToOwnerGui(ServerPlayerEntity pet, PlayerBondComponent bond) {
        try {
            if (bond.hasOwner() && bond.getOwnerUUID() != null && !bond.getOwnerUUID().isEmpty()) {
                ServerPlayerEntity owner = pet.getServer().getPlayerManager().getPlayer(java.util.UUID.fromString(bond.getOwnerUUID()));
                if (owner != null) {
                    Map<String, String> registered = ModComponents.PLAYER_BOND.get(owner).getRegisteredPets();
                    if (!registered.isEmpty()) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeInt(registered.size());
                        for (Map.Entry<String, String> entry : registered.entrySet()) {
                            ModPackets.writePetData(buf, UUID.fromString(entry.getKey()), entry.getValue(), owner.getServer());
                        }
                        ServerPlayNetworking.send(owner, ModPackets.OPEN_MANAGEMENT_GUI, buf);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static int addBondXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity pet = EntityArgumentType.getPlayer(context, "target");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (!bond.hasOwner()) { source.sendError(Text.translatable("text.bondofthebeast.command.no_owner", pet.getName().getString()).formatted(Formatting.RED)); return 0; }

        bond.addBondExperience(amount); ModComponents.PLAYER_BOND.sync(pet); syncToOwnerGui(pet, bond);
        source.sendFeedback(() -> Text.translatable("text.bondofthebeast.command.added_xp", amount, pet.getName().getString(), bond.getBondLevel()).formatted(Formatting.GREEN), true);
        pet.sendMessage(Text.translatable("text.bondofthebeast.command.received_xp", amount, bond.getBondLevel()).formatted(Formatting.GOLD), false);
        return 1;
    }

    private static int addSkillPointsCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity pet = EntityArgumentType.getPlayer(context, "target");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        var bond = ModComponents.PLAYER_BOND.get(pet);
        if (!bond.hasOwner()) { source.sendError(Text.translatable("text.bondofthebeast.command.no_owner", pet.getName().getString()).formatted(Formatting.RED)); return 0; }

        bond.addSkillPoints(amount); ModComponents.PLAYER_BOND.sync(pet); syncToOwnerGui(pet, bond);
        source.sendFeedback(() -> Text.translatable("text.bondofthebeast.command.added_sp", amount, pet.getName().getString(), bond.getSkillPoints()).formatted(Formatting.GREEN), true);
        pet.sendMessage(Text.translatable("text.bondofthebeast.command.received_sp", amount).formatted(Formatting.GOLD), false);
        return 1;
    }

    private static int setBondLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "target");
        int newLevel = IntegerArgumentType.getInteger(context, "level");

        var bond = ModComponents.PLAYER_BOND.get(targetPlayer);
        if (!bond.hasOwner()) { context.getSource().sendError(Text.translatable("text.bondofthebeast.command.no_owner", targetPlayer.getName().getString()).formatted(Formatting.RED)); return 0; }

        bond.setBondLevel(newLevel); bond.setBondExperience(0);
        if (newLevel == 1) { bond.setSkillPoints(0); bond.clearSkills(); }

        ModComponents.PLAYER_BOND.sync(targetPlayer); syncToOwnerGui(targetPlayer, bond);
        context.getSource().sendFeedback(() -> Text.translatable("text.bondofthebeast.command.set_level_owner", targetPlayer.getName().getString(), newLevel).formatted(Formatting.GREEN), true);
        targetPlayer.sendMessage(Text.translatable("text.bondofthebeast.command.set_level_pet", newLevel).formatted(Formatting.GOLD), false);
        return 1;
    }

    private static int openStaffInterfaceWithTarget(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer(); if (player == null) return 0;
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        PacketByteBuf buf = PacketByteBufs.create();
        ModPackets.writePetData(buf, target.getUuid(), target.getName().getString(), player.getServer());
        ServerPlayNetworking.send(player, ModPackets.OPEN_PET_STATS_GUI, buf);
        return 1;
    }

    private static int openStaffInterface(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer(); if (player == null) return 0;
        var bond = ModComponents.PLAYER_BOND.get(player);
        Map<String, String> registered = bond.getRegisteredPets();

        PacketByteBuf buf = PacketByteBufs.create();
        if (registered.size() == 1) {
            Map.Entry<String, String> singlePet = registered.entrySet().iterator().next();
            ModPackets.writePetData(buf, UUID.fromString(singlePet.getKey()), singlePet.getValue(), player.getServer());
            ServerPlayNetworking.send(player, ModPackets.OPEN_PET_STATS_GUI, buf);
        } else {
            buf.writeInt(registered.size());
            for (Map.Entry<String, String> entry : registered.entrySet()) {
                ModPackets.writePetData(buf, UUID.fromString(entry.getKey()), entry.getValue(), player.getServer());
            }
            ServerPlayNetworking.send(player, ModPackets.OPEN_MANAGEMENT_GUI, buf);
        }
        return 1;
    }

    private static int showDetailedInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        PlayerBondComponent targetBond = ModComponents.PLAYER_BOND.get(target);

        MutableText response = Text.literal("\n=== ").append(target.getName()).append(" ===\n").formatted(Formatting.GOLD);
        if (targetBond.hasOwner()) {
            response.append(Text.translatable("command.bondofthebeast.info.master").formatted(Formatting.WHITE)).append(Text.literal(targetBond.getOwnerName()).formatted(Formatting.AQUA)).append("\n");
            response.append(Text.translatable("command.bondofthebeast.info.level", targetBond.getBondLevel()).formatted(Formatting.WHITE)).append("\n");
            response.append(Text.translatable("command.bondofthebeast.info.experience", targetBond.getBondExperience(), (targetBond.getBondLevel() * 100)).formatted(Formatting.GRAY)).append("\n");
        } else response.append(Text.translatable("command.bondofthebeast.info.no_master").formatted(Formatting.GRAY)).append("\n");

        List<String> pets = new ArrayList<>();
        for (ServerPlayerEntity p : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            PlayerBondComponent pBond = ModComponents.PLAYER_BOND.get(p);
            if (pBond.hasOwner() && pBond.getOwnerUUID().equals(target.getUuidAsString())) pets.add(p.getName().getString() + " (" + pBond.getBondLevel() + ")");
        }
        if (!pets.isEmpty()) response.append(Text.translatable("command.bondofthebeast.info.pets").formatted(Formatting.WHITE)).append(Text.literal(String.join(", ", pets)).formatted(Formatting.YELLOW));
        else response.append(Text.translatable("command.bondofthebeast.info.no_pets").formatted(Formatting.GRAY));

        context.getSource().sendFeedback(() -> response, false);
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(target);

        MutableText status = Text.literal("\n").append(Text.translatable("command.bondofthebeast.status.title", target.getName().getString())).append("\n").formatted(Formatting.GREEN);
        String sittingKey = bond.isSitting() ? "gui.bondofthebeast.state_sitting" : "gui.bondofthebeast.state_walking";
        status.append(Text.translatable("command.bondofthebeast.status.is_sitting").formatted(Formatting.WHITE)).append(Text.translatable(sittingKey).formatted(bond.isSitting() ? Formatting.RED : Formatting.YELLOW)).append("\n");

        boolean tpEnabled = bond.isTeleportEnabled();
        status.append(Text.translatable("command.bondofthebeast.status.tp_enabled").formatted(Formatting.WHITE)).append(Text.translatable(tpEnabled ? "command.bondofthebeast.status.on" : "command.bondofthebeast.status.off").formatted(tpEnabled ? Formatting.GREEN : Formatting.RED)).append("\n");

        boolean protEnabled = bond.isProtectionMode();
        status.append(Text.translatable("command.bondofthebeast.status.protection").formatted(Formatting.WHITE)).append(Text.translatable(protEnabled ? "command.bondofthebeast.status.on" : "command.bondofthebeast.status.off").formatted(protEnabled ? Formatting.GREEN : Formatting.RED));

        context.getSource().sendFeedback(() -> status, false);
        return 1;
    }

    private static int setBond(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity pet = EntityArgumentType.getPlayer(context, "pet");
        ServerPlayerEntity owner = EntityArgumentType.getPlayer(context, "owner");

        ModComponents.PLAYER_BOND.get(pet).setOwner(owner.getUuidAsString(), owner.getName().getString());
        ModComponents.PLAYER_BOND.get(owner).addPetToRegistry(pet.getUuidAsString(), pet.getName().getString());
        ModComponents.PLAYER_BOND.sync(pet); ModComponents.PLAYER_BOND.sync(owner);

        context.getSource().sendFeedback(() -> Text.translatable("command.bondofthebeast.set.success", pet.getName().getString(), owner.getName().getString()), true);
        return 1;
    }

    private static int clearBond(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        var targetBond = ModComponents.PLAYER_BOND.get(target);

        boolean wasPet = false;
        boolean wasOwner = false;

        if (targetBond.hasOwner()) {
            wasPet = true;
            String ownerUuidStr = targetBond.getOwnerUUID();
            removeCollarAndAbsorb(target, targetBond);
            targetBond.clearOwner();
            ModComponents.PLAYER_BOND.sync(target);

            try {
                ServerPlayerEntity owner = context.getSource().getServer().getPlayerManager().getPlayer(java.util.UUID.fromString(ownerUuidStr));
                if (owner != null) {
                    var ownerBond = ModComponents.PLAYER_BOND.get(owner);
                    if (ownerBond.getRegisteredPets().containsKey(target.getUuidAsString())) {
                        ownerBond.removePetFromRegistry(target.getUuidAsString());
                        ModComponents.PLAYER_BOND.sync(owner);
                        owner.sendMessage(Text.translatable("text.bondofthebeast.command.bond_broken").formatted(Formatting.DARK_RED), false);
                    }
                }
            } catch (Exception ignored) {}

            context.getSource().sendFeedback(() -> Text.translatable("text.bondofthebeast.command.collar_removed_admin", target.getName().getString()).formatted(Formatting.GREEN), true);
        }

        Map<String, String> registeredPets = new java.util.HashMap<>(targetBond.getRegisteredPets());
        if (!registeredPets.isEmpty()) {
            wasOwner = true;
            for (String petUuidStr : registeredPets.keySet()) {
                try {
                    ServerPlayerEntity pet = context.getSource().getServer().getPlayerManager().getPlayer(java.util.UUID.fromString(petUuidStr));
                    if (pet != null) {
                        var petBond = ModComponents.PLAYER_BOND.get(pet);
                        if (petBond.hasOwner() && petBond.getOwnerUUID().equals(target.getUuidAsString())) {
                            removeCollarAndAbsorb(pet, petBond);
                            petBond.clearOwner();
                            ModComponents.PLAYER_BOND.sync(pet);
                            pet.sendMessage(Text.translatable("text.bondofthebeast.command.bond_broken").formatted(Formatting.DARK_RED), false);
                        }
                    }
                } catch (Exception ignored) {}
            }
            targetBond.getRegisteredPets().clear();
            ModComponents.PLAYER_BOND.sync(target);
            context.getSource().sendFeedback(() -> Text.translatable("text.bondofthebeast.command.all_bonds_broken", target.getName().getString()).formatted(Formatting.GREEN), true);
        }

        if (!wasPet && !wasOwner) {
            context.getSource().sendFeedback(() -> Text.translatable("text.bondofthebeast.command.no_bonds_player", target.getName().getString()).formatted(Formatting.YELLOW), false);
        }

        return 1;
    }

    private static void removeCollarAndAbsorb(ServerPlayerEntity pet, PlayerBondComponent bond) {
        if (bond.isAbsorbed()) { pet.changeGameMode(GameMode.SURVIVAL); bond.setAbsorbed(false); }
        bond.setBedPos(null);
        TrinketsApi.getTrinketComponent(pet).ifPresent(c -> {
            c.getInventory().values().forEach(g -> g.values().forEach(inv -> {
                for (int i = 0; i < inv.size(); i++) {
                    if (inv.getStack(i).getItem() instanceof CollarItem) {
                        ItemStack dropped = inv.getStack(i).copy();
                        if (dropped.hasNbt()) dropped.getNbt().remove("OwnerName");
                        pet.dropItem(dropped, true); inv.setStack(i, ItemStack.EMPTY);
                    }
                }
            }));
        });
    }
}