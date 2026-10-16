package com.bondofthebeast.client;

import com.bondofthebeast.ModPackets;
import com.bondofthebeast.block.PetBedBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PetBedScreen extends Screen {
    private static final int VISIBLE_PETS = 4;
    private final BlockPos pos;
    private final Map<String, PetOption> registeredPets;
    private final List<String> keys = new ArrayList<>();
    private String selectedUUID;
    private int currentRadius, listOffset;
    private boolean dropdownOpen;
    private GrimoireStyle.PanelLayout panel;
    private TextFieldWidget radiusField;
    private RadiusSlider radiusSlider;
    private final String initialPetUUID;
    private final int initialRadius;
    private final long initialChainSeconds;
    private final long openedAt = Util.getMeasuringTimeMs();

    public PetBedScreen(BlockPos pos, String petUUID, int radius, Map<String, PetOption> pets) {
        this(pos, petUUID, radius, -1, pets);
    }

    public PetBedScreen(BlockPos pos, String petUUID, int radius, long chainSeconds, Map<String, PetOption> pets) {
        super(Text.translatable("gui.bondofthebeast.bed.title"));
        this.pos = pos;
        selectedUUID = petUUID;
        currentRadius = Math.max(0, radius);
        registeredPets = pets;
        initialPetUUID = petUUID;
        initialRadius = radius;
        initialChainSeconds = chainSeconds;
        keys.add("");
        pets.keySet().stream().sorted(Comparator.comparing(key -> pets.get(key).name)).forEach(keys::add);
    }

    @Override protected void init() {
        panel = GrimoireStyle.panel(width, height, 276, 222);
        radiusField = null;
        radiusSlider = null;
        if (!keys.contains(selectedUUID)) selectedUUID = "";
        addDrawableChild(new GrimoireButtonWidget(panel.x(18), panel.y(54), panel.size(240), panel.size(24),
                getPetText(), button -> { dropdownOpen = !dropdownOpen; clearAndInit(); }));
        if (dropdownOpen) {
            listOffset = Math.max(0, Math.min(listOffset, Math.max(0, keys.size() - VISIBLE_PETS)));
            for (int row = 0; row < VISIBLE_PETS && listOffset + row < keys.size(); row++) {
                String key = keys.get(listOffset + row);
                Text label = key.isEmpty() ? Text.translatable("gui.bondofthebeast.bed.nobody") : Text.literal(registeredPets.get(key).name);
                var option = addDrawableChild(new GrimoireButtonWidget(panel.x(18), panel.y(86 + row * 24), panel.size(240), panel.size(22),
                        label, button -> { selectedUUID = key; dropdownOpen = false; clearAndInit(); }));
                option.setTooltip(Tooltip.of(label));
            }
            var previous = addDrawableChild(new GrimoireButtonWidget(panel.x(18), panel.y(194), panel.size(24), panel.size(20),
                    Text.literal("<"), button -> { listOffset = Math.max(0, listOffset - VISIBLE_PETS); clearAndInit(); }));
            previous.active = listOffset > 0;
            var next = addDrawableChild(new GrimoireButtonWidget(panel.x(234), panel.y(194), panel.size(24), panel.size(20),
                    Text.literal(">"), button -> { listOffset = Math.min(keys.size() - VISIBLE_PETS, listOffset + VISIBLE_PETS); clearAndInit(); }));
            next.active = listOffset + VISIBLE_PETS < keys.size();
            return;
        }
        radiusSlider = addDrawableChild(new RadiusSlider());
        radiusField = addDrawableChild(new TextFieldWidget(textRenderer, panel.x(184), panel.y(117), panel.size(69), panel.size(12), Text.translatable("gui.bondofthebeast.bed.custom_radius")));
        radiusField.setDrawsBackground(false);
        radiusField.setEditableColor(0xFFEEDDBB);
        radiusField.setUneditableColor(0xFF998575);
        radiusField.setMaxLength(10);
        radiusField.setTextPredicate(value -> {
            if (value.isEmpty()) return true;
            if (!value.matches("[0-9]+")) return false;
            try { return Long.parseLong(value) <= Integer.MAX_VALUE; }
            catch (NumberFormatException ignored) { return false; }
        });
        radiusField.setText(Integer.toString(currentRadius));
        radiusField.setChangedListener(value -> {
            currentRadius = value.isEmpty() ? 0 : Integer.parseInt(value);
            radiusSlider.setRadius(currentRadius);
        });
        radiusSlider.active = radiusField.active = !selectedUUID.isEmpty();
        addDrawableChild(new GrimoireButtonWidget(panel.x(18), panel.y(194), panel.size(240), panel.size(20),
                Text.translatable("gui.bondofthebeast.bed.done"), button -> {
            var buffer = PacketByteBufs.create();
            buffer.writeBlockPos(pos);
            buffer.writeString(selectedUUID);
            buffer.writeInt(currentRadius);
            ClientPlayNetworking.send(ModPackets.UPDATE_BED_C2S, buffer);
            close();
        }));
    }

    private final class RadiusSlider extends SliderWidget {
        RadiusSlider() { super(panel.x(18), panel.y(108), panel.size(154), panel.size(26), Text.empty(), Math.min(1, currentRadius / 50.0)); }
        void setRadius(int radius) { value = Math.min(1, radius / 50.0); }
        @Override protected void updateMessage() {}
        @Override protected void applyValue() {
            currentRadius = (int) Math.round(value * 50);
            if (radiusField != null) radiusField.setText(Integer.toString(currentRadius));
        }
        @Override public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            int centerY = getY() + height / 2;
            context.fill(getX() + 3, centerY - 2, getX() + width - 3, centerY + 2, 0xFF6C5045);
            context.fill(getX() + 4, centerY - 1, getX() + width - 4, centerY + 1, 0xFF382A2A);
            for (int i = 0; i <= 5; i++) {
                int tickX = getX() + 4 + (width - 9) * i / 5;
                context.fill(tickX, centerY + 5, tickX + 1, centerY + 7, 0xFF907454);
            }
            int knobX = getX() + 2 + (int) ((width - 12) * value);
            GrimoireStyle.leather(context, knobX, centerY - 6, 9, 12, active && (isHovered() || isFocused()));
        }
    }

    private Text getPetText() {
        String name = selectedUUID.isEmpty() ? Text.translatable("gui.bondofthebeast.bed.nobody").getString() : registeredPets.get(selectedUUID).name;
        return Text.literal(name + (dropdownOpen ? "  <" : "  >"));
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!dropdownOpen || mouseX < panel.left() || mouseX >= panel.x(276) || mouseY < panel.y(80) || mouseY >= panel.y(212))
            return super.mouseScrolled(mouseX, mouseY, amount);
        listOffset = Math.max(0, Math.min(Math.max(0, keys.size() - VISIBLE_PETS), listOffset + (amount > 0 ? -1 : 1)));
        clearAndInit();
        return true;
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.getMatrices().push();
        context.getMatrices().translate(panel.left(), panel.top(), 0);
        context.getMatrices().scale(panel.scale(), panel.scale(), 1);
        context.fill(3, 4, 279, 226, 0x990C090B);
        context.fill(0, 0, 276, 222, 0xFF2D2023);
        context.drawBorder(0, 0, 276, 222, 0xFF8E7051);
        context.drawBorder(3, 3, 270, 216, 0xFF4E3234);
        context.fill(8, 34, 268, 188, 0xFFE5D3AB);
        context.fill(9, 35, 267, 36, 0xFFF5E4BE);
        context.fill(16, 30, 260, 31, 0xFF9D7F58);
        context.fill(16, 31, 260, 32, 0xFF4B3031);
        GrimoireStyle.fittedText(context, textRenderer, title, 138, 13, 236, 0xFFF0DCAA);
        GrimoireStyle.fittedText(context, textRenderer, Text.translatable("gui.bondofthebeast.bed.assignment"), 138, 42, 236, GrimoireStyle.MUTED_INK, 0.85F);
        if (dropdownOpen) {
            GrimoireStyle.fittedText(context, textRenderer, Text.literal((listOffset + 1) + "-" + Math.min(keys.size(), listOffset + VISIBLE_PETS) + " / " + keys.size()),
                    138, 199, 170, 0xFFEAD6AD);
        } else {
            context.fill(18, 87, 258, 88, 0xFFBDA47A);
            GrimoireStyle.fittedText(context, textRenderer, Text.translatable("gui.bondofthebeast.bed.chain"), 138, 94, 232, GrimoireStyle.INK);
            context.fill(178, 108, 258, 134, 0xFF35272A);
            context.drawBorder(178, 108, 80, 26, radiusField.isFocused() ? 0xFFBC875D : 0xFF94734E);
            context.fill(180, 110, 256, 111, 0xFF5C4140);
            GrimoireStyle.wrappedText(context, textRenderer, Text.translatable("gui.bondofthebeast.bed.radius_tooltip"),
                    138, 141, 236, 20, GrimoireStyle.MUTED_INK);
            Text duration = Text.translatable("gui.bondofthebeast.bed.unlimited");
            if (!selectedUUID.isEmpty() && currentRadius > 0) {
                long seconds = PetBedBlockEntity.chainDurationSeconds(currentRadius);
                if (initialPetUUID.equals(selectedUUID) && initialChainSeconds > 0) {
                    long elapsed = PetBedBlockEntity.chainDurationSeconds(initialRadius) - initialChainSeconds;
                    seconds = Math.max(0, seconds - elapsed - (Util.getMeasuringTimeMs() - openedAt) / 1000);
                }
                String time = seconds >= 3600 ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                        : String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                duration = registeredPets.get(selectedUUID).temporary ? Text.translatable("gui.bondofthebeast.bed.chain_duration", time)
                        : Text.empty();
            }
            if (!selectedUUID.isEmpty() && !registeredPets.get(selectedUUID).hasCollar)
                duration = Text.translatable("gui.bondofthebeast.bed.no_collar");
            if (!duration.getString().isEmpty()) {
                context.fill(16, 164, 260, 183, 0x183A2625);
                GrimoireStyle.fittedText(context, textRenderer, duration, 138, 170, 232, GrimoireStyle.INK, 0.85F);
            }
        }
        context.getMatrices().pop();
        super.render(context, mouseX, mouseY, delta);
    }

    public static class PetOption {
        public final String name;
        public final boolean hasCollar, temporary;
        public PetOption(String name, boolean hasCollar) { this(name, hasCollar, false); }
        public PetOption(String name, boolean hasCollar, boolean temporary) { this.name = name; this.hasCollar = hasCollar; this.temporary = temporary; }
    }
}
