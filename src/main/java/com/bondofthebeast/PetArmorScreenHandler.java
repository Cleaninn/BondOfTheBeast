package com.bondofthebeast;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PetArmorScreenHandler extends ScreenHandler {
    public static final ScreenHandlerType<PetArmorScreenHandler> TYPE = Registry.register(
            Registries.SCREEN_HANDLER, new Identifier(BondOfTheBeast.MOD_ID, "pet_armor"),
            new ExtendedScreenHandlerType<>(PetArmorScreenHandler::new));
    private final ServerPlayerEntity pet;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static void register() { }

    public PetArmorScreenHandler(int syncId, PlayerInventory ownerInventory, PacketByteBuf data) {
        this(syncId, ownerInventory, null, new SimpleInventory(4));
        data.readUuid();
    }

    private PetArmorScreenHandler(int syncId, PlayerInventory ownerInventory, ServerPlayerEntity pet, Inventory armorInventory) {
        super(TYPE, syncId);
        this.pet = pet;
        for (int i = 0; i < 4; i++) {
            EquipmentSlot equipment = ARMOR[i];
            int inventoryIndex = pet == null ? i : 39 - i;
            addSlot(new Slot(armorInventory, inventoryIndex, 44 + i * 24, 32) {
                @Override public int getMaxItemCount() { return 1; }
                @Override public boolean canInsert(ItemStack stack) {
                    return LivingEntity.getPreferredEquipmentSlot(stack) == equipment;
                }
                @Override public boolean canTakeItems(PlayerEntity player) {
                    return canUse(player) && (player.isCreative() || !net.minecraft.enchantment.EnchantmentHelper.hasBindingCurse(getStack()));
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(ownerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(ownerInventory, col, 8 + col * 18, 142));
    }

    @Override public boolean canUse(PlayerEntity owner) {
        return pet == null || (pet.isAlive() && pet.getWorld() == owner.getWorld() && owner.squaredDistanceTo(pet) <= 64 &&
                BondRules.canOwn(owner) && BondRules.owns(owner, pet) && BondRules.allows(pet, "armor"));
    }

    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (!canUse(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasStack() || !slot.canTakeItems(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        if (index < 4) {
            if (!insertItem(stack, 4, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < 4; i++) if (!slots.get(i).hasStack() && slots.get(i).canInsert(stack)) {
                moved = insertItem(stack, i, i + 1, false);
                break;
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        slot.onTakeItem(player, stack);
        return original;
    }

    public static void open(ServerPlayerEntity owner, ServerPlayerEntity pet) {
        if (owner.getWorld() != pet.getWorld() || owner.squaredDistanceTo(pet) > 64) return;
        owner.openHandledScreen(new ExtendedScreenHandlerFactory() {
            @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeUuid(pet.getUuid()); }
            @Override public Text getDisplayName() { return Text.translatable("gui.bondofthebeast.armor_title", pet.getName()); }
            @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
                return new PetArmorScreenHandler(syncId, inventory, pet, pet.getInventory());
            }
        });
    }
}
