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

    public static void reset() {
        collaringTicks = -1;
        previousOwner = "";
        noticeUntil = 0;
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
