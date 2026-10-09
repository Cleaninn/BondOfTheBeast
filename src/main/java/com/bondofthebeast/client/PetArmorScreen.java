package com.bondofthebeast.client;

import com.bondofthebeast.PetArmorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class PetArmorScreen extends HandledScreen<PetArmorScreenHandler> {
    public PetArmorScreen(PetArmorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 166;
        playerInventoryTitleY = 72;
    }
    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        GrimoireStyle.paper(context, x, y, backgroundWidth, backgroundHeight);
        GrimoireStyle.leather(context, x + 4, y + 4, backgroundWidth - 8, 20, false);
        for (var slot : handler.slots) {
            context.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, GrimoireStyle.PREVIEW);
            context.drawBorder(x + slot.x - 1, y + slot.y - 1, 18, 18, GrimoireStyle.PARCHMENT_BORDER);
        }
    }
    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        GrimoireStyle.fittedText(context, textRenderer, title, backgroundWidth / 2, 9,
                backgroundWidth - 24, 0xFFF0DCAA, 1.0F);
        GrimoireStyle.fittedText(context, textRenderer, playerInventoryTitle, backgroundWidth / 2,
                playerInventoryTitleY, backgroundWidth - 16, GrimoireStyle.MUTED_INK, 1.0F);
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GrimoireStyle.drawBook(context, width, height);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
