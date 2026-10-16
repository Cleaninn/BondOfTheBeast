package com.bondofthebeast.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** A square leather tab with a return arrow, kept within the book cover. */
public final class GrimoireBackButton extends ButtonWidget {
    private static final Identifier TEXTURE = new Identifier("bondofthebeast",
            "textures/gui/abilities/engraved/back.png");

    public GrimoireBackButton(int x, int y, int size, Text label, PressAction onPress) {
        super(x, y, size, size, label, onPress, DEFAULT_NARRATION_SUPPLIER);
        setTooltip(Tooltip.of(label));
    }

    @Override protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX() + 1, getY() + 2, getX() + width + 1, getY() + height + 2, 0x881A0D12);
        GrimoireStyle.leather(context, getX(), getY(), width, height, isHovered() || isFocused());
        context.fill(getX() + 1, getY() + 2, getX() + width - 1, getY() + height - 2, 0x7048232E);
        GrimoireStyle.drawGlyph(context, TEXTURE, getX(), getY(), width, 0);
    }

    @Override protected net.minecraft.client.gui.tooltip.TooltipPositioner getTooltipPositioner() {
        return GrimoireStyle.TOOLTIP_POSITIONER;
    }
}
