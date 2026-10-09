package com.bondofthebeast.client;

import com.bondofthebeast.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlockSelectionScreen extends Screen {
    private static final int CELL = 22;
    private final Screen parent;
    private final StaffMainScreen.PetData pet;
    private final int listType;
    private final Set<String> currentList;
    private TextFieldWidget searchBox;
    private List<Block> allBlocks, filteredBlocks;
    private int scrollOffset, columns, rows, left, top, panelWidth, panelHeight, gridX, gridY;

    public BlockSelectionScreen(Screen parent, StaffMainScreen.PetData pet, int listType) {
        super(Text.translatable(listType == 0 ? "gui.bondofthebeast.block_select.blacklist" : "gui.bondofthebeast.block_select.whitelist"));
        this.parent = parent;
        this.pet = pet;
        this.listType = listType;
        currentList = new HashSet<>(listType == 0 ? pet.blacklistedBlocks : pet.whitelistedBlocks);
    }

    @Override protected void init() {
        String query = searchBox == null ? "" : searchBox.getText();
        int previousOffset = scrollOffset;
        columns = Math.max(1, Math.min(9, (width - 48) / CELL));
        rows = Math.max(1, Math.min(7, (height - 112) / CELL));
        panelWidth = columns * CELL + 32;
        panelHeight = rows * CELL + 96;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        gridX = left + 14;
        gridY = top + 58;
        allBlocks = Registries.BLOCK.stream().filter(block -> !block.getDefaultState().isAir()
                        && block.asItem() != net.minecraft.item.Items.AIR)
                .sorted(Comparator.comparing(block -> block.getName().getString())).toList();
        searchBox = new TextFieldWidget(textRenderer, left + 14, top + 33, panelWidth - 28, 18,
                Text.translatable("gui.bondofthebeast.block_select.search"));
        searchBox.setPlaceholder(Text.translatable("gui.bondofthebeast.block_select.search"));
        searchBox.setChangedListener(this::onSearch);
        searchBox.setText(query);
        onSearch(query);
        scrollOffset = Math.min(previousOffset, maximumScroll());
        addDrawableChild(searchBox);
        int buttonWidth = (panelWidth - 34) / 2;
        addDrawableChild(new GrimoireButtonWidget(left + 14, top + panelHeight - 28, buttonWidth, 20,
                Text.translatable("gui.bondofthebeast.save"), button -> saveAndExit()));
        addDrawableChild(new GrimoireButtonWidget(left + panelWidth - 14 - buttonWidth, top + panelHeight - 28, buttonWidth, 20,
                Text.translatable("gui.bondofthebeast.cancel"), button -> close()));
    }

    private void onSearch(String text) {
        String query = text.toLowerCase(Locale.ROOT);
        filteredBlocks = allBlocks.stream().filter(block -> block.getName().getString().toLowerCase(Locale.ROOT).contains(query)
                || Registries.BLOCK.getId(block).toString().contains(query)).toList();
        scrollOffset = 0;
    }

    private int maximumScroll() { return Math.max(0, (filteredBlocks.size() + columns - 1) / columns - rows); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        GrimoireStyle.paper(context, left, top, panelWidth, panelHeight);
        GrimoireStyle.leather(context, left + 6, top + 6, panelWidth - 12, 22, false);
        GrimoireStyle.fittedText(context, textRenderer, title, width / 2, top + 12, panelWidth - 32, 0xFFF0DCAA);
        Block hovered = null;
        for (int i = 0; i < columns * rows; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredBlocks.size()) break;
            Block block = filteredBlocks.get(index);
            int bx = gridX + i % columns * CELL;
            int by = gridY + i / columns * CELL;
            boolean selected = currentList.contains(Registries.BLOCK.getId(block).toString());
            context.fill(bx, by, bx + 20, by + 20, selected ? 0xFFB69076 : 0xFFD3BD90);
            context.drawBorder(bx, by, 20, 20, selected ? UiColors.BORDER : GrimoireStyle.PARCHMENT_BORDER);
            context.drawItem(block.asItem().getDefaultStack(), bx + 2, by + 2);
            if (mouseX >= bx && mouseX < bx + 20 && mouseY >= by && mouseY < by + 20) hovered = block;
        }
        if (maximumScroll() > 0) {
            int trackX = left + panelWidth - 11;
            int trackHeight = rows * CELL - 2;
            context.fill(trackX, gridY, trackX + 3, gridY + trackHeight, 0xFFAA8B5C);
            int thumbHeight = Math.max(8, trackHeight * rows / (maximumScroll() + rows));
            int thumbY = gridY + (trackHeight - thumbHeight) * scrollOffset / maximumScroll();
            context.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, UiColors.BORDER);
        }
        if (filteredBlocks.isEmpty()) GrimoireStyle.wrappedText(context, textRenderer,
                Text.translatable("gui.bondofthebeast.block_select.empty"), width / 2, gridY + 8,
                panelWidth - 32, rows * CELL - 12, GrimoireStyle.MUTED_INK);
        super.render(context, mouseX, mouseY, delta);
        if (hovered != null) context.drawOrderedTooltip(textRenderer,
                textRenderer.wrapLines(hovered.getName(), Math.min(200, width - 24)), mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) for (int i = 0; i < columns * rows; i++) {
            int index = i + scrollOffset * columns;
            if (index >= filteredBlocks.size()) break;
            int bx = gridX + i % columns * CELL;
            int by = gridY + i / columns * CELL;
            if (mouseX >= bx && mouseX < bx + 20 && mouseY >= by && mouseY < by + 20) {
                String id = Registries.BLOCK.getId(filteredBlocks.get(index)).toString();
                if (!currentList.remove(id)) currentList.add(id);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < gridX || mouseX >= left + panelWidth - 8 || mouseY < gridY || mouseY >= gridY + rows * CELL)
            return super.mouseScrolled(mouseX, mouseY, amount);
        scrollOffset = Math.max(0, Math.min(maximumScroll(), scrollOffset + (amount > 0 ? -1 : 1)));
        return true;
    }

    private void saveAndExit() {
        var buffer = PacketByteBufs.create();
        buffer.writeUuid(pet.uuid);
        buffer.writeInt(listType);
        buffer.writeInt(currentList.size());
        for (String id : currentList) buffer.writeString(id);
        ClientPlayNetworking.send(ModPackets.UPDATE_BLOCK_LISTS_C2S, buffer);
        if (listType == 0) pet.blacklistedBlocks = currentList;
        else pet.whitelistedBlocks = currentList;
        close();
    }

    @Override public void close() { client.setScreen(parent); }
}
