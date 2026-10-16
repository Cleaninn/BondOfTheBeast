package com.bondofthebeast.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Util;

import java.util.List;
import java.util.UUID;

/** Owner-only server snapshots also cover pets outside the entity tracking distance. */
public final class PetInstinctHud {
    private static List<Pet> pets = List.of();
    private static long updatedAt;

    private PetInstinctHud() {}
    public record Pet(UUID uuid, String name, float instinct) {}
    public static void update(List<Pet> value) { pets = List.copyOf(value); updatedAt = Util.getMeasuringTimeMs(); }
    public static void reset() { pets = List.of(); updatedAt = 0; }

    public static void render(DrawContext context, MinecraftClient client) {
        if (pets.isEmpty() || client.currentScreen != null || client.player.isSpectator() || Util.getMeasuringTimeMs() - updatedAt > 2500) return;
        int screenWidth = client.getWindow().getScaledWidth(), screenHeight = client.getWindow().getScaledHeight();
        int room = screenWidth / 2 - 104;
        int cardWidth = Math.min(128, Math.max(100, room - 5));
        // Narrow windows put the cards above the hotbar instead of covering its slots.
        int bottom = room >= 105 ? screenHeight - 5 : screenHeight - 64;
        int rows = Math.max(1, (bottom - 42) / 30);
        for (int i = 0; i < Math.min(pets.size(), rows); i++) {
            Pet pet = pets.get(i);
            int x = 5, y = bottom - 27 - i * 30;
            context.fill(x, y, x + cardWidth, y + 27, 0xCE21191D);
            context.drawBorder(x, y, cardWidth, 27, 0xFF755A49);
            var entry = client.getNetworkHandler().getPlayerListEntry(pet.uuid);
            var skin = entry == null ? DefaultSkinHelper.getTexture(pet.uuid) : entry.getSkinTexture();
            context.fill(x + 3, y + 5, x + 21, y + 23, 0xFF433039);
            context.drawTexture(skin, x + 4, y + 6, 16, 16, 8, 8, 8, 8, 64, 64);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            context.drawTexture(skin, x + 4, y + 6, 16, 16, 40, 8, 8, 8, 64, 64);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            String name = client.textRenderer.trimToWidth(pet.name, cardWidth - 29);
            context.drawText(client.textRenderer, name, x + 25, y + 4, 0xFFE9D6B4, false);
            int barX = x + 25, barY = y + 16, barWidth = cardWidth - 31;
            float value = Float.isFinite(pet.instinct) ? Math.max(0, Math.min(100, pet.instinct)) / 100 : 0;
            context.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 6, 0xFF80614B);
            context.fill(barX, barY, barX + barWidth, barY + 5, 0xFF352730);
            int fill = Math.round(barWidth * value);
            if (fill > 0) {
                context.fill(barX, barY, barX + fill, barY + 5, UiColors.INSTINCT);
                context.fill(barX, barY, barX + fill, barY + 1, 0xFFD38A99);
            }
        }
    }
}
