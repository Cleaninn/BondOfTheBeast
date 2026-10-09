package com.bondofthebeast.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

/** Separate entry page for block lists, keeping the ability grid compact. */
public final class BlockRulesScreen extends Screen {
    private final Screen parent;
    private final StaffMainScreen.PetData pet;
    private int left, top;
    private float scale;

    public BlockRulesScreen(Screen parent, StaffMainScreen.PetData pet) {
        super(Text.translatable("gui.bondofthebeast.block_rules"));
        this.parent = parent;
        this.pet = pet;
    }

    @Override protected void init() {
        scale = Math.min(1f, Math.min((width - 16) / 232f, (height - 16) / 142f));
        left = (width - Math.round(232 * scale)) / 2;
        top = (height - Math.round(142 * scale)) / 2;
        for (int type = 0; type < 2; type++) {
            final int listType = type;
            Text label = Text.translatable(type == 0 ? "gui.bondofthebeast.block_select.blacklist" : "gui.bondofthebeast.block_select.whitelist");
            var button = addDrawableChild(new GrimoireAbilityButton(x(18 + type * 106), y(38), size(90), size(44),
                    label, null, type == 0 ? "blacklist" : "whitelist",
                    b -> client.setScreen(new BlockSelectionScreen(this, pet, listType))));
            button.active = pet.isOnline && pet.hasCollar && pet.escapeTicks == 0 && pet.unlockedSkills.contains("nobreak");
            button.setTooltip(Tooltip.of(Text.empty().append(label.copy().formatted(net.minecraft.util.Formatting.GOLD))
                    .append(Text.literal("\n")).append(Text.translatable(type == 0 ?
                            "gui.bondofthebeast.staff.blacklist.desc" : "gui.bondofthebeast.staff.whitelist.desc"))));
        }
        addDrawableChild(new GrimoireButtonWidget(x(18), y(114), size(196), size(18),
                Text.translatable("gui.back"), b -> close()));
    }

    private int size(int value) { return Math.round(value * scale); }
    private int x(int value) { return left + size(value); }
    private int y(int value) { return top + size(value); }

    @Override public void close() { client.setScreen(parent); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xCB0D0908);
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(left, top, 0);
        matrices.scale(scale, scale, 1);
        GrimoireStyle.paper(context, 0, 0, 232, 142);
        GrimoireStyle.leather(context, 6, 6, 220, 22, false);
        GrimoireStyle.fittedText(context, textRenderer, title, 116, 12, 198, 0xFFF0DCAA);
        for (int type = 0; type < 2; type++) {
            GrimoireStyle.fittedText(context, textRenderer, Text.translatable(type == 0 ?
                            "gui.bondofthebeast.block_select.blacklist" : "gui.bondofthebeast.block_select.whitelist"),
                    63 + type * 106, 88, 94, GrimoireStyle.INK);
        }
        matrices.pop();
        super.render(context, mouseX, mouseY, delta);
    }
}
