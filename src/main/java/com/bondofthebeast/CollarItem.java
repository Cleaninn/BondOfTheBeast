package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CollarItem extends TrinketItem {
    public CollarItem(Settings settings) {
        super(settings);
    }

    // НОВОЕ: Ошейник больше не выпадает при смерти!
    @Override
    public TrinketEnums.DropRule getDropRule(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return TrinketEnums.DropRule.KEEP;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerEntity targetPet)) return ActionResult.PASS;
        if (!BondRules.hasOpenedBook(targetPet)) return ActionResult.FAIL;
        if (BondRules.isAllay(targetPet) || (!ModComponents.PLAYER_BOND.get(targetPet).hasOwner() &&
                BondRules.protectedFromForcedTaming(targetPet))) return ActionResult.FAIL;

        World world = user.getWorld();
        if (!ModComponents.PLAYER_BOND.get(targetPet).hasOwner() && !ForcedTamingService.stunned(targetPet)) return ActionResult.PASS;
        if (world.isClient) {
            if (!ModComponents.PLAYER_BOND.get(targetPet).hasOwner() && ForcedTamingService.stunned(targetPet) && BondRules.canOwn(user))
                user.setCurrentHand(hand);
            return ActionResult.SUCCESS;
        }

        PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(targetPet);
        if (!bond.hasOwner() && targetPet instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            ActionResult forced = ForcedTamingService.start(stack, user, sp, hand);
            if (forced != ActionResult.PASS) return forced;
        }


        if (bond.hasOwner() && bond.getOwnerUUID().equals(user.getUuidAsString())) {
            if (equipToPet(targetPet, stack, user.getName().getString(), true)) {
                if (stack.hasCustomName()) {
                    bond.setPetNickname(stack.getName().getString());
                }

                if (!user.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            } else {
                user.sendMessage(Text.translatable("text.bondofthebeast.collar_no_slot").formatted(Formatting.RED), true);
                return ActionResult.CONSUME;
            }
        } else {
            user.sendMessage(Text.translatable("text.bondofthebeast.not_your_pet").formatted(Formatting.RED), true);
            return ActionResult.CONSUME;
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) { return 72000; }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // Repeated use packets during a hold must not equip the collar on the holder.
        if (ForcedTamingService.isCollaring(user) || (user.isUsingItem() && user.getActiveItem().getItem() instanceof CollarItem)) {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(user.getStackInHand(hand));
        }
        var target = world.getEntitiesByClass(PlayerEntity.class, user.getBoundingBox().expand(4),
                player -> player != user && ForcedTamingService.aimsAt(user, player)).stream()
                .min(java.util.Comparator.comparingDouble(user::squaredDistanceTo));
        if (target.isPresent()) {
            ActionResult result = useOnEntity(user.getStackInHand(hand), user, target.get(), hand);
            return result == ActionResult.FAIL ? TypedActionResult.fail(user.getStackInHand(hand)) :
                    TypedActionResult.consume(user.getStackInHand(hand));
        }
        if (!canSelfEquipFromAir()) return TypedActionResult.pass(user.getStackInHand(hand));
        return super.use(world, user, hand);
    }

    protected boolean canSelfEquipFromAir() { return true; }

    public static boolean isForcedCollar(ItemStack stack) {
        return stack.getItem() instanceof CollarItem && stack.hasNbt() && stack.getNbt().getBoolean("ForcedCollar");
    }

    public static boolean hasForcedCollar(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player).map(c -> c.isEquipped(CollarItem::isForcedCollar)).orElse(false);
    }

    public static void sealEquipped(PlayerEntity player) {
        TrinketsApi.getTrinketComponent(player).ifPresent(c -> {
            for (var entry : c.getEquipped(stack -> stack.getItem() instanceof CollarItem)) {
                entry.getRight().getOrCreateNbt().putBoolean("ForcedCollar", true);
                entry.getLeft().inventory().markDirty();
            }
        });
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && user instanceof PlayerEntity player) ForcedTamingService.cancelCollaring(player);
        super.onStoppedUsing(stack, world, user, remainingUseTicks);
    }

    public static boolean equipToPet(PlayerEntity pet, ItemStack collar, String ownerName, boolean replace) {
        var optionalComponent = TrinketsApi.getTrinketComponent(pet);
        if (optionalComponent.isPresent()) {
            var component = optionalComponent.get();
            for (var group : component.getInventory().values()) {
                for (Map.Entry<String, TrinketInventory> entry : group.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("necklace")) {
                        TrinketInventory inventory = entry.getValue();
                        for (int i = 0; i < inventory.size(); i++) {
                            if (inventory.getStack(i).isEmpty() || replace) {
                                ItemStack displaced = inventory.getStack(i).copy();
                                ItemStack personalizedCollar = collar.copy();
                                personalizedCollar.setCount(1);
                                if (isForcedCollar(displaced) || ModComponents.PLAYER_BOND.get(pet).getForcedBond().forced)
                                    personalizedCollar.getOrCreateNbt().putBoolean("ForcedCollar", true);
                                if (displaced.hasNbt()) displaced.getNbt().remove("ForcedCollar");
                                if (ownerName.isEmpty()) {
                                    if (personalizedCollar.hasNbt()) personalizedCollar.getNbt().remove("OwnerName");
                                } else personalizedCollar.getOrCreateNbt().putString("OwnerName", ownerName);
                                inventory.setStack(i, personalizedCollar);
                                if (!displaced.isEmpty()) pet.getInventory().offerOrDrop(displaced);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void personalizeEquipped(PlayerEntity pet, String ownerName) {
        TrinketsApi.getTrinketComponent(pet).ifPresent(component -> {
            for (var equipped : component.getEquipped(stack -> stack.getItem() instanceof CollarItem)) {
                equipped.getRight().getOrCreateNbt().putString("OwnerName", ownerName);
                equipped.getLeft().inventory().markDirty();
            }
        });
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);
        if (!entity.getWorld().isClient() && entity instanceof PlayerEntity player) {
            if (stack.hasCustomName()) {
                PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(player);
                bond.setPetNickname(stack.getName().getString());
            }
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onUnequip(stack, slot, entity);
        if (!entity.getWorld().isClient() && entity instanceof PlayerEntity player) {
            PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(player);
            bond.setPetNickname(null);
        }
    }

    @Override
    public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(player);
            return !bond.hasOwner() && !isForcedCollar(stack);
        }
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.bondofthebeast.collar_contract").formatted(Formatting.GRAY));
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("OwnerName")) {
            tooltip.add(Text.translatable("tooltip.bondofthebeast.collar_owner")
                    .append(Text.literal(nbt.getString("OwnerName")).formatted(Formatting.GOLD))
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.bondofthebeast.collar_sealed").formatted(Formatting.DARK_RED, Formatting.ITALIC));
        }
    }
}
