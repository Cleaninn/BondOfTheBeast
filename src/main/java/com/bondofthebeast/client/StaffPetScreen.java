package com.bondofthebeast.client;

import com.bondofthebeast.BondRules;
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
    private StaffMainScreen.PetData pet;
    private int refreshTicks;
    private final java.util.List<Runnable> controlUpdates = new java.util.ArrayList<>();
    private GrimoireStyle.BookLayout book;
    private net.minecraft.client.network.OtherClientPlayerEntity preview;

    public StaffPetScreen(Screen parent, StaffMainScreen.PetData pet) {
        super(Text.translatable("gui.bondofthebeast.control_title"));
        this.parent = parent;
        this.pet = pet;
    }

    @Override protected void init() {
        controlUpdates.clear();
        book = GrimoireStyle.layout(width, height);
        preview = previewEntity();
        action(0, "sit", pet.isSitting, ModPackets.TOGGLE_PET_STATE_C2S, "sit");
        action(1, "tp", pet.isTeleportEnabled, ModPackets.TOGGLE_TELEPORT_C2S, "tp");
        action(2, "prot", pet.isProtectionMode, ModPackets.TOGGLE_PROTECTION_C2S, "prot");
        action(3, "aura", pet.isAuraEnabled, ModPackets.TOGGLE_AURA_C2S, "aura");
        action(4, "vampiric", pet.isVampiricMode, ModPackets.TOGGLE_VAMPIRIC_C2S, "vampiric");
        action(5, "absorb", pet.isAbsorbed, ModPackets.TOGGLE_ABSORB_C2S, "absorb");
        action(6, "armor", pet.armorLocked, ModPackets.TOGGLE_ARMOR_C2S, "armor_lock", "armor_lock");
        utility(7, "armor_open", "armor", "armor", b -> send(ModPackets.OPEN_ARMOR_C2S));
        action(8, "inventory", pet.itemForwarding, ModPackets.TOGGLE_ITEM_FORWARDING_C2S, "inventory");
        action(12, "nobreak", pet.isNoBreakMode, ModPackets.TOGGLE_NO_BREAK_C2S, "nobreak");
        action(13, "nobreak", pet.isNoInteractMode, ModPackets.TOGGLE_NO_INTERACT_C2S, "interact", "interact");
        action(14, "nobreak", pet.isPacifistMode, ModPackets.TOGGLE_PACIFIST_C2S, "pacifist", "pacifist");
        utility(15, "block_rules", "nobreak", "blocks", b -> client.setScreen(new BlockRulesScreen(this, pet)));
        addDrawableChild(new GrimoireBackButton(book.x(344), book.y(16), book.size(22),
                Text.translatable(parent == null ? "gui.bondofthebeast.staff.back_tooltip" :
                        "gui.bondofthebeast.back_to_pets"), b -> close()));
    }

    private int buttonX(int index) { return index >= 12 ? 180 + (index - 12) * 48 : 180 + index % 3 * 64; }
    private int buttonY(int index) { return index >= 12 ? 164 : 48 + index / 3 * 32; }
    private int buttonWidth(int index) { return index >= 12 ? 44 : 60; }

    private boolean available(String skill) {
        return pet.isOnline && pet.hasCollar && pet.escapeTicks == 0 && pet.unlockedSkills.contains(skill);
    }

    private void action(int index, String skill, boolean value, Identifier packet, String icon) {
        action(index, skill, value, packet, icon, skill);
    }

    private void action(int index, String skill, boolean value, Identifier packet, String icon, String name) {
        int x = book.x(buttonX(index));
        int y = book.y(buttonY(index));
        Text label = Text.translatable("gui.bondofthebeast.control." + name);
        var button = addDrawableChild(new GrimoireAbilityButton(x, y, book.size(buttonWidth(index)), book.size(28), label, value, icon, b -> {
            b.active = false;
            send(packet);
        }));
        controlUpdates.add(() -> {
            boolean unlocked = available(skill);
            Text state = Text.translatable(value ? "gui.bondofthebeast.on" : "gui.bondofthebeast.off");
            Text fullLabel = Text.empty().append(label.copy().formatted(net.minecraft.util.Formatting.GOLD)).append(Text.literal(" — ")).append(state);
            Text lockedReason = Text.translatable("gui.bondofthebeast.requires_stage",
                    BondRules.requiredStage(skill) + 1);
            if (!pet.forced) lockedReason = Text.translatable("gui.bondofthebeast.requires_full_form");
            if (pet.escapeTicks > 0) lockedReason = Text.translatable("gui.bondofthebeast.escape_seconds", (pet.escapeTicks + 19) / 20);
            if (!pet.hasCollar) lockedReason = Text.translatable("gui.bondofthebeast.staff.no_collar_suffix");
            if (!pet.isOnline) lockedReason = Text.translatable("gui.bondofthebeast.staff.offline");
            Text tooltip = fullLabel.copy().append(Text.literal("\n")).append(
                    Text.translatable("gui.bondofthebeast.control.description." + name).formatted(net.minecraft.util.Formatting.RESET));
            if (!unlocked) tooltip = tooltip.copy().append(Text.literal("\n")).append(lockedReason.copy().formatted(net.minecraft.util.Formatting.GRAY));
            button.active = unlocked;
            button.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(tooltip));
        });
        controlUpdates.get(controlUpdates.size() - 1).run();
    }

    private void utility(int index, String name, String skill, String icon, ButtonWidget.PressAction press) {
        Text label = Text.translatable("gui.bondofthebeast." + name);
        var button = addDrawableChild(new GrimoireAbilityButton(book.x(buttonX(index)),
                book.y(buttonY(index)), book.size(buttonWidth(index)), book.size(28), label, null, icon, press));
        controlUpdates.add(() -> {
            button.active = available(skill);
            Text tooltip = Text.empty().append(label.copy().formatted(net.minecraft.util.Formatting.GOLD))
                    .append(Text.literal("\n")).append(Text.translatable("gui.bondofthebeast." + name + ".description"));
            if (!button.active) {
                Text reason = !pet.isOnline ? Text.translatable("gui.bondofthebeast.staff.offline") :
                        !pet.hasCollar ? Text.translatable("gui.bondofthebeast.staff.no_collar_suffix") :
                        pet.escapeTicks > 0 ? Text.translatable("gui.bondofthebeast.escape_seconds", (pet.escapeTicks + 19) / 20) :
                        pet.forced ? Text.translatable("gui.bondofthebeast.requires_stage", BondRules.requiredStage(skill) + 1) :
                        Text.translatable("gui.bondofthebeast.requires_full_form");
                tooltip = tooltip.copy().append(Text.literal("\n")).append(reason.copy().formatted(net.minecraft.util.Formatting.GRAY));
            }
            button.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(tooltip));
        });
        controlUpdates.get(controlUpdates.size() - 1).run();
    }

    private void send(Identifier packet) {
        var buffer = PacketByteBufs.create();
        buffer.writeUuid(pet.uuid);
        ClientPlayNetworking.send(packet, buffer);
    }

    public boolean showsPet(java.util.UUID id) { return pet.uuid.equals(id); }

    public void updatePet(StaffMainScreen.PetData data) {
        if (!pet.uuid.equals(data.uuid)) return;
        boolean rebuild = pet.isSitting != data.isSitting || pet.isTeleportEnabled != data.isTeleportEnabled ||
                pet.isProtectionMode != data.isProtectionMode || pet.isAuraEnabled != data.isAuraEnabled ||
                pet.isVampiricMode != data.isVampiricMode || pet.isNoBreakMode != data.isNoBreakMode ||
                pet.isNoInteractMode != data.isNoInteractMode || pet.isPacifistMode != data.isPacifistMode ||
                pet.isAbsorbed != data.isAbsorbed || pet.armorLocked != data.armorLocked ||
                pet.itemForwarding != data.itemForwarding || pet.forced != data.forced || !pet.formId.equals(data.formId);
        pet = data;
        if (rebuild) clearAndInit();
        else controlUpdates.forEach(Runnable::run);
    }

    @Override public void close() { client.setScreen(parent); }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { client.setScreen(null); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void tick() {
        PetPreview.tick(preview);
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            send(ModPackets.REQUEST_PET_STATUS_C2S);
        }
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GrimoireStyle.drawBook(context, width, height);
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(book.x(), book.y(), 0);
        matrices.scale(book.scale(), book.scale(), 1);
        GrimoireStyle.leather(context, 42, 42, 116, 19, false);
        GrimoireStyle.fittedText(context, textRenderer, Text.literal(pet.name.split("\\|")[0]).formatted(net.minecraft.util.Formatting.BOLD), 100, 47, 96, 0xFFF1DCA7);
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
        Text stage = !pet.isOnline ? Text.translatable("gui.bondofthebeast.staff.offline") :
                pet.formStage < 0 ? Text.translatable("gui.bondofthebeast.human_stage.short") :
                Text.translatable("gui.bondofthebeast.stage", pet.formStage + 1);
        GrimoireStyle.paper(context, 45, 159, 110, 24);
        context.drawBorder(45, 159, 110, 24, 0xFFC3A46D);
        GrimoireStyle.fittedText(context, textRenderer, stage.copy().formatted(net.minecraft.util.Formatting.BOLD),
                100, 167, 98, GrimoireStyle.INK);
        if (pet.escapeTicks > 0) GrimoireStyle.fittedText(context, textRenderer,
                Text.translatable("gui.bondofthebeast.escape_seconds", (pet.escapeTicks + 19) / 20), 100, 189, 130, 0xFFEAD4A2);
        context.drawBorder(176, 143, 196, 54, 0xFF976B54);
        GrimoireStyle.fittedText(context, textRenderer, Text.translatable("gui.bondofthebeast.restrictions"),
                274, 149, 180, 0xFFEAD4A2);
        matrices.pop();
        super.render(context, mouseX, mouseY, delta);
    }

    private net.minecraft.client.network.OtherClientPlayerEntity previewEntity() {
        return PetPreview.create(client, pet);
    }

}
