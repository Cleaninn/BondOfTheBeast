package com.bondofthebeast.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PetBedBlockEntity extends BlockEntity {
    private String boundPetUUID = "";
    private int chainRadius = 0;
    private boolean isWaitingForPet = false;

    public PetBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PET_BED_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, PetBedBlockEntity be) {
        if (!world.isClient && be.isWaitingForPet && world.getTime() % 40 == 0) {
            try {
                PlayerEntity pet = world.getPlayerByUuid(UUID.fromString(be.boundPetUUID));
                if (pet instanceof ServerPlayerEntity sp) {
                    var bond = com.bondofthebeast.component.ModComponents.PLAYER_BOND.get(sp);

                    if (bond.hasOwner() && (bond.getBedPos() == null || (bond.getBedPos().equals(pos) && bond.getBedDimension().equals(world.getRegistryKey().getValue().toString())))) {
                        BlockPos currentBed = bond.getBedPos();

                        // Проверяем, не была ли эта лежанка уже привязана через пакет из меню
                        if (currentBed == null || !currentBed.equals(pos)) {
                            bond.setBedPos(pos);
                            sp.setSpawnPoint(world.getRegistryKey(), pos.up(), 0.0f, true, true);
                            sp.sendMessage(net.minecraft.text.Text.translatable("text.bondofthebeast.packet.bound_to_new_bed").formatted(net.minecraft.util.Formatting.GOLD), false);
                        }
                    }

                    be.isWaitingForPet = false;
                    be.markDirty();
                }
            } catch (Exception ignored) {}
        }

    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.boundPetUUID = nbt.getString("BoundPetUUID");
        this.chainRadius = Math.max(0, Math.min(50, nbt.getInt("ChainRadius")));
        if (nbt.contains("IsWaitingForPet")) {
            this.isWaitingForPet = nbt.getBoolean("IsWaitingForPet");
        } else {
            this.isWaitingForPet = !this.boundPetUUID.isEmpty();
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("BoundPetUUID", this.boundPetUUID);
        nbt.putInt("ChainRadius", this.chainRadius);
        nbt.putBoolean("IsWaitingForPet", this.isWaitingForPet);
    }

    @Nullable @Override public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
    @Override public NbtCompound toInitialChunkDataNbt() { return createNbt(); }

    public String getBoundPetUUID() { return boundPetUUID; }

    public void setBoundPetUUID(String uuid) {
        this.boundPetUUID = uuid;
        this.isWaitingForPet = !uuid.isEmpty();
        markDirty();
        world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    public int getChainRadius() { return chainRadius; }
    public void setChainRadius(int radius) { this.chainRadius = Math.max(0, Math.min(50, radius)); markDirty(); world.updateListeners(pos, getCachedState(), getCachedState(), 3); }
}