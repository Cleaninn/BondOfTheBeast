package com.bondofthebeast.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** The original blue diary, kept separate from the owner's grimoire. */
public final class PetDiaryStyle {
    private static final Identifier BOOK = new Identifier("bondofthebeast", "textures/gui/start_book.png");

    private PetDiaryStyle() {}

    public static void begin(DrawContext context, GrimoireStyle.PanelLayout panel, int width, int height) {
        context.fill(0, 0, width, height, 0xB8000000);
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(panel.left(), panel.top(), 0);
        matrices.scale(panel.scale(), panel.scale(), 1);
        context.drawTexture(BOOK, 0, 0, 0, 0, 240, 220, 240, 220);
    }

    private static void blue(DrawContext context, int x, int y, int width, int height) {
        context.drawTexture(BOOK, x, y, width, height, 30, 8, 40, 12, 240, 220);
    }

    public static void button(DrawContext context, int x, int y, int width, int height, boolean highlighted) {
        blue(context, x, y, width, height);
        context.drawBorder(x, y, width, height, highlighted ? 0xFFE8D5A4 : 0xFF927E62);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF4C5D82);
        if (highlighted) context.fill(x + 1, y + 2, x + width - 1, y + height - 1, 0x224CA4DC);
    }
}
