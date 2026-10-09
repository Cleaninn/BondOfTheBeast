package com.bondofthebeast;

import com.bondofthebeast.block.PetBedBlockEntity;
import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class LeashManager {
    public static final TagKey<net.minecraft.block.Block> ANCHORS = TagKey.of(
            RegistryKeys.BLOCK, new Identifier(BondOfTheBeast.MOD_ID, "leash_anchors"));
    private LeashManager() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (entity instanceof LeashKnotEntity knot) {
                if (world.isClient) return ActionResult.SUCCESS;
                boolean changed = false;
                boolean occupied = false;
                for (ServerPlayerEntity pet : world.getServer().getPlayerManager().getPlayerList()) {
                    var b = ModComponents.PLAYER_BOND.get(pet);
                    if (b.getLeashPos() != null &&
                            b.getLeashPos().equals(knot.getDecorationBlockPos()) &&
                            b.getLeashDimension().equals(world.getRegistryKey().getValue().toString())) {
                        occupied = true;
                        if (BondRules.owns(player, pet)) {
                            detach(pet);
                            changed = true;
                        }
                    }
                }
                if (changed || occupied) return ActionResult.SUCCESS;
            }
            if (!(entity instanceof ServerPlayerEntity pet)) {
                if (world.isClient && entity instanceof net.minecraft.entity.player.PlayerEntity &&
                        player.getStackInHand(hand).isOf(Items.LEAD)) return ActionResult.SUCCESS;
                return ActionResult.PASS;
            }
            if (!BondRules.owns(player, pet) || !BondRules.canOwn(player)) return ActionResult.PASS;
            var b = ModComponents.PLAYER_BOND.get(pet);
            if (player.isSneaking() && (b.getLeashPos() != null || !b.getLeashHolder().isEmpty())) {
                detach(pet);
                player.sendMessage(Text.translatable("text.bondofthebeast.leash_removed"), true);
                return ActionResult.SUCCESS;
            }
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.LEAD)) return ActionResult.PASS;
            if (!BondRules.canObey(pet) || !BondRules.hasCollar(pet) || b.isAbsorbed() || hasBedChain(pet)) {
                player.sendMessage(Text.translatable("text.bondofthebeast.leash_conflict"), true);
                return ActionResult.FAIL;
            }
            if (!b.getLeashHolder().isEmpty() || b.getLeashPos() != null) return ActionResult.SUCCESS;
            boolean paid = !player.isCreative();
            if (paid) stack.decrement(1);
            b.setLeash(player.getUuidAsString(), null, world.getRegistryKey().getValue().toString(), paid);
            b.setSitting(false);
            b.setTeleportEnabled(false);
            player.sendMessage(Text.translatable("text.bondofthebeast.leash_attached"), true);
            return ActionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.getBlockState(hit.getBlockPos()).isIn(ANCHORS)) return ActionResult.PASS;
            if (world.isClient) {
                boolean hasLead = world.getPlayers().stream().anyMatch(p -> {
                    var bond = ModComponents.PLAYER_BOND.get(p);
                    return player.getUuidAsString().equals(bond.getLeashHolder()) ||
                            (player.isSneaking() && hit.getBlockPos().equals(bond.getLeashPos()) && BondRules.owns(player, p));
                });
                return hasLead ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            if (!BondRules.canOwn(player)) return ActionResult.PASS;
            boolean changed = false;
            for (ServerPlayerEntity pet : world.getServer().getPlayerManager().getPlayerList()) {
                var b = ModComponents.PLAYER_BOND.get(pet);
                if (pet.getWorld() != world || !BondRules.owns(player, pet)) continue;
                if (player.getUuidAsString().equals(b.getLeashHolder())) {
                    b.setLeash("", hit.getBlockPos(), world.getRegistryKey().getValue().toString(), b.isLeashPaid());
                    changed = true;
                } else if (player.isSneaking() && hit.getBlockPos().equals(b.getLeashPos())) {
                    detach(pet);
                    changed = true;
                }
            }
            if (changed) {
                if (world.getBlockState(hit.getBlockPos()).isIn(BlockTags.FENCES))
                    LeashKnotEntity.getOrCreate(world, hit.getBlockPos()).onPlace();
                player.sendMessage(Text.translatable("text.bondofthebeast.leash_anchor"), true);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }

    public static boolean hasBedChain(ServerPlayerEntity pet) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        ServerWorld world = dimension(pet, b.getBedDimension());
        return BondRules.allows(pet, "sit") && b.getBedPos() != null && world != null &&
                world.getBlockEntity(b.getBedPos()) instanceof PetBedBlockEntity bed &&
                pet.getUuidAsString().equals(bed.getBoundPetUUID()) && bed.getChainRadius() > 0;
    }

    public static boolean isTethered(ServerPlayerEntity pet) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        return !b.getLeashHolder().isEmpty() || b.getLeashPos() != null || hasBedChain(pet);
    }

    public static ServerWorld dimension(ServerPlayerEntity pet, String id) {
        Identifier parsed = Identifier.tryParse(id);
        return parsed == null ? null : pet.getServer().getWorld(
                net.minecraft.registry.RegistryKey.of(RegistryKeys.WORLD, parsed));
    }

    public static void detach(ServerPlayerEntity pet) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        if (b.isLeashPaid()) pet.dropItem(new ItemStack(Items.LEAD), false);
        b.setLeash("", null, "", false);
    }

    public static void tick(ServerPlayerEntity pet) {
        var b = ModComponents.PLAYER_BOND.get(pet);
        if (!BondRules.canObey(pet) || !BondRules.hasCollar(pet) || !pet.isAlive()) {
            if (!b.getLeashHolder().isEmpty() || b.getLeashPos() != null) detach(pet);
            return;
        }
        Vec3d anchor = null;
        double radius = 8;
        if (!b.getLeashHolder().isEmpty()) {
            ServerPlayerEntity holder;
            try { holder = pet.getServer().getPlayerManager().getPlayer(java.util.UUID.fromString(b.getLeashHolder())); }
            catch (IllegalArgumentException e) { holder = null; }
            if (holder == null || !holder.isAlive() || holder.getWorld() != pet.getWorld() || !BondRules.owns(holder, pet)) {
                detach(pet);
                return;
            }
            anchor = holder.getPos();
        } else if (b.getLeashPos() != null) {
            ServerWorld world = dimension(pet, b.getLeashDimension());
            if (world != pet.getWorld() || !world.getBlockState(b.getLeashPos()).isIn(ANCHORS)) {
                detach(pet);
                return;
            }
            anchor = b.getLeashPos().toCenterPos();
        } else if (hasBedChain(pet)) {
            ServerWorld world = dimension(pet, b.getBedDimension());
            if (world != pet.getWorld()) {
                teleportSafely(pet, world, Vec3d.ofBottomCenter(b.getBedPos().up()));
                return;
            }
            var bed = (PetBedBlockEntity) world.getBlockEntity(b.getBedPos());
            radius = bed.getChainRadius();
            anchor = b.getBedPos().toCenterPos();
        }
        if (anchor == null || pet.isSleeping()) return;
        Vec3d delta = anchor.subtract(pet.getPos());
        double distance = delta.length();
        if (distance > radius) {
            Vec3d pull = delta.normalize().multiply(Math.min(0.7, (distance - radius) * 0.12));
            pet.addVelocity(pull.x, Math.max(-0.15, Math.min(0.15, pull.y)), pull.z);
            pet.velocityModified = true;
            if (distance > radius + 5) {
                Vec3d edge = anchor.subtract(delta.normalize().multiply(Math.max(1, radius - 1)));
                teleportSafely(pet, pet.getServerWorld(), edge);
            }
        }
    }

    public static boolean teleportSafely(ServerPlayerEntity pet, ServerWorld world, Vec3d target) {
        BlockPos origin = BlockPos.ofFloored(target);
        for (int dy = 0; dy <= 4; dy++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            BlockPos pos = origin.add(dx, dy, dz);
            Vec3d dest = Vec3d.ofBottomCenter(pos);
            if (!world.getWorldBorder().contains(pos) || !world.isChunkLoaded(pos) ||
                    world.getBlockState(pos.down()).getCollisionShape(world, pos.down()).isEmpty() ||
                    !world.getFluidState(pos).isEmpty() || !world.getFluidState(pos.up()).isEmpty() ||
                    world.getBlockState(pos.down()).isOf(net.minecraft.block.Blocks.MAGMA_BLOCK) ||
                    world.getBlockState(pos.down()).isOf(net.minecraft.block.Blocks.CAMPFIRE)) continue;
            if (!world.isSpaceEmpty(pet, pet.getBoundingBox().offset(dest.subtract(pet.getPos())))) continue;
            pet.teleport(world, dest.x, dest.y, dest.z, pet.getYaw(), pet.getPitch());
            pet.setVelocity(Vec3d.ZERO);
            pet.fallDistance = 0;
            return true;
        }
        return false;
    }
}
