package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.instinct.RegPlayerInstinctComponent;
import net.onixary.shapeShifterCurseFabric.player_form.transform.TransformManager;
import net.onixary.shapeShifterCurseFabric.status_effects.attachment.EffectManager;

/** The reservoir changes the wearer's form independently of ownership or bond type. */
public final class CollarCatalystService {
    private CollarCatalystService() {}

    public static boolean active(PlayerEntity player) {
        return BondRules.hasOpenedBook(player) && player.isAlive() && !player.isCreative() && !player.isSpectator() &&
                ForcedTamingService.infused(player) && !ForcedTamingService.freeWindow(player);
    }

    public static boolean gradualFinalStage(PlayerEntity player) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm();
        return form != null && form.getIndex() == 2 && form.getGroup() != null && form.getGroup().hasForm(3) &&
                (ModComponents.PLAYER_BOND.get(player).getForcedBond().forced || active(player));
    }

    public static void tick(ServerPlayerEntity player) {
        var state = ModComponents.PLAYER_BOND.get(player).getForcedBond();
        if (!BondRules.hasOpenedBook(player) || !player.isAlive() || player.isCreative() || player.isSpectator() || ForcedTamingService.freeWindow(player)) return;
        int stage = ForcedTamingService.stage(player);
        if (stage < 0 && EffectManager.hasTransformativeEffect(player) && (active(player) || state.catalystWarningFromCookie)) {
            if (state.warningTicks < 0) state.warningTicks = ForcedTamingService.WARNING_TICKS;
            if (state.warningTicks > 0) {
                state.warningTicks--;
                if (state.warningTicks % 20 == 0) warn(player, "text.bondofthebeast.curse_countdown", state.warningTicks);
            } else {
                EffectManager.ActiveTransformativeEffect(player);
                state.warningTicks = -1;
                state.catalystWarningFromCookie = false;
            }
        } else {
            state.warningTicks = -1;
            state.catalystWarningFromCookie = false;
        }
        if (!gradualFinalStage(player) || RegPlayerInstinctComponent.PLAYER_INSTINCT_COMP.get(player).instinctValue < 100) {
            state.finalWarningTicks = -1;
            return;
        }
        if (state.finalWarningTicks < 0) return;
        if (state.finalWarningTicks > 0) {
            state.finalWarningTicks--;
            if (state.finalWarningTicks % 20 == 0) warn(player, "text.bondofthebeast.final_countdown", state.finalWarningTicks);
        } else {
            var form = RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm();
            state.finalWarningTicks = -1;
            RegPlayerInstinctComponent.PLAYER_INSTINCT_COMP.get(player).instinctValue = 0;
            TransformManager.handleDirectTransform(player, form.getGroup().getForm(3), false);
        }
    }

    private static void warn(ServerPlayerEntity player, String key, int ticks) {
        player.sendMessage(Text.translatable(key, (ticks + 19) / 20), true);
        player.getServerWorld().spawnParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(),
                6, 0.3, 0.5, 0.3, 0);
    }
}
