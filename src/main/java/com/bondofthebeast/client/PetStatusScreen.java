package com.bondofthebeast.client;

import com.bondofthebeast.BondRules;
import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.ModPackets;
import com.bondofthebeast.component.ModComponents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class PetStatusScreen extends Screen {
    private final Screen parent;
    private GrimoireStyle.PanelLayout panel;
    private GrimoireButtonWidget resistButton;
    private net.minecraft.client.network.OtherClientPlayerEntity ownerPreview;
    private int commandIndex;

    public PetStatusScreen() { this(null); }
    public PetStatusScreen(Screen parent) {
        super(Text.translatable("gui.bondofthebeast.pet_status"));
        this.parent = parent;
    }
    @Override public void close() { client.setScreen(parent); }

    @Override protected void init() {
        panel = GrimoireStyle.panel(width, height, 240, 220);
        resistButton = null;
        commandIndex = 0;
        ownerPreview = createOwnerPreview();
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        if (bond.getForcedBond().forced && !bond.getForcedBond().contractSigned) {
            resistButton = addDrawableChild(new GrimoireButtonWidget(panel.x(52), panel.y(194), panel.size(136), panel.size(15),
                    Text.translatable("gui.bondofthebeast.diary.resist"), button -> {
                ClientPlayNetworking.send(ModPackets.REQUEST_ESCAPE_C2S, PacketByteBufs.create());
                close();
            }, true));
        }
        command("sit", "sit", "sit", bond::isSitting);
        command("tp", "tp", "tp", bond::isTeleportEnabled);
        command("absorb", "absorb", "absorb", bond::isAbsorbed);
        command("inventory", "inventory", "inventory", bond::isItemForwarding);
        command("nobreak", "nobreak", "nobreak", bond::isNoBreakMode);
        command("interact", "interact", "nobreak", bond::isNoInteractMode);
        command("pacifist", "pacifist", "nobreak", bond::isPacifistMode);
        command("armor_lock", "armor_lock", "armor", () -> bond.getForcedBond().armorLocked);
        command("armor_open", "armor", "armor", () -> true);
    }

    private void command(String name, String glyph, String skill, java.util.function.BooleanSupplier enabled) {
        int index = commandIndex++;
        addDrawableChild(new DiaryCommandWidget(panel.x(36 + index % 3 * 56), panel.y(131 + index / 3 * 20),
                panel.size(52), panel.size(18), name, glyph, () -> BondRules.allows(client.player, skill), enabled));
    }

    private net.minecraft.client.network.OtherClientPlayerEntity createOwnerPreview() {
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        if (!bond.hasOwner()) return null;
        try {
            var uuid = java.util.UUID.fromString(bond.getOwnerUUID());
            var data = new StaffMainScreen.PetData(uuid, bond.getOwnerName(), false, false, false, false, false, false, false, java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), true);
            var live = client.world.getPlayerByUuid(uuid);
            if (live != null) data.formId = net.onixary.shapeShifterCurseFabric.player_form.utils.RegPlayerFormComponent.PLAYER_FORM.get(live).nowForm.getFormID().toString();
            return PetPreview.create(client, data);
        } catch (IllegalArgumentException e) { return null; }
    }

    @Override public void tick() { PetPreview.tick(ownerPreview); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        if (resistButton != null) {
            resistButton.visible = bond.getForcedBond().forced && !bond.getForcedBond().contractSigned;
            int cooldown = ForcedTamingService.resistanceCooldownSeconds(client.player);
            resistButton.active = cooldown == 0 && !ForcedTamingService.freeWindow(client.player) && !bond.getForcedBond().contractSigned;
            resistButton.setMessage(Text.translatable("gui.bondofthebeast.diary.resist"));
            resistButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(cooldown > 0 ?
                    Text.translatable("gui.bondofthebeast.diary.resistance_cooldown", cooldown) :
                    Text.translatable("gui.bondofthebeast.determination")));
        }
        PetDiaryStyle.begin(context, panel, width, height);
        GrimoireStyle.fittedText(context, textRenderer, title, 120, 5, 180, 0xFFEAD4A2, 0.8F);
        if (ownerPreview != null) {
            context.enableScissor(panel.x(85), panel.y(23), panel.x(153), panel.y(103));
            PetPreview.draw(context, ownerPreview, 120, 103, 38,
                    net.minecraft.util.math.MathHelper.clamp((panel.x(120) - mouseX) / panel.scale(), -20, 20), 0);
            context.disableScissor();
        }
        GrimoireStyle.fittedText(context, textRenderer, Text.literal(bond.getOwnerName()), 120, 111, 160, 0xFFEAD4A2);
        GrimoireStyle.fittedText(context, textRenderer, Text.translatable("gui.bondofthebeast.diary.commands"), 120, 122, 170, 0xFFB8C3D9, 0.7F);
        Text hint = Text.translatable("gui.bondofthebeast.escape_rule." +
                (!bond.getForcedBond().forced || bond.getForcedBond().contractSigned ? "contract" :
                        ForcedTamingService.stage(client.player) >= 3 ? "enchanted" : "apple"));
        GrimoireStyle.fittedText(context, textRenderer, hint, 120, 212, 175, 0xFFBCC6D9, 0.55F);
        context.getMatrices().pop();
        super.render(context, mouseX, mouseY, delta);
    }
}
