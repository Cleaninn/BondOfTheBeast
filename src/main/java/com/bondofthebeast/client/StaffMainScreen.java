package com.bondofthebeast.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StaffMainScreen extends Screen {
    private final List<PetData> pets;
    private final List<Card> cards = new ArrayList<>();
    private UUID lastSelected;
    private boolean selectionPageRestored;
    private int page, perPage, columns, rows, left, top, panelWidth, panelHeight, cardWidth, cardHeight;
    private float cardScale;
    private static final int CARD_WIDTH = 148, CARD_HEIGHT = 158, GAP = 12;

    public StaffMainScreen(List<PetData> pets) {
        super(Text.translatable("gui.bondofthebeast.pet_select"));
        this.pets = pets;
    }

    @Override protected void init() {
        cardScale = Math.min(1.0F, Math.min((width - 32) / (float) CARD_WIDTH,
                (height - 66) / (float) CARD_HEIGHT));
        cardWidth = Math.round(CARD_WIDTH * cardScale);
        cardHeight = Math.round(CARD_HEIGHT * cardScale);
        columns = Math.min(3, Math.max(1, (width - 32) / (cardWidth + GAP)));
        columns = Math.min(columns, Math.max(1, pets.size()));
        int visibleRows = Math.max(1, (pets.size() + columns - 1) / columns);
        rows = Math.min(Math.min(2, Math.max(1, (height - 62) / (cardHeight + GAP))), visibleRows);
        perPage = columns * rows;
        page = Math.min(page, Math.max(0, (pets.size() - 1) / perPage));
        lastSelected = readLastSelection();
        if (!selectionPageRestored && lastSelected != null) {
            for (int i = 0; i < pets.size(); i++) {
                if (pets.get(i).uuid.equals(lastSelected)) {
                    page = i / perPage;
                    break;
                }
            }
        }
        selectionPageRestored = true;
        panelWidth = columns * (cardWidth + GAP) - GAP + 32;
        panelHeight = rows * (cardHeight + GAP) - GAP + 62;
        left = (width - panelWidth) / 2;
        top = Math.max(0, (height - panelHeight) / 2);
        cards.clear();
        int start = page * perPage;
        for (int i = start; i < Math.min(pets.size(), start + perPage); i++) {
            PetData pet = pets.get(i);
            int x = left + 16 + (i - start) % columns * (cardWidth + GAP);
            int y = top + 29 + (i - start) / columns * (cardHeight + GAP);
            var preview = preview(pet);
            cards.add(new Card(pet, x, y, preview));
            addDrawableChild(new GrimoireButtonWidget(x + Math.round(10 * cardScale), y + Math.round((CARD_HEIGHT - 26) * cardScale),
                    cardWidth - Math.round(20 * cardScale), Math.round(20 * cardScale),
                    Text.translatable("gui.bondofthebeast.select_pet"), button -> {
                rememberSelection(pet.uuid);
                client.setScreen(new StaffPetScreen(this, pet));
            }));
        }
        var previous = addDrawableChild(new GrimoireButtonWidget(left + 16, top + panelHeight - 26, 28, 20,
                Text.literal("<"), b -> { page--; clearAndInit(); }));
        previous.active = page > 0;
        var next = addDrawableChild(new GrimoireButtonWidget(left + panelWidth - 44, top + panelHeight - 26, 28, 20,
                Text.literal(">"), b -> { page++; clearAndInit(); }));
        next.active = start + perPage < pets.size();
    }

    private UUID readLastSelection() {
        try {
            Path path = client.runDirectory.toPath().resolve("bondofthebeast-last-pet.txt");
            if (Files.exists(path)) return UUID.fromString(Files.readString(path, StandardCharsets.UTF_8).trim());
        } catch (Exception ignored) {}
        return null;
    }

    private void rememberSelection(UUID pet) {
        lastSelected = pet;
        try {
            Path path = client.runDirectory.toPath().resolve("bondofthebeast-last-pet.txt");
            Files.createDirectories(path.getParent());
            Files.writeString(path, pet.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    private net.minecraft.client.network.OtherClientPlayerEntity preview(PetData pet) {
        return PetPreview.create(client, pet);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        GrimoireStyle.fittedText(context, textRenderer, title, width / 2, top + 8, panelWidth - 80, 0xFFE8D8B2);
        context.fill(width / 2 - 42, top + 22, width / 2 + 42, top + 23, 0xFF806744);
        for (Card card : cards) {
            var matrices = context.getMatrices();
            matrices.push();
            matrices.translate(card.x, card.y, 0);
            matrices.scale(cardScale, cardScale, 1);
            int x = 0, y = 0;
            GrimoireStyle.paper(context, x, y, CARD_WIDTH, CARD_HEIGHT);
            context.drawBorder(x, y, CARD_WIDTH, CARD_HEIGHT, card.pet.uuid.equals(lastSelected) ? GrimoireStyle.SELECTED_BORDER : GrimoireStyle.PARCHMENT_BORDER);
            GrimoireStyle.leather(context, x + 4, y + 4, CARD_WIDTH - 8, 19, card.pet.uuid.equals(lastSelected));
            String name = card.pet.name.split("\\|")[0];
            GrimoireStyle.fittedText(context, textRenderer, Text.literal(name).formatted(net.minecraft.util.Formatting.BOLD),
                    x + CARD_WIDTH / 2, y + 10, CARD_WIDTH - 30, 0xFFF0DCAA);
            context.fill(x + 8, y + 27, x + CARD_WIDTH - 8, y + 113, 0xFF493930);
            context.drawBorder(x + 8, y + 27, CARD_WIDTH - 16, 86, 0xFF967B50);
            context.fill(x + 10, y + 29, x + 18, y + 30, 0xFFB69C67);
            context.fill(x + CARD_WIDTH - 18, y + 110, x + CARD_WIDTH - 10, y + 111, 0xFFB69C67);
            if (card.preview != null) {
                context.enableScissor(card.x + Math.round(8 * cardScale), card.y + Math.round(27 * cardScale),
                        card.x + cardWidth - Math.round(8 * cardScale), card.y + Math.round(113 * cardScale));
                boolean looking = mouseX >= card.x && mouseX < card.x + cardWidth && mouseY >= card.y && mouseY < card.y + cardHeight;
                int modelScale = Math.min(PetPreview.isFeral(card.preview) ? 40 : 30,
                        Math.max(10, (int) ((PetPreview.isFeral(card.preview) ? 80 : 64) / Math.max(1, card.preview.getHeight()))));
                PetPreview.draw(context, card.preview, x + CARD_WIDTH / 2, y + 105, modelScale,
                        looking ? net.minecraft.util.math.MathHelper.clamp((card.x + cardWidth / 2f - mouseX) / cardScale, -20, 20) : 0,
                        looking ? net.minecraft.util.math.MathHelper.clamp((card.y + 72f * cardScale - mouseY) / cardScale, -10, 10) : 0);
                context.disableScissor();
            }
            GrimoireStyle.fittedText(context, textRenderer, Text.translatable(card.pet.isOnline ? "gui.bondofthebeast.pet_online" : "gui.bondofthebeast.staff.offline"),
                    x + CARD_WIDTH / 2, y + 119, CARD_WIDTH - 30, card.pet.isOnline ? 0xFF47734E : GrimoireStyle.MUTED_INK);
            matrices.pop();
        }
        if (pets.isEmpty()) {
            GrimoireStyle.paper(context, left + 16, top + 29, cardWidth, cardHeight);
            GrimoireStyle.wrappedText(context, textRenderer, Text.translatable("gui.bondofthebeast.no_pets"),
                    width / 2, top + 29 + cardHeight / 2 - 10, cardWidth - 20, 30, GrimoireStyle.INK);
        }
        GrimoireStyle.fittedText(context, textRenderer, Text.translatable("gui.bondofthebeast.page", page + 1, Math.max(1, (pets.size() + perPage - 1) / perPage)),
                width / 2, top + panelHeight - 20, 80, 0xFFE8D8B2);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public void tick() {
        for (var card : cards) PetPreview.tick(card.preview);
    }

    private record Card(PetData pet, int x, int y, net.minecraft.client.network.OtherClientPlayerEntity preview) {}
    public static class PetData {
        public final UUID uuid;
        public final String name;
        public boolean isSitting, isTeleportEnabled, isProtectionMode, isAuraEnabled, isPacifistMode, isVampiricMode, isNoBreakMode, isAbsorbed, isNoInteractMode;
        public int formStage = -2, escapeTicks;
        public boolean forced, armorLocked;
        public float instinct;
        public int bondLevel, bondExp;
        public boolean hasCollar;
        public int skillPoints;
        public int trustTier, activeTicks;
        public String formId = "shape-shifter-curse:original_before_enable";
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
