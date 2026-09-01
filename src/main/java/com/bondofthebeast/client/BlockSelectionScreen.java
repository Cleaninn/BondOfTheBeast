package com.bondofthebeast.client;

import com.bondofthebeast.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;

public class BlockSelectionScreen extends Screen {
    private final Screen parent;
    private final StaffMainScreen.PetData pet;
    private final int listType; // 0 = Black, 1 = White
    private final Set<String> currentList;

    private TextFieldWidget searchBox;
    private List<Block> allBlocks;
    private List<Block> filteredBlocks;
    private int scrollOffset = 0;

    private static final int COLUMNS = 9;
    private static final int ROWS = 5;

    public BlockSelectionScreen(Screen parent, StaffMainScreen.PetData pet, int listType) {
        super(Text.translatable(listType == 0 ? "gui.bondofthebeast.block_select.blacklist" : "gui.bondofthebeast.block_select.whitelist"));
        this.parent = parent;
        this.pet = pet;
        this.listType = listType;
        this.currentList = new HashSet<>(listType == 0 ? pet.blacklistedBlocks : pet.whitelistedBlocks);
    }

    @Override
    protected void init() {
        this.allBlocks = Registries.BLOCK.stream()
                .filter(b -> !b.getName().getString().contains("Air"))
                .sorted(Comparator.comparing(b -> b.getName().getString()))
                .collect(Collectors.toList());
        this.filteredBlocks = new ArrayList<>(allBlocks);

        this.searchBox = new TextFieldWidget(textRenderer, width / 2 - 100, 30, 200, 20, Text.empty());
        this.searchBox.setChangedListener(this::onSearch);
        addSelectableChild(this.searchBox);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.bondofthebeast.save"), b -> saveAndExit())
                .dimensions(width / 2 - 110, height - 30, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.bondofthebeast.cancel"), b -> this.client.setScreen(parent))
                .dimensions(width / 2 + 10, height - 30, 100, 20).build());
    }

    private void onSearch(String text) {
        this.filteredBlocks = allBlocks.stream()
                .filter(b -> b.getName().getString().toLowerCase().contains(text.toLowerCase()))
                .collect(Collectors.toList());
        this.scrollOffset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, Text.empty().append(this.title).formatted(Formatting.GOLD), width / 2, 10, 0xFFFFFF);
        this.searchBox.render(context, mouseX, mouseY, delta);

        int startX = width / 2 - (COLUMNS * 11);
        int startY = 60;

        for (int i = 0; i < COLUMNS * ROWS; i++) {
            int index = i + scrollOffset * COLUMNS;
            if (index >= filteredBlocks.size()) break;

            Block block = filteredBlocks.get(index);
            String id = Registries.BLOCK.getId(block).toString();
            int bx = startX + (i % COLUMNS) * 22;
            int by = startY + (i / COLUMNS) * 22;

            boolean isSelected = currentList.contains(id);

            context.fill(bx, by, bx + 20, by + 20, isSelected ? 0x8800FF00 : 0x44FFFFFF);
            if (isSelected) {
                context.drawBorder(bx - 1, by - 1, 22, 22, 0xFF00FF00);
            }

            context.drawItem(block.asItem().getDefaultStack(), bx + 2, by + 2);

            if (mouseX >= bx && mouseX <= bx + 20 && mouseY >= by && mouseY <= by + 20) {
                context.drawTooltip(textRenderer, block.getName(), mouseX, mouseY);
            }
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = width / 2 - (COLUMNS * 11);
        int startY = 60;

        for (int i = 0; i < COLUMNS * ROWS; i++) {
            int index = i + scrollOffset * COLUMNS;
            if (index >= filteredBlocks.size()) break;

            int bx = startX + (i % COLUMNS) * 22;
            int by = startY + (i / COLUMNS) * 22;

            if (mouseX >= bx && mouseX <= bx + 20 && mouseY >= by && mouseY <= by + 20) {
                String id = Registries.BLOCK.getId(filteredBlocks.get(index)).toString();
                if (currentList.contains(id)) {
                    currentList.remove(id);
                } else {
                    currentList.add(id);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount > 0 && scrollOffset > 0) scrollOffset--;
        else if (amount < 0 && (scrollOffset + ROWS) * COLUMNS < filteredBlocks.size()) scrollOffset++;
        return true;
    }

    private void saveAndExit() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(pet.uuid);
        buf.writeInt(listType);
        buf.writeInt(currentList.size());
        for (String id : currentList) buf.writeString(id);
        ClientPlayNetworking.send(ModPackets.UPDATE_BLOCK_LISTS_C2S, buf);

        if (listType == 0) pet.blacklistedBlocks = currentList;
        else if (listType == 1) pet.whitelistedBlocks = currentList;

        this.client.setScreen(parent);
    }
}