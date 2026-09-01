package com.bondofthebeast.client;

import com.bondofthebeast.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PetBedScreen extends Screen {
    private final BlockPos pos;
    private final Map<String, PetOption> registeredPets;
    private String selectedUUID;
    private int currentRadius;
    private SliderWidget slider;
    private ButtonWidget mainPetButton;
    private ButtonWidget doneButton;
    private final List<ButtonWidget> dropdownButtons = new ArrayList<>();
    private boolean isDropdownOpen = false;

    public PetBedScreen(BlockPos pos, String petUUID, int radius, Map<String, PetOption> pets) {
        super(Text.empty());
        this.pos = pos;
        this.selectedUUID = petUUID;
        this.currentRadius = radius;
        this.registeredPets = pets;
    }

    @Override
    protected void init() {
        super.init();
        dropdownButtons.clear();
        int x = width / 2, y = height / 2;
        List<String> keys = new ArrayList<>();
        keys.add("");
        keys.addAll(registeredPets.keySet());
        if (!keys.contains(selectedUUID)) selectedUUID = "";

        mainPetButton = ButtonWidget.builder(getPetText(), b -> { isDropdownOpen = !isDropdownOpen; updateVisibility(); }).dimensions(x - 100, y - 60, 200, 20).build();
        addDrawableChild(mainPetButton);

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String name = key.isEmpty() ? Text.translatable("gui.bondofthebeast.bed.nobody").getString() : registeredPets.get(key).name;
            ButtonWidget dropBtn = ButtonWidget.builder(Text.literal(name), b -> { selectedUUID = key; isDropdownOpen = false; updateVisibility(); }).dimensions(x - 100, y - 38 + (i * 20), 200, 20).build();
            dropdownButtons.add(dropBtn);
            addDrawableChild(dropBtn);
        }

        slider = new SliderWidget(x - 100, y - 10, 200, 20, getRadiusText(currentRadius), currentRadius / 50f) {
            @Override protected void updateMessage() { int val = (int)(this.value * 50); setMessage(getRadiusText(val)); }
            @Override protected void applyValue() { currentRadius = (int)(this.value * 50); }
        };
        slider.setTooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.bed.radius_tooltip")));
        addDrawableChild(slider);

        doneButton = ButtonWidget.builder(Text.translatable("gui.bondofthebeast.bed.done").formatted(Formatting.GREEN), b -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos); buf.writeString(selectedUUID); buf.writeInt(currentRadius);
            ClientPlayNetworking.send(ModPackets.UPDATE_BED_C2S, buf);
            this.close();
        }).dimensions(x - 100, y + 30, 200, 20).build();
        addDrawableChild(doneButton);
        updateVisibility();
    }

    private Text getRadiusText(int val) {
        String valStr = val == 0 ? Text.translatable("gui.bondofthebeast.bed.unlimited").getString() : val + " " + Text.translatable("gui.bondofthebeast.bed.blocks").getString();
        return Text.translatable("gui.bondofthebeast.bed.radius", valStr);
    }

    private Text getPetText() {
        String name = selectedUUID.isEmpty() ? Text.translatable("gui.bondofthebeast.bed.nobody").getString() : registeredPets.getOrDefault(selectedUUID, new PetOption("", true)).name;
        return Text.translatable("gui.bondofthebeast.bed.pet", name + (isDropdownOpen ? " \u25B2" : " \u25BC"));
    }

    private void updateVisibility() {
        mainPetButton.setMessage(getPetText());
        for (ButtonWidget btn : dropdownButtons) { btn.visible = isDropdownOpen; btn.active = isDropdownOpen; }
        slider.visible = !isDropdownOpen; slider.active = !isDropdownOpen && !selectedUUID.isEmpty();
        doneButton.visible = !isDropdownOpen; doneButton.active = !isDropdownOpen;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        if (!isDropdownOpen) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.bondofthebeast.bed.select_pet").formatted(Formatting.GOLD), width / 2, height / 2 - 80, 0xFFFFFF);
            if (!selectedUUID.isEmpty() && !registeredPets.get(selectedUUID).hasCollar) {
                context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.bondofthebeast.bed.no_collar").formatted(Formatting.RED), width / 2, height / 2 + 15, 0xFFFFFF);
            }
        } else context.drawCenteredTextWithShadow(textRenderer, Text.empty(), width / 2, height / 2 - 80, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    public static class PetOption { public final String name; public final boolean hasCollar; public PetOption(String n, boolean h) { name = n; hasCollar = h; } }
}