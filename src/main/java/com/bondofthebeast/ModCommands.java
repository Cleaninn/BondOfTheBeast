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

        dispatcher.register(rootCommand);
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
        buf.writeInt(registered.size());
        for (Map.Entry<String, String> entry : registered.entrySet())
            ModPackets.writePetData(buf, UUID.fromString(entry.getKey()), entry.getValue(), player.getServer());
        ServerPlayNetworking.send(player, ModPackets.OPEN_MANAGEMENT_GUI, buf);
        return 1;
    }

    private static int showDetailedInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        PlayerBondComponent targetBond = ModComponents.PLAYER_BOND.get(target);

        MutableText response = Text.literal("\n=== ").append(target.getName()).append(" ===\n").formatted(Formatting.GOLD);
        if (targetBond.hasOwner()) {
            response.append(Text.translatable("command.bondofthebeast.info.master").formatted(Formatting.WHITE)).append(Text.literal(targetBond.getOwnerName()).formatted(Formatting.AQUA)).append("\n");
            response.append(Text.translatable(targetBond.getForcedBond().forced ? "gui.bondofthebeast.stage" : "gui.bondofthebeast.trust." + targetBond.getVoluntaryBond().tier, ForcedTamingService.stage(target)).formatted(Formatting.WHITE)).append("\n");
        } else response.append(Text.translatable("command.bondofthebeast.info.no_master").formatted(Formatting.GRAY)).append("\n");

        List<String> pets = new ArrayList<>();
        for (ServerPlayerEntity p : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            PlayerBondComponent pBond = ModComponents.PLAYER_BOND.get(p);
            if (pBond.hasOwner() && pBond.getOwnerUUID().equals(target.getUuidAsString())) pets.add(p.getName().getString() + " (" + (pBond.getForcedBond().forced ? ForcedTamingService.stage(p) : pBond.getVoluntaryBond().tier) + ")");
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

        if (pet == owner) return 0;
        BondService.release(pet);
        ModComponents.PLAYER_BOND.get(pet).setOwner(owner.getUuidAsString(), owner.getName().getString());
        ModComponents.PLAYER_BOND.get(owner).addPetToRegistry(pet.getUuidAsString(), pet.getName().getString());
        ModComponents.PLAYER_BOND.sync(pet); ModComponents.PLAYER_BOND.sync(owner);

        context.getSource().sendFeedback(() -> Text.translatable("command.bondofthebeast.set.success", pet.getName().getString(), owner.getName().getString()), true);
        return 1;
    }

    private static int clearBond(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        var bond = ModComponents.PLAYER_BOND.get(target);
        for (String id : new java.util.ArrayList<>(bond.getRegisteredPets().keySet()))
            BondService.release(target.getServer(), id, target.getUuidAsString());
        BondService.release(target);
        context.getSource().sendFeedback(() -> Text.translatable("text.bondofthebeast.command.all_bonds_broken", target.getName().getString()), true);
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
