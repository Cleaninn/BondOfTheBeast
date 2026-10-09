package com.bondofthebeast.client;

import com.bondofthebeast.BondRules;
import com.bondofthebeast.VoluntaryBondService;
import com.bondofthebeast.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StaffPetScreen extends Screen {
    private final Screen parent;
    private final StaffMainScreen.PetData pet;
    private GrimoireStyle.BookLayout book;
    private net.minecraft.client.network.OtherClientPlayerEntity preview;

    public StaffPetScreen(Screen parent, StaffMainScreen.PetData pet) {
        super(Text.translatable("gui.bondofthebeast.control_title"));
        this.parent = parent;
        this.pet = pet;
    }

    @Override protected void init() {
        book = GrimoireStyle.layout(width, height);
        preview = previewEntity();
        if (pet.forced) {
            action(0, "sit", pet.isSitting, ModPackets.TOGGLE_PET_STATE_C2S, "sit");
            action(1, "nobreak", pet.isNoBreakMode, ModPackets.TOGGLE_NO_BREAK_C2S, "nobreak");
            action(2, "nobreak", pet.isNoInteractMode, ModPackets.TOGGLE_NO_INTERACT_C2S, "interact", "interact");
            action(3, "nobreak", pet.isPacifistMode, ModPackets.TOGGLE_PACIFIST_C2S, "pacifist", "pacifist");
            action(4, "armor", pet.armorLocked, ModPackets.TOGGLE_ARMOR_C2S, "armor_lock", "armor_lock");
            utility(5, "armor_open", "armor", "armor", b -> send(ModPackets.OPEN_ARMOR_C2S));
            utility(6, "block_rules", "nobreak", "blocks", b -> client.setScreen(new BlockRulesScreen(this, pet)));
        } else {
            action(0, "sit", pet.isSitting, ModPackets.TOGGLE_PET_STATE_C2S, "sit");
            action(1, "tp", pet.isTeleportEnabled, ModPackets.TOGGLE_TELEPORT_C2S, "tp");
            action(2, "prot", pet.isProtectionMode, ModPackets.TOGGLE_PROTECTION_C2S, "prot");
            action(3, "aura", pet.isAuraEnabled, ModPackets.TOGGLE_AURA_C2S, "aura");
            action(4, "vampiric", pet.isVampiricMode, ModPackets.TOGGLE_VAMPIRIC_C2S, "vampiric");
            action(5, "nobreak", pet.isNoBreakMode, ModPackets.TOGGLE_NO_BREAK_C2S, "nobreak");
            action(6, "nobreak", pet.isNoInteractMode, ModPackets.TOGGLE_NO_INTERACT_C2S, "interact", "interact");
            action(7, "nobreak", pet.isPacifistMode, ModPackets.TOGGLE_PACIFIST_C2S, "pacifist", "pacifist");
            action(8, "absorb", pet.isAbsorbed, ModPackets.TOGGLE_ABSORB_C2S, "absorb");
            action(9, "armor", pet.armorLocked, ModPackets.TOGGLE_ARMOR_C2S, "armor_lock", "armor_lock");

            utility(10, "armor_open", "armor", "armor", b -> send(ModPackets.OPEN_ARMOR_C2S));
            utility(11, "block_rules", "nobreak", "blocks", b -> client.setScreen(new BlockRulesScreen(this, pet)));
        }
        addDrawableChild(new GrimoireBackButton(book.x(344), book.y(25), book.size(24),
                Text.translatable(parent == null ? "gui.bondofthebeast.staff.back_tooltip" :
                        "gui.bondofthebeast.back_to_pets"), b -> close()));
    }

    private boolean available(String skill) {
        return pet.isOnline && pet.hasCollar && pet.escapeTicks == 0 && pet.unlockedSkills.contains(skill);
    }

    private void action(int index, String skill, boolean value, Identifier packet, String icon) {
        action(index, skill, value, packet, icon, skill);
    }

    private void action(int index, String skill, boolean value, Identifier packet, String icon, String name) {
        boolean unlocked = available(skill);
        if ((packet.equals(ModPackets.TOGGLE_NO_INTERACT_C2S) || packet.equals(ModPackets.TOGGLE_PACIFIST_C2S)) && !pet.isNoBreakMode) unlocked = false;
        int x = book.x(180 + index % 3 * 64);
        int y = book.y(52 + index / 3 * 34);
        Text label = Text.translatable("gui.bondofthebeast.control." + name);
        Text state = Text.translatable(value ? "gui.bondofthebeast.on" : "gui.bondofthebeast.off");
        Text fullLabel = Text.empty().append(label.copy().formatted(net.minecraft.util.Formatting.GOLD)).append(Text.literal(" — ")).append(state);
        Text lockedReason = Text.translatable("gui.bondofthebeast.requires_stage",
                BondRules.requiredStage(skill));
        if (!pet.forced) lockedReason = Text.translatable("gui.bondofthebeast.requires_trust",
                Text.translatable("gui.bondofthebeast.trust." + VoluntaryBondService.requiredTier(skill)));
        if ((name.equals("interact") || name.equals("pacifist")) && !pet.isNoBreakMode) lockedReason = Text.translatable("gui.bondofthebeast.staff.requires_nobreak");
        if (pet.escapeTicks > 0) lockedReason = Text.translatable("gui.bondofthebeast.escape_seconds", pet.escapeTicks / 20);
        if (!pet.hasCollar) lockedReason = Text.translatable("gui.bondofthebeast.staff.no_collar_suffix");
        if (!pet.isOnline) lockedReason = Text.translatable("gui.bondofthebeast.staff.offline");
        Text tooltip = fullLabel.copy().append(Text.literal("\n")).append(
                Text.translatable("gui.bondofthebeast.control.description." + name).formatted(net.minecraft.util.Formatting.RESET));
        if (!unlocked) tooltip = tooltip.copy().append(Text.literal("\n")).append(lockedReason.copy().formatted(net.minecraft.util.Formatting.GRAY));
        var button = addDrawableChild(new GrimoireAbilityButton(x, y, book.size(60), book.size(32), label, value, icon, b -> {
            b.active = false;
            send(packet);
        }));
        button.active = unlocked;
        button.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(tooltip));
    }

    private void utility(int index, String name, String skill, String icon, ButtonWidget.PressAction press) {
        Text label = Text.translatable("gui.bondofthebeast." + name);
        var button = addDrawableChild(new GrimoireAbilityButton(book.x(180 + index % 3 * 64),
                book.y(52 + index / 3 * 34), book.size(60), book.size(32), label, null, icon, press));
        button.active = available(skill);
        Text tooltip = Text.empty().append(label.copy().formatted(net.minecraft.util.Formatting.GOLD))
                .append(Text.literal("\n")).append(Text.translatable("gui.bondofthebeast." + name + ".description"));
        if (!button.active) {
            Text reason = !pet.isOnline ? Text.translatable("gui.bondofthebeast.staff.offline") :
                    !pet.hasCollar ? Text.translatable("gui.bondofthebeast.staff.no_collar_suffix") :
                    pet.escapeTicks > 0 ? Text.translatable("gui.bondofthebeast.escape_seconds", pet.escapeTicks / 20) :
                    pet.forced ? Text.translatable("gui.bondofthebeast.requires_stage", BondRules.requiredStage(skill)) :
                    Text.translatable("gui.bondofthebeast.requires_trust",
                            Text.translatable("gui.bondofthebeast.trust." + VoluntaryBondService.requiredTier(skill)));
            tooltip = tooltip.copy().append(Text.literal("\n")).append(reason.copy().formatted(net.minecraft.util.Formatting.GRAY));
        }
        button.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(tooltip));
    }

    private void send(Identifier packet) {
        var buffer = PacketByteBufs.create();
        buffer.writeUuid(pet.uuid);
        ClientPlayNetworking.send(packet, buffer);
    }

    @Override public void close() { client.setScreen(parent); }

    @Override public void tick() {
        PetPreview.tick(preview);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GrimoireStyle.drawBook(context, width, height);
        drawBondProgress(context);
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(book.x(), book.y(), 0);
        matrices.scale(book.scale(), book.scale(), 1);
        GrimoireStyle.leather(context, 42, 42, 116, 19, false);
        GrimoireStyle.fittedText(context, textRenderer, Text.literal(pet.name.split("\\|")[0]).formatted(net.minecraft.util.Formatting.BOLD), 100, 47, 96, 0xFFF1DCA7);
        Text status = !pet.isOnline ? Text.translatable("gui.bondofthebeast.staff.offline") : !pet.hasCollar ? Text.translatable("gui.bondofthebeast.staff.no_collar_suffix") :
                pet.escapeTicks > 0 ? Text.translatable("gui.bondofthebeast.escape_seconds", pet.escapeTicks / 20) :
                        pet.forced ? Text.translatable(pet.formStage < 0 ? "gui.bondofthebeast.human_stage.short" : "gui.bondofthebeast.stage", pet.formStage) :
                                Text.translatable(pet.trustTier >= 3 ? "gui.bondofthebeast.bond_complete.short" : "gui.bondofthebeast.bond_time.remaining",
                                        VoluntaryBondService.remainingSeconds(pet.trustTier, pet.activeTicks) / 3600,
                                        VoluntaryBondService.remainingSeconds(pet.trustTier, pet.activeTicks) / 60 % 60);
        if (preview != null) {
            context.enableScissor(book.x(54), book.y(68), book.x(147), book.y(147));
            int modelScale = Math.min(PetPreview.isFeral(preview) ? 40 : 28,
                    Math.max(10, (int) ((PetPreview.isFeral(preview) ? 80 : 60) / Math.max(1, preview.getHeight()))));
            boolean looking = mouseX >= book.x(54) && mouseX < book.x(147) && mouseY >= book.y(68) && mouseY < book.y(147);
            PetPreview.draw(context, preview, 100, 145, modelScale,
                    looking ? net.minecraft.util.math.MathHelper.clamp((book.x(100) - mouseX) / book.scale(), -20, 20) : 0,
                    looking ? net.minecraft.util.math.MathHelper.clamp((book.y(106) - mouseY) / book.scale(), -10, 10) : 0);
            context.disableScissor();
        }
        GrimoireStyle.fittedText(context, textRenderer,
                Text.translatable("gui.bondofthebeast.staff.level", pet.bondLevel), 100, 153, 130, 0xFFEAD4A2);
        GrimoireStyle.leather(context, 31, 188, 136, 21, false);
        GrimoireStyle.fittedText(context, textRenderer,
                Text.translatable(pet.forced ? "gui.bondofthebeast.automatic_unlocks.short" :
                        pet.trustTier >= 3 ? "gui.bondofthebeast.bond_complete.title" : "gui.bondofthebeast.bond_time.title"),
                99, 190, 126, 0xFFEAD4A2, 1.0F);
        GrimoireStyle.fittedText(context, textRenderer, status, 99, 199, 126, 0xFFBDA77B, 1.0F);
        matrices.pop();
        super.render(context, mouseX, mouseY, delta);
        if (mouseX >= book.x(54) && mouseX < book.x(147) && mouseY >= book.y(68) && mouseY < book.y(147)) {
            context.drawOrderedTooltip(textRenderer, textRenderer.wrapLines(Text.translatable("gui.bondofthebeast.escape_rule." +
                    (!pet.forced || pet.formStage >= 3 ? "contract" : pet.formStage == 2 ? "enchanted" : "apple")),
                    Math.min(200, width - 24)), mouseX, mouseY);
        }
    }

    private void drawBondProgress(DrawContext context) {
        int maxExp = Math.max(1, pet.bondLevel * 100);
        int filled = pet.bondLevel >= 100 ? 98 : Math.max(0, Math.min(98, (pet.bondExp * 98) / maxExp));
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(book.x(), book.y(), 0);
        // Match the backdrop's actual size; these are the slot's native texture coordinates.
        matrices.scale(book.size(384) / 256.0F, book.size(216) / 200.0F, 1);
        context.fill(17, 164, 17 + filled, 173, 0xFF754249);
        matrices.pop();
    }

    private net.minecraft.client.network.OtherClientPlayerEntity previewEntity() {
        return PetPreview.create(client, pet);
    }

}
