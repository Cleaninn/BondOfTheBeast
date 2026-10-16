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
        int previousWarning = state.warningTicks, previousFinal = state.finalWarningTicks;
        if (!BondRules.hasOpenedBook(player) || !player.isAlive() || player.isCreative() || player.isSpectator() || ForcedTamingService.freeWindow(player)) {
            state.warningTicks = -1;
            state.finalWarningTicks = -1;
            syncWarningChanges(player, previousWarning, previousFinal);
            return;
        }
        int stage = ForcedTamingService.stage(player);
        if (stage < 0 && EffectManager.hasTransformativeEffect(player) && (active(player) || state.catalystWarningFromCookie)) {
            if (state.warningTicks < 0) state.warningTicks = ForcedTamingService.WARNING_TICKS;
            if (state.warningTicks > 0) {
                state.warningTicks--;
                if (state.warningTicks % 20 == 0) showCountdownPulse(player);
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
            syncWarningChanges(player, previousWarning, previousFinal);
            return;
        }
        if (state.finalWarningTicks < 0) {
            syncWarningChanges(player, previousWarning, previousFinal);
            return;
        }
        if (state.finalWarningTicks > 0) {
            state.finalWarningTicks--;
            if (state.finalWarningTicks % 20 == 0) showCountdownPulse(player);
        } else {
            var form = RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm();
            state.finalWarningTicks = -1;
            RegPlayerInstinctComponent.PLAYER_INSTINCT_COMP.get(player).instinctValue = 0;
            TransformManager.handleDirectTransform(player, form.getGroup().getForm(3), false);
        }
        syncWarningChanges(player, previousWarning, previousFinal);
    }

    public static void reservoirRemoved(ServerPlayerEntity player) {
        var state = ModComponents.PLAYER_BOND.get(player).getForcedBond();
        int previousWarning = state.warningTicks, previousFinal = state.finalWarningTicks;
        if (!state.catalystWarningFromCookie) state.warningTicks = -1;
        if (!state.forced) state.finalWarningTicks = -1;
        syncWarningChanges(player, previousWarning, previousFinal);
    }

    private static void syncWarningChanges(ServerPlayerEntity player, int previousWarning, int previousFinal) {
        var state = ModComponents.PLAYER_BOND.get(player).getForcedBond();
        if ((previousWarning >= 0) == (state.warningTicks >= 0) && (previousFinal >= 0) == (state.finalWarningTicks >= 0)) return;
        if ((previousWarning >= 0 && state.warningTicks < 0) || (previousFinal >= 0 && state.finalWarningTicks < 0))
            player.sendMessage(Text.empty(), true);
        ModComponents.PLAYER_BOND.sync(player);
    }

    private static void showCountdownPulse(ServerPlayerEntity player) {
        ModComponents.PLAYER_BOND.sync(player);
        player.getServerWorld().spawnParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(),
                6, 0.3, 0.5, 0.3, 0);
    }
}
