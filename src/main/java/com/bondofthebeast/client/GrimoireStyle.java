package com.bondofthebeast.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Shared book backdrop and parchment colors for grimoire screens. */
public final class GrimoireStyle {
    public static final Identifier OWNER_BOOK = new Identifier("bondofthebeast", "textures/gui/owner_book_petmen.png");
    private static final Identifier BOOK_FONT = new Identifier("minecraft", "default");
    public static final net.minecraft.client.gui.tooltip.TooltipPositioner TOOLTIP_POSITIONER =
            (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> {
                int x = mouseX + 12;
                if (x + tooltipWidth > screenWidth - 6) x = mouseX - tooltipWidth - 12;
                int y = mouseY + 12;
                if (y + tooltipHeight > screenHeight - 6) y = mouseY - tooltipHeight - 12;
                return new org.joml.Vector2i(Math.max(6, Math.min(x, screenWidth - tooltipWidth - 6)),
                        Math.max(6, Math.min(y, screenHeight - tooltipHeight - 6)));
            };

    public static final int PARCHMENT = 0xE8EAD9AD;
    public static final int PARCHMENT_BORDER = 0xFF806744;
    public static final int SELECTED_BORDER = 0xFF4A3028;
    public static final int INK = 0xFF382820;
    public static final int MUTED_INK = 0xFF6A5540;
    public static final int PREVIEW = 0xCC25191D;

    private GrimoireStyle() {}

    public static void drawBook(DrawContext context, int screenWidth, int screenHeight) {
        BookLayout book = layout(screenWidth, screenHeight);
        context.fill(0, 0, screenWidth, screenHeight, 0xB8000000);
        // Destination dimensions and source region are separate: never sample past the texture.
        context.drawTexture(OWNER_BOOK, book.x, book.y, book.size(384), book.size(216),
                0.0F, 0.0F, 256, 200, 256, 256);
    }

    public static BookLayout layout(int width, int height) {
        float scale = Math.min(1.0F, Math.min((width - 16) / 384.0F, (height - 12) / 216.0F));
        return new BookLayout((width - Math.round(384 * scale)) / 2,
                (height - Math.round(216 * scale)) / 2, scale);
    }

    public record BookLayout(int x, int y, float scale) {
        public int x(int offset) { return x + size(offset); }
        public int y(int offset) { return y + size(offset); }
        public int size(int length) { return Math.round(length * scale); }
    }

    public static PanelLayout panel(int width, int height, int panelWidth, int panelHeight) {
        float scale = Math.min(1.0F, Math.min((width - 16) / (float) panelWidth,
                (height - 16) / (float) panelHeight));
        return new PanelLayout((width - Math.round(panelWidth * scale)) / 2,
                (height - Math.round(panelHeight * scale)) / 2, panelWidth, panelHeight, scale);
    }

    public record PanelLayout(int left, int top, int width, int height, float scale) {
        public int size(int value) { return Math.round(value * scale); }
        public int x(int value) { return left + size(value); }
        public int y(int value) { return top + size(value); }
    }

    /** Draw in panel coordinates until the caller pops the matrix stack. */
    public static void beginPanel(DrawContext context, PanelLayout panel) {
        context.fill(panel.left + 2, panel.top + 3, panel.x(panel.width) + 2,
                panel.y(panel.height) + 3, 0x881A0D12);
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(panel.left, panel.top, 0);
        matrices.scale(panel.scale, panel.scale, 1);
        paper(context, 0, 0, panel.width, panel.height);
        leather(context, 6, 6, panel.width - 12, 22, false);
    }

    public static net.minecraft.text.MutableText bookText(net.minecraft.text.Text text) {
        return text.copy().styled(style -> style.withFont(BOOK_FONT));
    }

    public static void wrappedText(DrawContext context, net.minecraft.client.font.TextRenderer renderer,
                                   net.minecraft.text.Text text, int centerX, int y, int maxWidth,
                                   int maxHeight, int color) {
        var styledText = bookText(text);
        float scale = 1.0F;
        var lines = renderer.wrapLines(styledText, maxWidth);
        while (lines.size() * 10 * scale > maxHeight && scale > 0.5F) {
            scale -= 0.05F;
            lines = renderer.wrapLines(styledText, (int) (maxWidth / scale));
        }
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, y, 0);
        matrices.scale(scale, scale, 1);
        int lineY = 0;
        for (var line : lines) {
            context.drawText(renderer, line, -renderer.getWidth(line) / 2, lineY, color, false);
            lineY += 10;
        }
        matrices.pop();
    }

    public static void paper(DrawContext context, int x, int y, int width, int height) {
        context.drawTexture(OWNER_BOOK, x, y, width, height, 36.0F, 65.0F, 60, 69, 256, 256);
        context.drawBorder(x, y, width, height, PARCHMENT_BORDER);
    }

    public static void leather(DrawContext context, int x, int y, int width, int height, boolean highlighted) {
        context.drawTexture(OWNER_BOOK, x, y, width, height, 120.0F, 70.0F, 60, 40, 256, 256);
        if (highlighted) context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x28F0CCA0);
        context.drawBorder(x, y, width, height, highlighted ? 0xFFE5C58A : 0xFFA58A58);
        context.fill(x + 2, y + 1, x + width - 2, y + 2, 0xFF8F5150);
        context.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, 0xFF3C2225);
    }

    public static void fittedText(DrawContext context, net.minecraft.client.font.TextRenderer renderer,
                                  net.minecraft.text.Text text, int centerX, int y, int maxWidth, int color) {
        fittedText(context, renderer, text, centerX, y, maxWidth, color, 1.0F);
    }

    public static void fittedText(DrawContext context, net.minecraft.client.font.TextRenderer renderer,
                                  net.minecraft.text.Text text, int centerX, int y, int maxWidth, int color,
                                  float maximumScale) {
        var styledText = bookText(text);
        int textWidth = renderer.getWidth(styledText);
        float scale = Math.min(maximumScale, maxWidth / (float) Math.max(1, textWidth));
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, y, 0);
        matrices.scale(scale, scale, 1);
        context.drawText(renderer, styledText, -textWidth / 2, 0, color, false);
        matrices.pop();
    }

    /** Keep native sprite pixels square and aligned, including on resized screens. */
    public static void drawGlyph(DrawContext context, Identifier texture, int x, int y, int size,
                                 int horizontalPixelOffset) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        double guiScale = client.getWindow().getScaleFactor();
        // Leave a wider margin while keeping each sprite pixel an integer number of screen pixels.
        int pixelScale = Math.max(1, (int) Math.floor((size - 2) * 0.8F * guiScale / 14));
        float iconSize = (float) (14 * pixelScale / guiScale);
        float iconX = (float) ((Math.round((x + (size - iconSize) / 2) * guiScale)
                + horizontalPixelOffset) / guiScale);
        float iconY = (float) (Math.round((y + (size - iconSize) / 2) * guiScale) / guiScale);
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(iconX, iconY, 0);
        matrices.scale((float) (pixelScale / guiScale), (float) (pixelScale / guiScale), 1);
        var textures = client.getTextureManager();
        textures.bindTexture(texture);
        textures.getTexture(texture).setFilter(false, false);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        context.drawTexture(texture, 0, 0, 1, 1, 14, 14, 16, 16);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        matrices.pop();
    }
}
