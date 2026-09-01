package com.bondofthebeast.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StaffMainScreen extends Screen {
    private final List<PetData> pets;

    public StaffMainScreen(List<PetData> pets) {
        super(Text.translatable("gui.bondofthebeast.staff.main_screen_name"));
        this.pets = pets;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < pets.size(); i++) {
            PetData p = pets.get(i);
            String displayName = p.name;
            String loginName = "";
            if (p.name.contains("|")) {
                String[] parts = p.name.split("\\|");
                displayName = parts[0];
                if (parts.length > 1) {
                    loginName = parts[1];
                }
            }

            MutableText t = Text.literal(displayName);
            if (!loginName.isEmpty() && !loginName.equals(displayName)) {
                t.append(Text.literal(" (" + loginName + ")").formatted(Formatting.GRAY));
            }

            if (!p.isOnline) {
                t.append(Text.translatable("gui.bondofthebeast.staff.offline").formatted(Formatting.DARK_GRAY));
            } else if (!p.hasCollar) {
                t.append(" ").append(Text.translatable("gui.bondofthebeast.staff.no_collar_suffix").formatted(Formatting.RED));
            }

            this.addDrawableChild(ButtonWidget.builder(t, b -> this.client.setScreen(new StaffPetScreen(this, p)))
                    .dimensions(this.width / 2 - 100, 50 + (i * 24), 200, 20).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.bondofthebeast.staff.main_title").formatted(Formatting.GOLD), this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    public static class PetData {
        public final UUID uuid;
        public final String name;
        public boolean isSitting, isTeleportEnabled, isProtectionMode, isAuraEnabled, isPacifistMode, isVampiricMode, isNoBreakMode, isAbsorbed, isNoInteractMode;
        public int bondLevel, bondExp;
        public boolean hasCollar;
        public int skillPoints;
        public Set<String> unlockedSkills;
        public Set<String> blacklistedBlocks;
        public Set<String> whitelistedBlocks;
        public boolean isOnline;

        public PetData(UUID uuid, String name, boolean sitting, boolean tp, boolean prot, boolean aura, boolean pacifist, boolean vampiric, boolean noBreak, boolean absorbed, boolean noInteract, int level, int exp, boolean collar, int skillPoints, Set<String> unlockedSkills, Set<String> black, Set<String> white, boolean isOnline) {
            this.uuid = uuid; this.name = name; this.isSitting = sitting; this.isTeleportEnabled = tp;
            this.isProtectionMode = prot; this.isAuraEnabled = aura; this.isPacifistMode = pacifist;
            this.isVampiricMode = vampiric; this.isNoBreakMode = noBreak; this.isAbsorbed = absorbed;
            this.isNoInteractMode = noInteract;
            this.bondLevel = level; this.bondExp = exp; this.hasCollar = collar;
            this.skillPoints = skillPoints; this.unlockedSkills = unlockedSkills;
            this.blacklistedBlocks = black; this.whitelistedBlocks = white; this.isOnline = isOnline;
        }
    }
}