package com.bondofthebeast.client;

import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.onixary.shapeShifterCurseFabric.player_form.instinct.RegPlayerInstinctComponent;

public final class BondHud {
    private BondHud() {}
    private static int collaringTicks = -1;
    private static long collaringUpdated;
    private static int previousStage = Integer.MIN_VALUE;
    private static int previousEscape;
    private static boolean previousForced;
    private static boolean previousSigned;
    private static long noticeUntil;
    public static void reset() {
        collaringTicks = -1;
        previousStage = Integer.MIN_VALUE;
        previousEscape = 0;
        previousForced = false;
        previousSigned = false;
        noticeUntil = 0;
    }
    public static void updateCollaring(int ticks) {
        collaringTicks = ticks;
        collaringUpdated = net.minecraft.util.Util.getMeasuringTimeMs();
    }
    public static boolean isCollaringVisible() {
        return collaringTicks >= 0 && net.minecraft.util.Util.getMeasuringTimeMs() - collaringUpdated < 500;
    }
    public static void render(DrawContext context, float delta) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (isCollaringVisible()) {
            int barWidth = Math.min(150, client.getWindow().getScaledWidth() - 16);
            float progress = Math.min(1, collaringTicks / (float) ForcedTamingService.COLLARING_TICKS);
            context.fill(5, 5, 11 + barWidth, 19, 0xD02B1921);
            context.drawBorder(5, 5, barWidth + 6, 14, UiColors.CARD_BORDER);
            context.fill(8, 9, 8 + barWidth, 15, 0xFF403039);
            context.fill(8, 9, 8 + (int) (barWidth * progress), 15, UiColors.INSTINCT);
            return;
        }
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        int stage = ForcedTamingService.stage(client.player);
        var state = bond.getForcedBond();
        long now = net.minecraft.util.Util.getMeasuringTimeMs();
        boolean restrained = state.forced || com.bondofthebeast.CollarItem.hasForcedCollar(client.player);
        if (restrained && (!previousForced || stage != previousStage || state.escapeTicks > previousEscape || state.contractSigned != previousSigned))
            noticeUntil = now + 8000;
        previousForced = restrained;
        previousStage = stage;
        previousEscape = state.escapeTicks;
        previousSigned = state.contractSigned;
        boolean countdown = state.warningTicks >= 0 || state.finalWarningTicks >= 0;
        if (!countdown && (!restrained || now >= noticeUntil)) return;
        Text hint = state.finalWarningTicks >= 0 ? Text.translatable("text.bondofthebeast.final_countdown", (state.finalWarningTicks + 19) / 20) : state.escapeTicks > 0 ? Text.translatable("gui.bondofthebeast.escape_seconds", state.escapeTicks / 20) :
                state.warningTicks >= 0 ? Text.translatable("text.bondofthebeast.curse_countdown", (state.warningTicks + 19) / 20) :
                Text.translatable("gui.bondofthebeast.escape_rule." + (state.contractSigned ? "contract" : stage >= 2 ? "enchanted" : "apple"));
        int cooldown = ForcedTamingService.resistanceCooldownSeconds(client.player);
        if (cooldown > 0 && state.escapeTicks == 0 && !state.contractSigned)
            hint = hint.copy().append(Text.literal("\n")).append(Text.translatable("gui.bondofthebeast.resistance_cooldown", cooldown));
        int panelWidth = Math.min(240, client.getWindow().getScaledWidth() - 10);
        var stageLines = client.textRenderer.wrapLines(GrimoireStyle.bookText(Text.translatable(
                stage < 0 ? "gui.bondofthebeast.human_stage" : "gui.bondofthebeast.stage", stage)), panelWidth - 12);
        var hintLines = client.textRenderer.wrapLines(GrimoireStyle.bookText(hint), panelWidth - 12);
        int barY = 17 + stageLines.size() * 10 + hintLines.size() * 10;
        context.fill(5, 5, 5 + panelWidth, barY + 12, 0xD02B1921);
        context.drawBorder(5, 5, panelWidth, barY + 7, UiColors.CARD_BORDER);
        int textY = 10;
        for (var line : stageLines) {
            context.drawText(client.textRenderer, line, 11, textY, UiColors.ACCENT, false);
            textY += 10;
        }
        textY += 3;
        for (var line : hintLines) {
            context.drawText(client.textRenderer, line, 11, textY, 0xFFE0C080, false);
            textY += 10;
        }
        float instinct = RegPlayerInstinctComponent.PLAYER_INSTINCT_COMP.get(client.player).instinctValue;
        int barWidth = panelWidth - 12;
        context.fill(11, barY, 11 + barWidth, barY + 6, 0xFF403039);
        context.fill(11, barY, 11 + (int) (barWidth * Math.max(0, Math.min(1, instinct / 100))), barY + 6, UiColors.INSTINCT);
    }
}
