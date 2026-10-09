package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

public class CommandScepterItem extends Item {
    public CommandScepterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            if (!BondRules.canOwn(user)) {
                user.sendMessage(Text.translatable("text.bondofthebeast.owner_too_wild_to_command").formatted(Formatting.RED), true);
                return TypedActionResult.fail(user.getStackInHand(hand));
            }

            Map<String, String> registeredPets = ModComponents.PLAYER_BOND.get(serverPlayer).getRegisteredPets();
            if (registeredPets.isEmpty()) {
                user.sendMessage(Text.translatable("text.bondofthebeast.no_pets_owned").formatted(Formatting.RED), true);
                return TypedActionResult.fail(user.getStackInHand(hand));
            }

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(registeredPets.size());
            for (Map.Entry<String, String> entry : registeredPets.entrySet()) {
                ModPackets.writePetData(buf, UUID.fromString(entry.getKey()), entry.getValue(), serverPlayer.getServer());
            }
            ServerPlayNetworking.send(serverPlayer, ModPackets.OPEN_MANAGEMENT_GUI, buf);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
