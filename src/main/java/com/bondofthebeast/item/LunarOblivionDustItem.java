package com.bondofthebeast.item;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LunarOblivionDustItem extends Item {

    public LunarOblivionDustItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            Vec3d eyePos = user.getEyePos();
            Vec3d lookVec = user.getRotationVec(1.0F);
            Vec3d targetCenter = eyePos.add(lookVec.multiply(1.2));

            Box area = new Box(targetCenter.x - 0.75, user.getY() - 0.25, targetCenter.z - 0.75,
                    targetCenter.x + 0.75, user.getY() + 2.25, targetCenter.z + 0.75);

            serverWorld.spawnParticles(ParticleTypes.WHITE_ASH, targetCenter.x, targetCenter.y, targetCenter.z, 80, 0.5, 0.5, 0.5, 0.02);
            serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE, targetCenter.x, targetCenter.y, targetCenter.z, 40, 0.6, 0.6, 0.6, 0.03);
            serverWorld.spawnParticles(ParticleTypes.END_ROD, targetCenter.x, targetCenter.y, targetCenter.z, 6, 0.5, 0.5, 0.5, 0.01);

            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0F, 1.2F);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_POWDER_SNOW_STEP, SoundCategory.PLAYERS, 1.5F, 0.8F);

            for (PlayerEntity target : world.getEntitiesByClass(PlayerEntity.class, area, p -> p != user && !p.isCreative() && !p.isSpectator() && !com.bondofthebeast.ForcedTamingService.freeWindow(p) && user.canSee(p))) {
                // Оставляем только Замедление 99 (как маркер для миксина) и Слепоту
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, com.bondofthebeast.ForcedTamingService.STUN_TICKS, 99, false, true, true));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, com.bondofthebeast.ForcedTamingService.STUN_TICKS, 0, false, true, true));

                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.0F, 0.6F);
            }

            if (!user.isCreative()) {
                stack.decrement(1);
            }
            user.getItemCooldownManager().set(this, 60);
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
