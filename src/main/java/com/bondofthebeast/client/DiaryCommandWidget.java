package com.bondofthebeast.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.BooleanSupplier;

/** Read-only command status in the pet's diary. */
final class DiaryCommandWidget extends ButtonWidget {
    private final Identifier icon;
    private final String name;
    private final BooleanSupplier available;
    private final BooleanSupplier enabled;
    private boolean lastAvailable, lastEnabled;
    private boolean initialized;

    DiaryCommandWidget(int x, int y, int width, int height, String name, String glyph,
                       BooleanSupplier available, BooleanSupplier enabled) {
        super(x, y, width, height, Text.translatable("gui.bondofthebeast.control." + name), b -> {}, DEFAULT_NARRATION_SUPPLIER);
        this.name = name;
        this.icon = new Identifier("bondofthebeast", "textures/gui/abilities/engraved/" + glyph + ".png");
        this.available = available;
        this.enabled = enabled;
    }

    @Override protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean unlocked = available.getAsBoolean();
        boolean on = enabled.getAsBoolean() && unlocked;
        if (!initialized || unlocked != lastAvailable || on != lastEnabled) {
            initialized = true;
            lastAvailable = unlocked;
            lastEnabled = on;
            Text status = Text.translatable(!unlocked ? "gui.bondofthebeast.diary.unavailable" :
                    on ? "gui.bondofthebeast.on" : "gui.bondofthebeast.off");
            Text tooltip = getMessage().copy().append(Text.literal(" — ")).append(status)
                    .append(Text.literal("\n")).append(Text.translatable("gui.bondofthebeast.control.description." + name));
            setTooltip(Tooltip.of(tooltip));
        }
        PetDiaryStyle.button(context, getX(), getY(), width, height, isHovered());
        GrimoireStyle.drawGlyph(context, icon, getX() + 1, getY(), height, 0);
        int x = getX() + width - 10, y = getY() + height / 2 - 3;
        context.fill(x - 1, y - 1, x + 7, y + 7, 0xFF101725);
        context.fill(x, y, x + 6, y + 6, on ? 0xFF83B25F : unlocked ? 0xFF5E6070 : 0xFF323949);
        if (!unlocked) context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x65313B50);
    }

    @Override protected net.minecraft.client.gui.tooltip.TooltipPositioner getTooltipPositioner() {
        return GrimoireStyle.TOOLTIP_POSITIONER;
    }
}
