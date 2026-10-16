package com.bondofthebeast.client;

import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class BondHud {
    private BondHud() {}
    private static int collaringTicks = -1;
    private static boolean collaringVictim;
    private static long collaringUpdated;
    private static String previousOwner = "";
    private static long noticeUntil;
    private static Text stageTitle, stageBody;
    private static long stageNoticeUntil;
    public static void stageNotice(String pet, int stage, int seconds, boolean owner) {
        stageTitle = Text.translatable(owner ? "gui.bondofthebeast.stage_notice.owner" : "gui.bondofthebeast.stage_notice.pet", stage);
        stageBody = owner ? Text.translatable("gui.bondofthebeast.stage_notice.owner_body", pet, seconds) :
                Text.translatable("gui.bondofthebeast.stage_notice.pet_body", seconds);
        stageNoticeUntil = 0;
    }
    private static void renderStageNotice(DrawContext context, MinecraftClient client, long now) {
        if (stageTitle == null || client.currentScreen != null) return;
        if (stageNoticeUntil == 0) stageNoticeUntil = now + 8000;
        if (now > stageNoticeUntil) { stageTitle = null; return; }
        int width = Math.min(180, client.getWindow().getScaledWidth() - 20);
        var titleLines = client.textRenderer.wrapLines(GrimoireStyle.bookText(stageTitle), width - 16);
        var bodyLines = client.textRenderer.wrapLines(GrimoireStyle.bookText(stageBody), width - 16);
        int height = 19 + (titleLines.size() + bodyLines.size()) * 10;
        int x = client.getWindow().getScaledWidth() - width - 5;
        context.fill(x, 5, x + width, 5 + height, 0xEC30202A);
        context.drawBorder(x, 5, width, height, 0xFF9D7E58);
        context.fill(x + 4, 8, x + width - 4, 9, 0xFF71414C);
        int y = 13;
        for (var line : titleLines) { context.drawText(client.textRenderer, line, x + 8, y, 0xFFEBD29E, false); y += 10; }
        y += 3;
        for (var line : bodyLines) { context.drawText(client.textRenderer, line, x + 8, y, 0xFFCEBD9C, false); y += 10; }
    }

    public static void reset() {
        PetInstinctHud.reset();
        collaringTicks = -1;
        previousOwner = "";
        noticeUntil = 0;
        stageTitle = null;
    }

    public static void updateCollaring(int ticks, boolean victim) {
        collaringTicks = ticks;
        collaringVictim = victim;
        collaringUpdated = net.minecraft.util.Util.getMeasuringTimeMs();
    }

    public static boolean isCollaringVisible() {
        return collaringTicks >= 0 && net.minecraft.util.Util.getMeasuringTimeMs() - collaringUpdated < 500;
    }

    public static void render(DrawContext context, float delta) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        PetInstinctHud.render(context, client);
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        var state = bond.getForcedBond();
        long now = net.minecraft.util.Util.getMeasuringTimeMs();
        String owner = state.forced ? bond.getOwnerUUID() : "";
        if (!owner.isEmpty() && !owner.equals(previousOwner)) noticeUntil = now + 8000;
        previousOwner = owner;

        Text message;
        boolean fitting = isCollaringVisible();
        boolean curseWarning = state.warningTicks >= 0 &&
                (ForcedTamingService.infused(client.player) || state.catalystWarningFromCookie) &&
                !ForcedTamingService.freeWindow(client.player);
        if (!fitting && !curseWarning && state.finalWarningTicks < 0 && (owner.isEmpty() || now >= noticeUntil))
            renderStageNotice(context, client, now);
        if (fitting) message = Text.translatable(collaringVictim ?
                "gui.bondofthebeast.hud.collar_victim" : "gui.bondofthebeast.hud.collar_owner");
        else if (!owner.isEmpty() && now < noticeUntil) {
            message = Text.translatable("gui.bondofthebeast.hud.forced_bound", bond.getOwnerName());
            if (curseWarning) message = message.copy().append(Text.literal("\n"))
                    .append(Text.translatable("text.bondofthebeast.curse_countdown", (state.warningTicks + 19) / 20));
        }
        else if (state.finalWarningTicks >= 0) message = Text.translatable("text.bondofthebeast.final_countdown", (state.finalWarningTicks + 19) / 20);
        else if (curseWarning) message = Text.translatable("text.bondofthebeast.curse_countdown", (state.warningTicks + 19) / 20);
        else return;

        int panelWidth = Math.min(240, client.getWindow().getScaledWidth() - 10);
        var lines = client.textRenderer.wrapLines(GrimoireStyle.bookText(message), panelWidth - 12);
        int bottom = 14 + lines.size() * 10 + (fitting ? 10 : 0);
        context.fill(5, 5, 5 + panelWidth, bottom, 0xD02B1921);
        context.drawBorder(5, 5, panelWidth, bottom - 5, UiColors.CARD_BORDER);
        int y = 10;
        for (var line : lines) {
            context.drawText(client.textRenderer, line, 11, y, 0xFFEAD4A2, false);
            y += 10;
        }
        if (fitting) {
            int barWidth = panelWidth - 12;
            float progress = Math.max(0, Math.min(1, collaringTicks / (float) ForcedTamingService.COLLARING_TICKS));
            context.fill(11, y + 2, 11 + barWidth, y + 8, 0xFF403039);
            context.fill(11, y + 2, 11 + (int) (barWidth * progress), y + 8, UiColors.INSTINCT);
        }
    }
}
