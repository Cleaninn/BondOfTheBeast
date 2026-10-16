package com.bondofthebeast.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PetFeedingVisual {
    private static final int DURATION = 24;
    private static final Map<UUID, Meal> MEALS = new HashMap<>();

    private PetFeedingVisual() {}

    private static final class Meal {
        final ItemStack food;
        int age;

        Meal(ItemStack food) { this.food = food.copyWithCount(1); }
    }

    public static void play(UUID pet, ItemStack food) {
        if (!food.isEmpty()) MEALS.put(pet, new Meal(food));
    }

    public static void reset() { MEALS.clear(); }

    public static void tick(MinecraftClient client) {
        if (client.world == null) { reset(); return; }
        var iterator = MEALS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            PlayerEntity pet = client.world.getPlayerByUuid(entry.getKey());
            Meal meal = entry.getValue();
            if (pet == null || !pet.isAlive() || meal.age >= DURATION) { iterator.remove(); continue; }
            Vec3d mouth = mouth(pet, 1);
            if (meal.age % 6 == 0) {
                var random = pet.getRandom();
                client.world.playSound(mouth.x, mouth.y, mouth.z, SoundEvents.ENTITY_GENERIC_EAT,
                        SoundCategory.PLAYERS, 0.65f, 0.95f + random.nextFloat() * 0.2f, false);
                for (int i = 0; i < 6; i++) {
                    client.world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, meal.food),
                            mouth.x + (random.nextDouble() - 0.5) * 0.12,
                            mouth.y + (random.nextDouble() - 0.5) * 0.08,
                            mouth.z + (random.nextDouble() - 0.5) * 0.12,
                            (random.nextDouble() - 0.5) * 0.06, 0.025 + random.nextDouble() * 0.025,
                            (random.nextDouble() - 0.5) * 0.06);
                }
            }
            if (meal.age == DURATION - 2) {
                client.world.playSound(mouth.x, mouth.y, mouth.z, SoundEvents.ENTITY_PLAYER_BURP,
                        SoundCategory.PLAYERS, 0.35f, 1.1f, false);
            }
            meal.age++;
        }
    }

    private static Vec3d mouth(PlayerEntity pet, float delta) {
        float yaw = MathHelper.lerpAngleDegrees(delta, pet.prevYaw, pet.getYaw()) * MathHelper.RADIANS_PER_DEGREE;
        double reach = MathHelper.clamp(pet.getWidth() * 0.55, 0.22, 0.5);
        return pet.getLerpedPos(delta).add(-MathHelper.sin(yaw) * reach,
                pet.getEyeHeight(pet.getPose()) - Math.min(0.16, pet.getHeight() * 0.09), MathHelper.cos(yaw) * reach);
    }

    public static void render(WorldRenderContext context) {
        var client = MinecraftClient.getInstance();
        if (client.world == null || context.consumers() == null) return;
        var matrices = context.matrixStack();
        for (var entry : MEALS.entrySet()) {
            PlayerEntity pet = client.world.getPlayerByUuid(entry.getKey());
            if (pet == null || !pet.isAlive() || pet.isInvisible()) continue;
            Meal meal = entry.getValue();
            float age = meal.age + context.tickDelta();
            if (age >= DURATION - 2) continue;
            Vec3d position = mouth(pet, context.tickDelta()).subtract(context.camera().getPos());
            float size = MathHelper.clamp(pet.getHeight() * 0.18f, 0.18f, 0.3f) * (1 - meal.age / 6 * 0.18f);
            matrices.push();
            matrices.translate(position.x, position.y + MathHelper.sin(age * 1.4f) * 0.025, position.z);
            matrices.multiply(context.camera().getRotation());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(age * 1.4f) * 12));
            matrices.scale(size, size, size);
            client.getItemRenderer().renderItem(meal.food, ModelTransformationMode.FIXED,
                    WorldRenderer.getLightmapCoordinates(client.world, pet.getBlockPos().up()), OverlayTexture.DEFAULT_UV,
                    matrices, context.consumers(), client.world, pet.getId());
            matrices.pop();
        }
    }
}
