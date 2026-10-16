package com.bondofthebeast.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** A pixel-art ability glyph and a separate punched-paper state indicator. */
public final class GrimoireAbilityButton extends ButtonWidget {
    // Previous parchment icons remain available as "ink".
    private static final String ICON_STYLE = "engraved";
    private final Identifier abilityTexture;
    private final Boolean value;
    private final int iconOffset;

    public GrimoireAbilityButton(int x, int y, int width, int height, Text label, Boolean value,
                                 String glyph, PressAction onPress) {
        super(x, y, width, height, value == null ? label : label.copy().append(Text.literal(" "))
                .append(Text.translatable(value ? "gui.bondofthebeast.on" : "gui.bondofthebeast.off")),
                onPress, DEFAULT_NARRATION_SUPPLIER);
        this.value = value;
        this.iconOffset = glyph.equals("tp") ? -2 : 0;
        this.abilityTexture = new Identifier("bondofthebeast",
                "textures/gui/abilities/" + ICON_STYLE + "/" + glyph + ".png");
    }

    @Override protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX() + 2, getY() + 2, getX() + width + 2, getY() + height + 2, 0x881A0D12);
        GrimoireStyle.leather(context, getX() - 1, getY() - 1, width + 2, height + 2, false);
        GrimoireStyle.paper(context, getX(), getY(), width, height);
        int iconArea = Math.min(height, width);
        // A square leather plate keeps the glyph distinct from the paper switch.
        if (ICON_STYLE.equals("engraved")) {
            context.drawTexture(GrimoireStyle.OWNER_BOOK, getX(), getY(), iconArea, iconArea,
                    120.0F, 70.0F, 60, 40, 256, 256);
            context.fill(getX(), getY(), getX() + iconArea, getY() + iconArea, 0xAD48232E);
            context.drawBorder(getX(), getY(), iconArea, iconArea, 0xFFB48C59);
            context.fill(getX() + 1, getY() + 1, getX() + iconArea - 1, getY() + 2, 0xFF99615E);
            context.fill(getX() + 1, getY() + iconArea - 2,
                    getX() + iconArea - 1, getY() + iconArea - 1, 0xFF31131E);
        } else {
            context.drawBorder(getX(), getY(), iconArea, iconArea, GrimoireStyle.PARCHMENT_BORDER);
        }
        GrimoireStyle.drawGlyph(context, abilityTexture, getX(), getY(), iconArea, iconOffset);
        if (!active) context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x66523D31);
        int holeSize = Math.max(5, Math.round(height * .25f));
        int holeX = getX() + iconArea + (width - iconArea - holeSize) / 2;
        int holeY = getY() + (height - holeSize) / 2;
        context.fill(holeX - 1, holeY - 1, holeX + holeSize + 1, holeY + holeSize + 1, 0xFF8D7651);
        context.fill(holeX, holeY + holeSize, holeX + holeSize + 1, holeY + holeSize + 1, 0xFFF4E3AF);
        context.fill(holeX, holeY, holeX + holeSize, holeY + holeSize, 0xFF41372A);
        if (Boolean.TRUE.equals(value)) {
            context.fill(holeX + 1, holeY + 1, holeX + holeSize - 1, holeY + holeSize - 1, 0xFF588447);
            context.fill(holeX + 1, holeY + 1, holeX + holeSize - 1, holeY + 2, 0xFF94BD6C);
        } else if (value == null) {
            // Navigation has an arrow in the recess, rather than a fake on/off state.
            int midX = holeX + holeSize / 2, midY = holeY + holeSize / 2;
            for (int row = -2; row <= 2; row++) {
                int offset = 2 - Math.abs(row);
                context.fill(midX + offset - 1, midY + row, midX + offset, midY + row + 1,
                        active ? 0xFFEAD4A2 : 0xFF9A8865);
            }
        }
        if (active && (isHovered() || isFocused())) context.drawBorder(getX(), getY(), width, height, 0xFFF0DCAA);
    }

    @Override protected net.minecraft.client.gui.tooltip.TooltipPositioner getTooltipPositioner() {
        return GrimoireStyle.TOOLTIP_POSITIONER;
    }
}
