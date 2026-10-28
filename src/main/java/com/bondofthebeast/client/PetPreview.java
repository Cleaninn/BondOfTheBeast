package com.bondofthebeast.client;

import com.mojang.authlib.GameProfile;
import dev.emi.trinkets.api.TrinketsApi;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType;
import net.onixary.shapeShifterCurseFabric.player_animation.v3.AnimRegistries;
import net.onixary.shapeShifterCurseFabric.player_animation.v3.AnimSystem;
import net.onixary.shapeShifterCurseFabric.player_form.utils.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.skin.RegPlayerSkinComponent;

/** Isolated display entity: configure SSC appearance without changing the real player. */
final class PetPreview {
    private PetPreview() {}

    static OtherClientPlayerEntity create(MinecraftClient client, StaffMainScreen.PetData pet) {
        if (client.world == null) return null;
        String[] names = pet.name.split("\\|");
        var entity = new OtherClientPlayerEntity(client.world, new GameProfile(pet.uuid, names[names.length - 1]));
        var id = Identifier.tryParse(pet.formId);
        var form = id == null ? null : RegPlayerForms.playerForms.get(id);
        if (form != null) {
            // Restore the display appearance like a synchronized component. A fresh
            // preview has not activated a book, so normal form transitions are blocked.
            var appearance = new NbtCompound();
            var formComponent = RegPlayerFormComponent.PLAYER_FORM.get(entity);
            formComponent.writeToNbt(appearance);
            appearance.putString("nowFormID", form.getFormID().toString());
            formComponent.readFromNbt(appearance);
        }
        var live = client.world.getPlayerByUuid(pet.uuid);
        if (live != null) {
            for (var slot : EquipmentSlot.values()) entity.equipStack(slot, live.getEquippedStack(slot).copy());
            TrinketsApi.getTrinketComponent(live).ifPresent(source ->
                    TrinketsApi.getTrinketComponent(entity).ifPresent(target -> {
                        var accessories = new NbtCompound();
                        source.writeToNbt(accessories);
                        target.readFromNbt(accessories);
                    }));
            var skin = new NbtCompound();
            RegPlayerSkinComponent.SKIN_SETTINGS.get(live).writeToNbt(skin);
            RegPlayerSkinComponent.SKIN_SETTINGS.get(entity).readFromNbt(skin);
        }
        if (form != null) {
            // Display entities are outside the world tick loop. Install the form's idle
            // animation explicitly: feral geometry relies on it for the four-legged pose.
            var bondData = new NbtCompound();
            bondData.putBoolean("IsSitting", pet.isSitting);
            com.bondofthebeast.component.ModComponents.PLAYER_BOND.get(entity).readFromNbt(bondData);
            entity.setSneaking(pet.isSitting);
            entity.setPose(pet.isSitting ? net.minecraft.entity.EntityPose.CROUCHING : net.minecraft.entity.EntityPose.STANDING);
            entity.setOnGround(true);
            var data = new AnimSystem.AnimSystemData(entity);
            data.playerForm = form;
            var controller = form.getAnimStateController(entity, data, AnimRegistries.ANIM_STATE_IDLE);
            if (controller != null) {
                if (!controller.isRegistered(entity, data)) controller.registerAnim(entity, data);
                var holder = controller.getAnimation(entity, data);
                if (holder != null && holder.getAnimation() != null) {
                    PlayerAnimationAccess.getPlayerAnimLayer(entity).addAnimLayer(100,
                            new KeyframeAnimationPlayer(holder.getAnimation()));
                    tick(entity);
                }
            }
        }
        return entity;
    }

    static void tick(OtherClientPlayerEntity entity) {
        if (entity != null) PlayerAnimationAccess.getPlayerAnimLayer(entity).tick();
    }

    static boolean isFeral(OtherClientPlayerEntity entity) {
        return RegPlayerFormComponent.PLAYER_FORM.get(entity).nowForm.getBodyType() == PlayerFormBodyType.FERAL;
    }

    static void draw(DrawContext context, OtherClientPlayerEntity entity, int x, int y, int scale, float lookX, float lookY) {
        float yaw = isFeral(entity) ? 145 : 180;
        float turn = (float) Math.atan(lookX / 40f);
        float tilt = (float) Math.atan(lookY / 40f);
        entity.bodyYaw = yaw + turn * 20;
        entity.setYaw(yaw + turn * 40);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.headYaw;
        entity.setPitch(-tilt * 20);
        var camera = new org.joml.Quaternionf().rotationX(tilt * (float) Math.PI / 9);
        var rotation = new org.joml.Quaternionf().rotationZ((float) Math.PI).mul(camera);
        InventoryScreen.drawEntity(context, x, y, scale, rotation, camera, entity);
    }
}
