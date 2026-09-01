package com.bondofthebeast.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class PetBedBlockEntity extends BlockEntity {
    private String boundPetUUID = "";
    private int chainRadius = 0;

    public PetBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PET_BED_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, PetBedBlockEntity be) {
        if (world.isClient && !be.boundPetUUID.isEmpty() && be.chainRadius > 0) {
            try {
                UUID petUUID = UUID.fromString(be.boundPetUUID);
                PlayerEntity pet = world.getPlayerByUuid(petUUID);

                if (pet != null) {
                    boolean isWildEnough = true;
                    try {
                        var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(pet);
                        if (sscComp != null && sscComp.getCurrentForm() != null) {
                            if (sscComp.getCurrentForm().FormID != null && sscComp.getCurrentForm().FormID.getPath().toLowerCase().contains("allay")) {
                                isWildEnough = false;
                            } else {
                                int index = sscComp.getCurrentForm().getIndex();
                                boolean isFeral = sscComp.getCurrentForm().getBodyType() == net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType.FERAL;
                                isWildEnough = index >= 3 || isFeral;
                            }
                        }
                    } catch (Exception ignored) {}

                    if (!isWildEnough) return;

                    Vec3d bedPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5);
                    Vec3d petPos = pet.getPos().add(0, pet.getHeight() / 2.0, 0);
                    Vec3d vector = bedPos.subtract(petPos);
                    double distance = vector.length();

                    if (distance > be.chainRadius) {
                        Vec3d step = vector.normalize().multiply(0.5);
                        for (double d = 0; d < distance; d += 0.5) {
                            Vec3d particlePos = petPos.add(step.multiply(d));
                            world.addParticle(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.boundPetUUID = nbt.getString("BoundPetUUID");
        this.chainRadius = nbt.getInt("ChainRadius");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("BoundPetUUID", this.boundPetUUID);
        nbt.putInt("ChainRadius", this.chainRadius);
    }

    @Nullable @Override public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
    @Override public NbtCompound toInitialChunkDataNbt() { return createNbt(); }
    public String getBoundPetUUID() { return boundPetUUID; }
    public void setBoundPetUUID(String uuid) { this.boundPetUUID = uuid; markDirty(); world.updateListeners(pos, getCachedState(), getCachedState(), 3); }
    public int getChainRadius() { return chainRadius; }
    public void setChainRadius(int radius) { this.chainRadius = radius; markDirty(); world.updateListeners(pos, getCachedState(), getCachedState(), 3); }
}