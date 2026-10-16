package com.bondofthebeast.client;

import com.bondofthebeast.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
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
    private final int initialRadius, initialChainSeconds;
    private final long openedAt = net.minecraft.util.Util.getMeasuringTimeMs();

    public PetBedScreen(BlockPos pos, String petUUID, int radius, Map<String, PetOption> pets) {
        this(pos, petUUID, radius, -1, pets);
    }

    public PetBedScreen(BlockPos pos, String petUUID, int radius, int chainSeconds, Map<String, PetOption> pets) {
        super(Text.translatable("gui.bondofthebeast.bed.title"));
        this.pos = pos;
        this.selectedUUID = petUUID;
        this.currentRadius = radius;
        this.registeredPets = pets;
        initialPetUUID = petUUID;
        initialRadius = radius;
        initialChainSeconds = chainSeconds;
        keys.add("");
        pets.keySet().stream().sorted(Comparator.comparing(key -> pets.get(key).name)).forEach(keys::add);
    }

    @Override protected void init() {
        panel = GrimoireStyle.panel(width, height, 260, 194);
        if (!keys.contains(selectedUUID)) selectedUUID = "";
        var select = addDrawableChild(new GrimoireButtonWidget(panel.x(14), panel.y(38), panel.size(232), panel.size(22),
                getPetText(), button -> { dropdownOpen = !dropdownOpen; clearAndInit(); }));
        select.setTooltip(Tooltip.of(getPetText()));
        if (dropdownOpen) {
            listOffset = Math.max(0, Math.min(listOffset, Math.max(0, keys.size() - VISIBLE_PETS)));
            for (int row = 0; row < VISIBLE_PETS && listOffset + row < keys.size(); row++) {
                String key = keys.get(listOffset + row);
                Text label = key.isEmpty() ? Text.translatable("gui.bondofthebeast.bed.nobody") : Text.literal(registeredPets.get(key).name);
                var option = addDrawableChild(new GrimoireButtonWidget(panel.x(14), panel.y(68 + row * 22), panel.size(232), panel.size(20),
                        label, button -> { selectedUUID = key; dropdownOpen = false; clearAndInit(); }));
                option.setTooltip(Tooltip.of(label));
            }
            var previous = addDrawableChild(new GrimoireButtonWidget(panel.x(14), panel.y(164), panel.size(24), panel.size(20),
                    Text.literal("<"), button -> { listOffset = Math.max(0, listOffset - VISIBLE_PETS); clearAndInit(); }));
            previous.active = listOffset > 0;
            var next = addDrawableChild(new GrimoireButtonWidget(panel.x(222), panel.y(164), panel.size(24), panel.size(20),
                    Text.literal(">"), button -> { listOffset = Math.min(keys.size() - VISIBLE_PETS, listOffset + VISIBLE_PETS); clearAndInit(); }));
            next.active = listOffset + VISIBLE_PETS < keys.size();
        } else {
            radiusField = addDrawableChild(new TextFieldWidget(textRenderer, panel.x(204), panel.y(84), panel.size(38), panel.size(12), Text.translatable("gui.bondofthebeast.bed.custom_radius")));
            radiusField.setDrawsBackground(false);
            radiusField.setEditableColor(GrimoireStyle.INK);
            radiusField.setMaxLength(2);
            radiusField.setTextPredicate(value -> value.isEmpty() || value.matches("[0-9]{1,2}") && Integer.parseInt(value) <= 50);
            radiusField.setText(Integer.toString(currentRadius));
            radiusField.setChangedListener(value -> {
                if (!value.isEmpty()) { currentRadius = Integer.parseInt(value); radiusSlider.setRadius(currentRadius); }
            });
            radiusField.setTooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.bed.custom_radius")));
            radiusSlider = addDrawableChild(new RadiusSlider());
            radiusSlider.active = !selectedUUID.isEmpty();
            radiusField.active = !selectedUUID.isEmpty();
            radiusSlider.setTooltip(Tooltip.of(Text.translatable("gui.bondofthebeast.bed.radius_tooltip")));
            addDrawableChild(new GrimoireButtonWidget(panel.x(14), panel.y(164), panel.size(232), panel.size(20),
                    Text.translatable("gui.bondofthebeast.bed.done"), button -> {
                var buffer = PacketByteBufs.create();
                buffer.writeBlockPos(pos);
                buffer.writeString(selectedUUID);
                buffer.writeInt(currentRadius);
                ClientPlayNetworking.send(ModPackets.UPDATE_BED_C2S, buffer);
                close();
            }));
        }
    }

    private final class RadiusSlider extends SliderWidget {
        RadiusSlider() { super(panel.x(14), panel.y(76), panel.size(178), panel.size(24), getRadiusText(currentRadius), currentRadius / 50.0); }
        void setRadius(int radius) { value = radius / 50.0; updateMessage(); }
        @Override protected void updateMessage() { setMessage(getRadiusText((int) Math.round(value * 50))); }
        @Override protected void applyValue() {
            currentRadius = (int) Math.round(value * 50);
            if (radiusField != null) radiusField.setText(Integer.toString(currentRadius));
        }
        @Override protected net.minecraft.client.gui.tooltip.TooltipPositioner getTooltipPositioner() {
            return GrimoireStyle.TOOLTIP_POSITIONER;
        }
        @Override public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            GrimoireStyle.paper(context, getX(), getY(), width, height);
            context.fill(getX() + 5, getY() + height - 7, getX() + width - 5, getY() + height - 5, 0xFFAA8B5C);
            int knobX = getX() + 4 + (int) ((width - 14) * value);
            GrimoireStyle.leather(context, knobX, getY() + height - 10, 7, 7, isHovered() || isFocused());
            GrimoireStyle.fittedText(context, textRenderer, getMessage(), getX() + width / 2, getY() + 3, width - 16,
                    active ? GrimoireStyle.INK : GrimoireStyle.MUTED_INK, Math.min(1.0F, (height - 12) / 9.0F));
        }
    }

    private Text getRadiusText(int value) {
        String radius = value == 0 ? Text.translatable("gui.bondofthebeast.bed.unlimited").getString() : value + " " + Text.translatable("gui.bondofthebeast.bed.blocks").getString();
        return Text.translatable("gui.bondofthebeast.bed.radius", radius);
    }

    private Text getPetText() {
        String name = selectedUUID.isEmpty() ? Text.translatable("gui.bondofthebeast.bed.nobody").getString() : registeredPets.get(selectedUUID).name;
        return Text.translatable("gui.bondofthebeast.bed.pet", name + (dropdownOpen ? " <" : " >"));
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!dropdownOpen || mouseX < panel.left() || mouseX >= panel.x(260) || mouseY < panel.y(64) || mouseY >= panel.y(184))
            return super.mouseScrolled(mouseX, mouseY, amount);
        listOffset = Math.max(0, Math.min(Math.max(0, keys.size() - VISIBLE_PETS), listOffset + (amount > 0 ? -1 : 1)));
        clearAndInit();
        return true;
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        GrimoireStyle.beginPanel(context, panel);
        GrimoireStyle.fittedText(context, textRenderer, title, 130, 12, 224, 0xFFF0DCAA);
        if (dropdownOpen) {
            GrimoireStyle.fittedText(context, textRenderer, Text.literal((listOffset + 1) + "-" + Math.min(keys.size(), listOffset + VISIBLE_PETS) + " / " + keys.size()),
                    130, 169, 156, GrimoireStyle.INK, 1.0F);
        } else {
            GrimoireStyle.paper(context, 200, 76, 46, 24);
            GrimoireStyle.wrappedText(context, textRenderer, Text.translatable("gui.bondofthebeast.bed.radius_tooltip"),
                    130, 107, 226, 22, GrimoireStyle.MUTED_INK);
            if (!selectedUUID.isEmpty() && currentRadius > 0) {
                PetOption pet = registeredPets.get(selectedUUID);
                int seconds = com.bondofthebeast.block.PetBedBlockEntity.chainDurationSeconds(currentRadius);
                if (initialPetUUID.equals(selectedUUID) && initialChainSeconds > 0) {
                    int elapsed = com.bondofthebeast.block.PetBedBlockEntity.chainDurationSeconds(initialRadius) - initialChainSeconds;
                    seconds = Math.max(0, seconds - elapsed - (int) ((net.minecraft.util.Util.getMeasuringTimeMs() - openedAt) / 1000));
                }
                Text duration = pet.temporary ? Text.translatable("gui.bondofthebeast.bed.chain_duration", String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)) : Text.translatable("gui.bondofthebeast.bed.chain_permanent");
                GrimoireStyle.fittedText(context, textRenderer, duration, 130, 132, 226, GrimoireStyle.INK, 0.8F);
            }
            if (!selectedUUID.isEmpty() && !registeredPets.get(selectedUUID).hasCollar)
                GrimoireStyle.fittedText(context, textRenderer, Text.translatable("gui.bondofthebeast.bed.no_collar"), 130, 144, 226, UiColors.BORDER, 1.0F);
        }
        context.getMatrices().pop();
        super.render(context, mouseX, mouseY, delta);
    }

    public static class PetOption {
        public final String name;
        public final boolean hasCollar;
        public final boolean temporary;
        public PetOption(String name, boolean hasCollar) { this(name, hasCollar, false); }
        public PetOption(String name, boolean hasCollar, boolean temporary) { this.name = name; this.hasCollar = hasCollar; this.temporary = temporary; }
    }
}
