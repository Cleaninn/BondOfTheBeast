package com.bondofthebeast.client;

import com.bondofthebeast.block.PetBedBlockEntity;
import com.bondofthebeast.component.ModComponents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;

/** Renders actual leash geometry independently of SSC's player renderer. */
public final class PetLeashRenderer {
    private PetLeashRenderer() {}
    public static void render(WorldRenderContext context) {
        var world = MinecraftClient.getInstance().world;
        if (world == null || context.consumers() == null) return;
        float delta = context.tickDelta();
        String dimension = world.getRegistryKey().getValue().toString();
        for (PlayerEntity pet : world.getPlayers()) {
            var bond = ModComponents.PLAYER_BOND.get(pet);
            // The server validates collars before maintaining any tether. Remote
            // Trinkets inventory can arrive later than the synchronized bond.
            if (!bond.hasOwner()) continue;
            Vec3d anchor = null;
            boolean chain = false;
            if (!bond.getLeashHolder().isEmpty()) {
                try {
                    PlayerEntity holder = world.getPlayerByUuid(java.util.UUID.fromString(bond.getLeashHolder()));
                    if (holder != null) anchor = holder.getLeashPos(delta);
                } catch (IllegalArgumentException ignored) {}
            } else if (bond.getLeashPos() != null && dimension.equals(bond.getLeashDimension())) {
                anchor = bond.getLeashPos().toCenterPos().add(0, 0.1, 0);
            } else if (bond.getBedPos() != null && dimension.equals(bond.getBedDimension()) &&
                    world.getBlockEntity(bond.getBedPos()) instanceof PetBedBlockEntity bed &&
                    pet.getUuidAsString().equals(bed.getBoundPetUUID()) && bed.getChainRadius() > 0) {
                anchor = bond.getBedPos().toCenterPos().add(0, -0.25, 0);
                chain = true;
            }
            if (anchor == null) continue;
            Vec3d start = pet.getLerpedPos(delta).add(0, pet.getHeight() * 0.7, 0);
            Vec3d offset = anchor.subtract(start);
            if (offset.lengthSquared() > 256 * 256) continue;
            var matrices = context.matrixStack();
            matrices.push();
            Vec3d relative = start.subtract(context.camera().getPos());
            matrices.translate(relative.x, relative.y, relative.z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            VertexConsumer vertices = context.consumers().getBuffer(RenderLayer.getLeash());
            int light = LightmapTextureManager.pack(world.getLightLevel(LightType.BLOCK, pet.getBlockPos()),
                    world.getLightLevel(LightType.SKY, pet.getBlockPos()));
            float dx = (float) offset.x, dy = (float) offset.y, dz = (float) offset.z;
            float horizontal = MathHelper.sqrt(dx * dx + dz * dz);
            float sideX = horizontal < 0.001f ? 0.02f : dz / horizontal * 0.02f;
            float sideZ = horizontal < 0.001f ? 0 : -dx / horizontal * 0.02f;
            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i <= 32; i++) {
                    int step = pass == 0 ? i : 32 - i;
                    float t = step / 32f;
                    float sag = (float) Math.min(0.8, offset.length() * 0.06) * 4 * t * (1 - t);
                    float x = dx * t, y = dy * t - sag, z = dz * t;
                    float shade = (step % 2 == 0) ? 0.85f : 0.65f;
                    float r = chain ? 0.65f : 0.5f, g = chain ? 0.67f : 0.4f, b = chain ? 0.7f : 0.3f;
                    if (i == 0) vertices.vertex(matrix, x - sideX, y + (pass == 0 ? 0 : 0.025f), z - sideZ)
                            .color(r * shade, g * shade, b * shade, 1).light(light).next();
                    vertices.vertex(matrix, x - sideX, y + (pass == 0 ? 0 : 0.025f), z - sideZ)
                            .color(r * shade, g * shade, b * shade, 1).light(light).next();
                    vertices.vertex(matrix, x + sideX, y + (pass == 0 ? 0.025f : 0), z + sideZ)
                            .color(r * shade, g * shade, b * shade, 1).light(light).next();
                    if (i == 32) vertices.vertex(matrix, x + sideX, y + (pass == 0 ? 0.025f : 0), z + sideZ)
                            .color(r * shade, g * shade, b * shade, 1).light(light).next();
                }
            }
            matrices.pop();
        }
    }
}
