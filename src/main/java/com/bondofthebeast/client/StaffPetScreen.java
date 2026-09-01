package com.bondofthebeast.client;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class StaffPetScreen extends Screen {
    private final Screen parent;
    private final StaffMainScreen.PetData pet;

    private static final Identifier BG = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/owner_book_petmen.png");
    private static final Identifier TEX_SIT = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_sit.png");
    private static final Identifier TEX_TP = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_tp.png");
    private static final Identifier TEX_PROT = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_prot.png");
    private static final Identifier TEX_AURA = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_aura.png");
    private static final Identifier TEX_VAMPIRIC = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_vampiric.png");
    private static final Identifier TEX_NOBREAK = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_nobreak.png");
    private static final Identifier TEX_ABSORB = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_absorb.png");
    private static final Identifier TEX_LOCK = new Identifier(BondOfTheBeast.MOD_ID, "textures/gui/btn_lock.png");

    private final int OFFSET_TREE_X = 160;
    private final int OFFSET_TREE_Y = 40;
    private final int TREE_STEP_X = 42;
    private final int TREE_STEP_Y = 38;
    private final int ICON_SIZE = 26;
    private final int LIST_BTN_SIZE = 16;

    private final int OFFSET_MODEL_X = 66;
    private final int OFFSET_MODEL_Y = 125;
    private final int MODEL_SCALE = 30;

    private final int OFFSET_TITLE_Y = 40;
    private final int OFFSET_NAME_Y = 50;

    private final int OFFSET_EXP_X = 66;
    private final int OFFSET_EXP_Y = 165;
    private final int OFFSET_SP_X = 205;
    private final int OFFSET_SP_Y = 45;

    private final int OFFSET_BACK_X = 14;
    private final int OFFSET_BACK_Y = 22;
    private final int OFFSET_INFO_X = 226;
    private final int OFFSET_INFO_Y = 20;

    private ButtonWidget bSit, bPac, bNoB, bTp, bAur, bAbsorb, bPro, bVam, bBlack, bWhite, bInteract, bInfo;

    public StaffPetScreen(Screen parent, StaffMainScreen.PetData pet) {
        super(Text.translatable("gui.bondofthebeast.staff.title"));
        this.parent = parent;
        this.pet = pet;
    }

    @Override
    protected void init() {
        super.init();
        int bgX = (width - 256) / 2, bgY = (height - 200) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<-"), b -> this.client.setScreen(parent))
                .dimensions(bgX + OFFSET_BACK_X, bgY + OFFSET_BACK_Y, 16, 16).build());

        int cX = bgX + OFFSET_TREE_X;
        int cY = bgY + OFFSET_TREE_Y;

        bSit = createBtn(cX - ICON_SIZE/2, cY, ICON_SIZE, "sit", null, b -> {
            pet.isSitting = !pet.isSitting;
            sendToggle(ModPackets.TOGGLE_PET_STATE_C2S);
            this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
        });

        bTp = createBtn(cX - TREE_STEP_X - ICON_SIZE/2, cY + TREE_STEP_Y, ICON_SIZE, "tp", "sit", b -> {
            pet.isTeleportEnabled = !pet.isTeleportEnabled;
            sendToggle(ModPackets.TOGGLE_TELEPORT_C2S);
            this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
        });

        bAbsorb = createBtn(cX - TREE_STEP_X - ICON_SIZE/2, cY + TREE_STEP_Y * 2, ICON_SIZE, "absorb", "tp", b -> {
            if (!pet.isAbsorbed) {
                PlayerEntity ent = client.world.getPlayerByUuid(pet.uuid);
                if (ent == null || client.player.squaredDistanceTo(ent) > 25.0) {
                    if (client.player != null) client.player.sendMessage(Text.translatable("text.bondofthebeast.too_far_absorb").formatted(Formatting.RED), true);
                    return;
                }
            }
            pet.isAbsorbed = !pet.isAbsorbed;
            if(pet.isAbsorbed) pet.isSitting = false;
            sendToggle(ModPackets.TOGGLE_ABSORB_C2S);
            this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
        });

        bPro = createBtn(cX - ICON_SIZE/2, cY + TREE_STEP_Y, ICON_SIZE, "prot", "sit", b -> {
            pet.isProtectionMode = !pet.isProtectionMode;
            sendToggle(ModPackets.TOGGLE_PROTECTION_C2S);
            this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
        });

        bVam = createBtn(cX - ICON_SIZE/2, cY + TREE_STEP_Y * 2, ICON_SIZE, "vampiric", "prot", b -> {
            pet.isVampiricMode = !pet.isVampiricMode;
            sendToggle(ModPackets.TOGGLE_VAMPIRIC_C2S);
            this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
        });

        bNoB = createBtn(cX + TREE_STEP_X - ICON_SIZE/2, cY + TREE_STEP_Y, ICON_SIZE, "nobreak", "sit", b -> {
            pet.isNoBreakMode = !pet.isNoBreakMode;
            if (!pet.isNoBreakMode) {
                pet.isNoInteractMode = false;
                pet.isPacifistMode = false;
            }
            sendToggle(ModPackets.TOGGLE_NO_BREAK_C2S);
            this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
        });

        bAur = createBtn(cX + TREE_STEP_X - ICON_SIZE/2, cY + TREE_STEP_Y * 2, ICON_SIZE, "aura", "nobreak", b -> {
            pet.isAuraEnabled = !pet.isAuraEnabled;
            sendToggle(ModPackets.TOGGLE_AURA_C2S);
            this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
        });

        boolean noBreakUnlocked = pet.unlockedSkills.contains("nobreak");

        int listX = bNoB.getX() + 32;
        int listY = bNoB.getY() - 4;

        bInteract = ButtonWidget.builder(Text.empty(), b -> {
                    pet.isNoInteractMode = !pet.isNoInteractMode;
                    sendToggle(ModPackets.TOGGLE_NO_INTERACT_C2S);
                    this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
                }).dimensions(listX, listY, LIST_BTN_SIZE, LIST_BTN_SIZE)
                .tooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.staff.interact_tooltip").append("\n")
                        .append(pet.isNoBreakMode ? Text.translatable(pet.isNoInteractMode ? "command.bondofthebeast.status.on" : "command.bondofthebeast.status.off").formatted(pet.isNoInteractMode ? Formatting.GREEN : Formatting.RED)
                                : Text.translatable("gui.bondofthebeast.staff.requires_nobreak").formatted(Formatting.RED)))).build();

        bPac = ButtonWidget.builder(Text.empty(), b -> {
                    pet.isPacifistMode = !pet.isPacifistMode;
                    sendToggle(ModPackets.TOGGLE_PACIFIST_C2S);
                    this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
                }).dimensions(listX + 16, listY, LIST_BTN_SIZE, LIST_BTN_SIZE)
                .tooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.staff.pacifist_tooltip").append("\n")
                        .append(pet.isNoBreakMode ? Text.translatable(pet.isPacifistMode ? "command.bondofthebeast.status.on" : "command.bondofthebeast.status.off").formatted(pet.isPacifistMode ? Formatting.GREEN : Formatting.RED)
                                : Text.translatable("gui.bondofthebeast.staff.requires_nobreak").formatted(Formatting.RED)))).build();

        bBlack = ButtonWidget.builder(Text.empty(), b -> this.client.setScreen(new BlockSelectionScreen(this, pet, 0)))
                .dimensions(listX, listY + 16, LIST_BTN_SIZE, LIST_BTN_SIZE)
                .tooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.staff.blacklist_tooltip"))).build();

        bWhite = ButtonWidget.builder(Text.empty(), b -> this.client.setScreen(new BlockSelectionScreen(this, pet, 1)))
                .dimensions(listX + 16, listY + 16, LIST_BTN_SIZE, LIST_BTN_SIZE)
                .tooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.staff.whitelist_tooltip"))).build();

        bInteract.active = pet.hasCollar && noBreakUnlocked && pet.isNoBreakMode && pet.isOnline;
        bPac.active = pet.hasCollar && noBreakUnlocked && pet.isNoBreakMode && pet.isOnline;
        bBlack.active = pet.hasCollar && noBreakUnlocked && pet.isOnline;
        bWhite.active = pet.hasCollar && noBreakUnlocked && pet.isOnline;

        addDrawableChild(bInteract);
        addDrawableChild(bPac);
        addDrawableChild(bBlack);
        addDrawableChild(bWhite);

        ButtonWidget bSpTooltip = ButtonWidget.builder(Text.empty(), b -> {})
                .dimensions(bgX + OFFSET_SP_X - 10, bgY + OFFSET_SP_Y - 5, 20, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.staff.sp_tooltip", pet.skillPoints)))
                .build();
        bSpTooltip.setAlpha(0.0f);
        addDrawableChild(bSpTooltip);

        bInfo = ButtonWidget.builder(Text.empty(), b -> {})
                .dimensions(bgX + OFFSET_INFO_X, bgY + OFFSET_INFO_Y, 18, 18)
                .tooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.staff.info.tooltip")))
                .build();
        addDrawableChild(bInfo);
    }

    private ButtonWidget createBtn(int x, int y, int size, String key, String parentKey, ButtonWidget.PressAction toggleAction) {
        boolean isUnlocked = pet.unlockedSkills.contains(key);
        boolean canUnlock = parentKey == null || pet.unlockedSkills.contains(parentKey);

        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("gui.bondofthebeast.staff." + key + ".name").formatted(Formatting.GOLD));
        lines.add(Text.translatable("gui.bondofthebeast.staff." + key + ".desc").formatted(Formatting.GRAY));

        if (pet.hasCollar && isUnlocked) {
            boolean active = false;
            if (key.equals("sit")) active = pet.isSitting;
            else if (key.equals("tp")) active = pet.isTeleportEnabled;
            else if (key.equals("nobreak")) active = pet.isNoBreakMode;
            else if (key.equals("absorb")) active = pet.isAbsorbed;
            else if (key.equals("prot")) active = pet.isProtectionMode;
            else if (key.equals("aura")) active = pet.isAuraEnabled;
            else if (key.equals("vampiric")) active = pet.isVampiricMode;

            if (active) {
                lines.add(Text.translatable("gui.bondofthebeast.staff.state_on").formatted(Formatting.GREEN));
            } else {
                lines.add(Text.translatable("gui.bondofthebeast.staff.state_off").formatted(Formatting.RED));
            }
        }

        if (!pet.hasCollar) {
            lines.add(Text.translatable("gui.bondofthebeast.staff.need_collar").formatted(Formatting.RED));
        } else if (!isUnlocked) {
            if (!canUnlock) {
                lines.add(Text.translatable("gui.bondofthebeast.staff.requires_previous").formatted(Formatting.RED));
            } else if (pet.skillPoints > 0) {
                lines.add(Text.translatable("gui.bondofthebeast.staff.click_to_unlock").formatted(Formatting.GREEN));
            } else {
                lines.add(Text.translatable("gui.bondofthebeast.staff.not_enough_points").formatted(Formatting.RED));
            }
        }

        ButtonWidget button = ButtonWidget.builder(Text.empty(), b -> {
            if (!pet.hasCollar || !pet.isOnline) return;
            if (isUnlocked) {
                toggleAction.onPress(b);
            } else if (canUnlock && pet.skillPoints > 0) {
                pet.unlockedSkills.add(key);
                pet.skillPoints--;
                sendUnlock(key);
                this.client.setScreen(new StaffPetScreen(this.parent, this.pet));
            }
        }).dimensions(x, y, size, size).tooltip(Tooltip.of(joinTexts(lines))).build();

        button.active = pet.hasCollar && pet.isOnline;
        return addDrawableChild(button);
    }

    private Text joinTexts(List<Text> lines) {
        MutableText finalBox = Text.empty();
        for (int i = 0; i < lines.size(); i++) {
            finalBox.append(lines.get(i));
            if (i < lines.size() - 1) finalBox.append(Text.literal("\n"));
        }
        return finalBox;
    }

    private void sendToggle(Identifier id) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(pet.uuid);
        ClientPlayNetworking.send(id, buf);
    }

    private void sendUnlock(String skill) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(pet.uuid);
        buf.writeString(skill);
        ClientPlayNetworking.send(ModPackets.UNLOCK_SKILL_C2S, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int x = (width - 256) / 2, y = (height - 200) / 2;

        context.drawTexture(BG, x, y, 0, 0, 256, 200);

        drawTreeLine(context, bSit, bTp, pet.unlockedSkills.contains("tp"));
        drawTreeLine(context, bSit, bPro, pet.unlockedSkills.contains("prot"));
        drawTreeLine(context, bSit, bNoB, pet.unlockedSkills.contains("nobreak"));

        drawTreeLine(context, bTp, bAbsorb, pet.unlockedSkills.contains("absorb"));
        drawTreeLine(context, bPro, bVam, pet.unlockedSkills.contains("vampiric"));
        drawTreeLine(context, bNoB, bAur, pet.unlockedSkills.contains("aura"));

        String displayName = pet.name;
        String loginName = "";
        if (pet.name.contains("|")) {
            String[] parts = pet.name.split("\\|");
            displayName = parts[0];
            if (parts.length > 1) {
                loginName = parts[1];
            }
        }

        Text titleText = Text.translatable("gui.bondofthebeast.staff.pet_bond").formatted(Formatting.GOLD);
        Text nameText = Text.literal(displayName).formatted(Formatting.WHITE);

        context.drawCenteredTextWithShadow(textRenderer, titleText, x + OFFSET_MODEL_X, y + OFFSET_TITLE_Y, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, nameText, x + OFFSET_MODEL_X, y + OFFSET_NAME_Y, 0xFFFFFF);

        if (!loginName.isEmpty() && !loginName.equals(displayName)) {
            Text loginText = Text.literal("(" + loginName + ")").formatted(Formatting.GRAY);
            context.drawCenteredTextWithShadow(textRenderer, loginText, x + OFFSET_MODEL_X, y + OFFSET_NAME_Y + 10, 0xFFFFFF);
        }

        context.drawCenteredTextWithShadow(textRenderer, String.valueOf(pet.skillPoints), x + OFFSET_SP_X, y + OFFSET_SP_Y, 0xFFFF55);

        if (!pet.isOnline) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.bondofthebeast.staff.pet_offline_big").formatted(Formatting.RED, Formatting.BOLD), x + OFFSET_MODEL_X, y + OFFSET_MODEL_Y + 10, 0xFFFFFF);
        } else {
            PlayerEntity ent = client.world.getPlayerByUuid(pet.uuid);
            if (ent != null) {
                boolean hudHidden = client.options.hudHidden;
                client.options.hudHidden = true;
                InventoryScreen.drawEntity(context, x + OFFSET_MODEL_X, y + OFFSET_MODEL_Y, MODEL_SCALE, (float)(x+OFFSET_MODEL_X)-mouseX, (float)(y+OFFSET_MODEL_Y-38)-mouseY, ent);
                client.options.hudHidden = hudHidden;
            }
        }

        drawExperienceBar(context, x + OFFSET_EXP_X, y + OFFSET_EXP_Y);

        super.render(context, mouseX, mouseY, delta);

        drawIcons(context);

        context.drawCenteredTextWithShadow(textRenderer, "?", bInfo.getX() + 9, bInfo.getY() + 5, 0xFFD700);
    }

    private void drawTreeLine(DrawContext context, ButtonWidget b1, ButtonWidget b2, boolean unlocked) {
        int color = unlocked ? 0xFFFFAA00 : 0xFF333333;
        int thick = 2;
        int x1 = b1.getX() + b1.getWidth() / 2, y1 = b1.getY() + b1.getHeight() / 2;
        int x2 = b2.getX() + b2.getWidth() / 2, y2 = b2.getY() + b2.getHeight() / 2;

        int midY = (y1 + y2) / 2;

        context.fill(x1 - thick, y1, x1 + thick, midY, color);
        context.fill(Math.min(x1, x2) - thick, midY - thick, Math.max(x1, x2) + thick, midY + thick, color);
        context.fill(x2 - thick, midY, x2 + thick, y2, color);
    }

    private void drawExperienceBar(DrawContext context, int centerX, int barY) {
        int barWidth = 90, barHeight = 6, barX = centerX - (barWidth / 2);

        float progress = Math.min(1.0f, (float) pet.bondExp / (pet.bondLevel * 100));

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        context.fill(barX, barY, barX + (int)(barWidth * progress), barY + barHeight, 0xFF228B22);
        context.drawBorder(barX - 1, barY - 1, barWidth + 2, barHeight + 2, 0xFF000000);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.bondofthebeast.staff.level", pet.bondLevel).formatted(Formatting.GOLD), centerX, barY - 12, 0xFFFFFF);
    }

    private void drawIcons(DrawContext context) {
        drawIconSitInverted(context, bSit, TEX_SIT, pet.isSitting, "sit");
        drawIcon(context, bNoB, TEX_NOBREAK, pet.isNoBreakMode, "nobreak");
        drawIcon(context, bTp, TEX_TP, pet.isTeleportEnabled, "tp");
        drawIcon(context, bAur, TEX_AURA, pet.isAuraEnabled, "aura");
        drawIcon(context, bAbsorb, TEX_ABSORB, pet.isAbsorbed, "absorb");
        drawIcon(context, bPro, TEX_PROT, pet.isProtectionMode, "prot");
        drawIcon(context, bVam, TEX_VAMPIRIC, pet.isVampiricMode, "vampiric");

        if (pet.unlockedSkills.contains("nobreak")) {
            context.drawItem(net.minecraft.item.Items.CHEST.getDefaultStack(), bInteract.getX(), bInteract.getY());
            context.drawItem(net.minecraft.item.Items.IRON_SWORD.getDefaultStack(), bPac.getX(), bPac.getY());
            context.drawItem(net.minecraft.item.Items.INK_SAC.getDefaultStack(), bBlack.getX(), bBlack.getY());
            context.drawItem(net.minecraft.item.Items.PAPER.getDefaultStack(), bWhite.getX(), bWhite.getY());

            if (pet.isNoInteractMode) context.drawBorder(bInteract.getX() - 1, bInteract.getY() - 1, 18, 18, 0xFF00FF00);
            else context.drawBorder(bInteract.getX() - 1, bInteract.getY() - 1, 18, 18, 0xFFFF0000);

            if (pet.isPacifistMode) context.drawBorder(bPac.getX() - 1, bPac.getY() - 1, 18, 18, 0xFF00FF00);
            else context.drawBorder(bPac.getX() - 1, bPac.getY() - 1, 18, 18, 0xFFFF0000);
        } else {
            context.drawTexture(TEX_LOCK, bInteract.getX(), bInteract.getY(), 16, 16, 0, 0, 16, 16, 16, 16);
            context.drawTexture(TEX_LOCK, bPac.getX(), bPac.getY(), 16, 16, 0, 0, 16, 16, 16, 16);
            context.drawTexture(TEX_LOCK, bBlack.getX(), bBlack.getY(), 16, 16, 0, 0, 16, 16, 16, 16);
            context.drawTexture(TEX_LOCK, bWhite.getX(), bWhite.getY(), 16, 16, 0, 0, 16, 16, 16, 16);
        }
    }

    private void drawIcon(DrawContext context, ButtonWidget btn, Identifier tex, boolean active, String key) {
        int offset = (btn.getWidth() - ICON_SIZE) / 2;
        if (!pet.hasCollar || !pet.unlockedSkills.contains(key)) {
            context.drawTexture(TEX_LOCK, btn.getX() + offset, btn.getY() + offset, ICON_SIZE, ICON_SIZE, 0, 0, 16, 16, 16, 16);
        } else {
            context.drawTexture(tex, btn.getX() + offset, btn.getY() + offset, ICON_SIZE, ICON_SIZE, 0, active ? 0 : 24, 24, 24, 24, 48);
        }
    }

    private void drawIconSitInverted(DrawContext context, ButtonWidget btn, Identifier tex, boolean active, String key) {
        int offset = (btn.getWidth() - ICON_SIZE) / 2;
        if (!pet.hasCollar || !pet.unlockedSkills.contains(key)) {
            context.drawTexture(TEX_LOCK, btn.getX() + offset, btn.getY() + offset, ICON_SIZE, ICON_SIZE, 0, 0, 16, 16, 16, 16);
        } else {
            context.drawTexture(tex, btn.getX() + offset, btn.getY() + offset, ICON_SIZE, ICON_SIZE, 0, active ? 24 : 0, 24, 24, 24, 48);
        }
    }
}