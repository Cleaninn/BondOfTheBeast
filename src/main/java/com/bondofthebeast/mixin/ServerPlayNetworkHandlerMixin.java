package com.bondofthebeast.mixin;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void lockSittingPosition(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (ModComponents.PLAYER_BOND.get(this.player).isSitting()) {
            // Проверяем, изменились ли координаты X, Y или Z
            if (packet.changesPosition()) {
                double dx = Math.abs(packet.getX(this.player.getX()) - this.player.getX());
                double dz = Math.abs(packet.getZ(this.player.getZ()) - this.player.getZ());

                // Если игрок сдвинулся больше чем на мизерное расстояние
                if (dx > 0.01 || dz > 0.01) {
                    this.player.requestTeleport(this.player.getX(), this.player.getY(), this.player.getZ());
                    // Мы не отменяем пакет (ci.cancel()), а просто возвращаем игрока.
                    // Это предотвратит kick за "Invalid movement".
                }
            }
        }
    }
}