package com.bondofthebeast.client;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class PetStatusScreen extends Screen {
    private final Screen parent;

    private static final Identifier BOOK_BG = new Identifier("bondofthebeast", "textures/gui/start_book.png");

    private static final Identifier TEX_SIT = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_sit.png");
    private static final Identifier TEX_TP = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_tp.png");
    private static final Identifier TEX_PROT = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_prot.png");
    private static final Identifier TEX_AURA = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_aura.png");
    private static final Identifier TEX_VAMPIRIC = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_vampiric.png");
    private static final Identifier TEX_NOBREAK = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_nobreak.png");
    private static final Identifier TEX_ABSORB = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_absorb.png");
    private static final Identifier TEX_LOCK = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_lock.png");

    private final int OFFSET_INFO_X = 70;
    private final int OFFSET_INFO_Y = -96;

    private final int OFFSET_BACK_X = -90;
    private final int OFFSET_BACK_Y = 82;

    private final int OFFSET_TITLE_Y = -105;
    private final int OFFSET_NAME_Y = -95;
    private final int OFFSET_MODEL_X = 0;
    private final int OFFSET_MODEL_Y = -25;
    private final int MODEL_SCALE = 30;
    private final int OFFSET_EXP_X = 0;
    private final int OFFSET_EXP_Y = -5;

    private final int GRID_START_X = -65;
    private final int GRID_START_Y = 30;
    private final int GRID_STEP_X = 40;
    private final int GRID_STEP_Y = 30;
    private final int ICON_SIZE = 22;

    public PetStatusScreen(Screen parent) {
        super(Text.translatable("gui.bondofthebeast.pet_diary.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = width / 2;
        int y = height / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {})
                .dimensions(x + OFFSET_INFO_X, y + OFFSET_INFO_Y, 14, 14)
                .tooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.pet_diary.info.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<-"), b -> this.client.setScreen(parent))
                .dimensions(x + OFFSET_BACK_X, y + OFFSET_BACK_Y, 16, 16).build());

        setupDirectiveTooltips(x + GRID_START_X, y + GRID_START_Y);
    }

    private void setupDirectiveTooltips(int startX, int startY) {
        addTransparentTooltipButton(startX, startY, "sit");
        addTransparentTooltipButton(startX + GRID_STEP_X, startY, "tp");
        addTransparentTooltipButton(startX + GRID_STEP_X * 2, startY, "prot");
        addTransparentTooltipButton(startX + GRID_STEP_X * 3, startY, "nobreak");

        addTransparentTooltipButton(startX, startY + GRID_STEP_Y, "aura");
        addTransparentTooltipButton(startX + GRID_STEP_X, startY + GRID_STEP_Y, "absorb");
        addTransparentTooltipButton(startX + GRID_STEP_X * 2, startY + GRID_STEP_Y, "vampiric");

        int extraX = startX + GRID_STEP_X * 3;
        int extraY = startY + GRID_STEP_Y;
        addCustomTooltipButton(extraX, extraY, 11, Text.translatable("gui.bondofthebeast.staff.interact_tooltip"));
        addCustomTooltipButton(extraX + 11, extraY, 11, Text.translatable("gui.bondofthebeast.staff.pacifist_tooltip"));
        addCustomTooltipButton(extraX, extraY + 11, 11, Text.translatable("gui.bondofthebeast.staff.blacklist_tooltip"));
        addCustomTooltipButton(extraX + 11, extraY + 11, 11, Text.translatable("gui.bondofthebeast.staff.whitelist_tooltip"));
    }

    private void addTransparentTooltipButton(int x, int y, String key) {
        ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {})
                .dimensions(x, y, ICON_SIZE, ICON_SIZE)
                .tooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.staff." + key + ".desc.pet")))
                .build();
        btn.setAlpha(0.0f);
        this.addDrawableChild(btn);
    }

    private void addCustomTooltipButton(int x, int y, int size, Text text) {
        ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {})
                .dimensions(x, y, size, size)
                .tooltip(Tooltip.of(text))
                .build();
        btn.setAlpha(0.0f);
        this.addDrawableChild(btn);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int x = width / 2;
        int y = height / 2;

        context.drawTexture(BOOK_BG, x - 120, y - 110, 0.0F, 0.0F, 240, 220, 240, 220);

        PlayerEntity player = this.client.player;
        if (player == null) return;
        var bond = ModComponents.PLAYER_BOND.get(player);

        if (bond.hasOwner()) {
            PlayerEntity owner = client.world.getPlayerByUuid(UUID.fromString(bond.getOwnerUUID()));
            if (owner != null) {
                Text titleText = Text.translatable("gui.bondofthebeast.pet_diary.master").formatted(Formatting.GOLD);
                Text nameText = Text.literal(bond.getOwnerName()).formatted(Formatting.GOLD);

                context.drawText(textRenderer, titleText, x + OFFSET_MODEL_X - textRenderer.getWidth(titleText) / 2, y + OFFSET_TITLE_Y, 0xFFFFFF, false);
                context.drawText(textRenderer, nameText, x + OFFSET_MODEL_X - textRenderer.getWidth(nameText) / 2, y + OFFSET_NAME_Y, 0xFFFFFF, false);

                boolean hudHidden = client.options.hudHidden;
                client.options.hudHidden = true;
                InventoryScreen.drawEntity(context, x + OFFSET_MODEL_X, y + OFFSET_MODEL_Y, MODEL_SCALE, (float)(x+OFFSET_MODEL_X)-mouseX, (float)(y+OFFSET_MODEL_Y-38)-mouseY, owner);
                client.options.hudHidden = hudHidden;
            }
        }

        drawPetExperienceBar(context, x + OFFSET_EXP_X, y + OFFSET_EXP_Y);
        super.render(context, mouseX, mouseY, delta);
        drawAllDirectives(context, x + GRID_START_X, y + GRID_START_Y);

        context.drawCenteredTextWithShadow(textRenderer, "?", x + OFFSET_INFO_X + 7, y + OFFSET_INFO_Y + 3, 0xFFD700);
    }

    private void drawPetExperienceBar(DrawContext context, int centerX, int barY) {
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        int barWidth = 110;
        int barHeight = 6;
        int barX = centerX - (barWidth / 2);
        int maxExp = bond.getBondLevel() * 100;
        float progress = Math.min(1.0f, (float) bond.getBondExperience() / maxExp);

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        context.fill(barX, barY, barX + (int)(barWidth * progress), barY + barHeight, 0xFF228B22);
        context.drawBorder(barX - 1, barY - 1, barWidth + 2, barHeight + 2, 0xFF000000);

        Text lvlText = Text.translatable("command.bondofthebeast.info.level", bond.getBondLevel()).formatted(Formatting.GOLD);
        context.drawCenteredTextWithShadow(textRenderer, lvlText, centerX, barY - 12, 0xFFFFFF);
    }

    private void drawAllDirectives(DrawContext context, int startX, int startY) {
        var bond = ModComponents.PLAYER_BOND.get(client.player);

        drawIconSitInverted(context, TEX_SIT, startX, startY, bond.isSitting(), "sit");
        drawIcon(context, TEX_TP, startX + GRID_STEP_X, startY, bond.isTeleportEnabled(), "tp");
        drawIcon(context, TEX_PROT, startX + GRID_STEP_X * 2, startY, bond.isProtectionMode(), "prot");
        drawIcon(context, TEX_NOBREAK, startX + GRID_STEP_X * 3, startY, bond.isNoBreakMode(), "nobreak");

        drawIcon(context, TEX_AURA, startX, startY + GRID_STEP_Y, bond.isAuraEnabled(), "aura");
        drawIcon(context, TEX_ABSORB, startX + GRID_STEP_X, startY + GRID_STEP_Y, bond.isAbsorbed(), "absorb");
        drawIcon(context, TEX_VAMPIRIC, startX + GRID_STEP_X * 2, startY + GRID_STEP_Y, bond.isVampiricMode(), "vampiric");

        int extraX = startX + GRID_STEP_X * 3;
        int extraY = startY + GRID_STEP_Y;

        if (bond.isSkillUnlocked("nobreak")) {
            context.getMatrices().push();
            float scale = 0.6f;
            context.getMatrices().scale(scale, scale, 1.0f);

            int sX1 = (int)((extraX + 1) / scale);
            int sY1 = (int)((extraY + 1) / scale);
            int sX2 = (int)((extraX + 12) / scale);
            int sY2 = (int)((extraY + 12) / scale);

            context.drawItem(net.minecraft.item.Items.CHEST.getDefaultStack(), sX1, sY1);
            context.drawItem(net.minecraft.item.Items.IRON_SWORD.getDefaultStack(), sX2, sY1);
            context.drawItem(net.minecraft.item.Items.INK_SAC.getDefaultStack(), sX1, sY2);
            context.drawItem(net.minecraft.item.Items.PAPER.getDefaultStack(), sX2, sY2);
            context.getMatrices().pop();

            if (bond.isNoInteractMode()) context.drawBorder(extraX, extraY, 11, 11, 0xFF00FF00);
            else context.drawBorder(extraX, extraY, 11, 11, 0xFFFF0000);

            if (bond.isPacifistMode()) context.drawBorder(extraX + 11, extraY, 11, 11, 0xFF00FF00);
            else context.drawBorder(extraX + 11, extraY, 11, 11, 0xFFFF0000);
        } else {
            context.drawTexture(TEX_LOCK, extraX + 3, extraY + 3, 16, 16, 0, 0, 16, 16, 16, 16);
        }
    }

    private void drawIcon(DrawContext context, Identifier tex, int x, int y, boolean active, String key) {
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        if (!bond.isSkillUnlocked(key)) {
            context.drawTexture(TEX_LOCK, x, y, ICON_SIZE, ICON_SIZE, 0, 0, 16, 16, 16, 16);
        } else {
            context.drawTexture(tex, x, y, ICON_SIZE, ICON_SIZE, 0, active ? 0 : 24, 24, 24, 24, 48);
        }
    }

    private void drawIconSitInverted(DrawContext context, Identifier tex, int x, int y, boolean active, String key) {
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        if (!bond.isSkillUnlocked(key)) {
            context.drawTexture(TEX_LOCK, x, y, ICON_SIZE, ICON_SIZE, 0, 0, 16, 16, 16, 16);
        } else {
            context.drawTexture(tex, x, y, ICON_SIZE, ICON_SIZE, 0, active ? 24 : 0, 24, 24, 24, 48);
        }
    }
}