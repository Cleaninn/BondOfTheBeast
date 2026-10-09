package com.bondofthebeast.client;

import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.onixary.shapeShifterCurseFabric.player_form.instinct.RegPlayerInstinctComponent;

public final class BondHud {
    private BondHud() {}
    public static void render(DrawContext context, float delta) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.player.isUsingItem() && client.player.getActiveItem().getItem() instanceof com.bondofthebeast.CollarItem) {
            int panelWidth = Math.min(220, client.getWindow().getScaledWidth() - 10);
            var title = GrimoireStyle.bookText(Text.translatable("gui.bondofthebeast.collaring"));
            context.fill(5, 5, 5 + panelWidth, 35, 0xD02B1921);
            context.drawBorder(5, 5, panelWidth, 30, UiColors.CARD_BORDER);
            GrimoireStyle.fittedText(context, client.textRenderer, title, 5 + panelWidth / 2, 10, panelWidth - 12, 0xFFE0C080);
            int barWidth = panelWidth - 12;
            float progress = Math.min(1, client.player.getItemUseTime() / (float) ForcedTamingService.COLLARING_TICKS);
            context.fill(11, 24, 11 + barWidth, 29, 0xFF403039);
            context.fill(11, 24, 11 + (int) (barWidth * progress), 29, UiColors.INSTINCT);
            return;
        }
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        if (!bond.hasOwner() || !bond.getForcedBond().forced) return;
        int stage = ForcedTamingService.stage(client.player);
        var state = bond.getForcedBond();
        Text hint = state.finalWarningTicks >= 0 ? Text.translatable("text.bondofthebeast.final_countdown", (state.finalWarningTicks + 19) / 20) : state.escapeTicks > 0 ? Text.translatable("gui.bondofthebeast.escape_seconds", state.escapeTicks / 20) :
                state.warningTicks >= 0 ? Text.translatable("text.bondofthebeast.curse_countdown", (state.warningTicks + 19) / 20) :
                Text.translatable("gui.bondofthebeast.escape_rule." + (stage >= 3 ? "contract" : stage == 2 ? "enchanted" : "apple"));
        int cooldown = ForcedTamingService.resistanceCooldownSeconds(client.player);
        if (cooldown > 0 && state.escapeTicks == 0 && stage < 3)
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
