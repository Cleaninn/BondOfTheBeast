package com.bondofthebeast.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Leather plaque with parchment lettering for grimoire screens. */
public final class GrimoireButtonWidget extends ButtonWidget {
    public GrimoireButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override protected net.minecraft.client.gui.tooltip.TooltipPositioner getTooltipPositioner() {
        return GrimoireStyle.TOOLTIP_POSITIONER;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHovered();
        GrimoireStyle.leather(context, getX(), getY(), width, height, active && (hovered || isFocused()));
        if (!active) context.fill(getX() + 1, getY() + 2, getX() + width - 1, getY() + height - 2, 0x700F0C0B);
        int color = active ? 0xFFF0DCAA : 0xFFA28E73;
        if (width >= 112) {
            context.fill(getX() + 5, getY() + height / 2, getX() + 8, getY() + height / 2 + 1, color);
            context.fill(getX() + width - 8, getY() + height / 2, getX() + width - 5, getY() + height / 2 + 1, color);
        }
        var renderer = MinecraftClient.getInstance().textRenderer;
        int maxWidth = width >= 112 ? width - 24 : width - 8;
        float scale = Math.min(Math.min(1.0F, (height - 6) / (float) renderer.fontHeight),
                maxWidth / (float) Math.max(1, renderer.getWidth(GrimoireStyle.bookText(getMessage()))));
        GrimoireStyle.fittedText(context, renderer, getMessage(), getX() + width / 2,
                getY() + (height - (int) Math.ceil(renderer.fontHeight * scale)) / 2, maxWidth, color, scale);
    }
}
