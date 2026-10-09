package com.bondofthebeast.client;

import com.bondofthebeast.ForcedTamingService;
import com.bondofthebeast.ModPackets;
import com.bondofthebeast.VoluntaryBondService;
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

    public PetStatusScreen() { this(null); }
    public PetStatusScreen(Screen parent) {
        super(Text.translatable("gui.bondofthebeast.pet_status"));
        this.parent = parent;
    }
    @Override public void close() { client.setScreen(parent); }

    @Override protected void init() {
        panel = GrimoireStyle.panel(width, height, 300, 194);
        resistButton = null;
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        if (bond.getForcedBond().forced && ForcedTamingService.stage(client.player) < 3) {
            resistButton = addDrawableChild(new GrimoireButtonWidget(panel.x(14), panel.y(136), panel.size(272), panel.size(22),
                    Text.translatable("gui.bondofthebeast.determination"), button -> {
                ClientPlayNetworking.send(ModPackets.REQUEST_ESCAPE_C2S, PacketByteBufs.create());
                close();
            }));
        }
        addDrawableChild(new GrimoireButtonWidget(panel.x(14), panel.y(164), panel.size(272), panel.size(20),
                Text.translatable("gui.back"), button -> close()));
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (resistButton != null) {
            int cooldown = ForcedTamingService.resistanceCooldownSeconds(client.player);
            resistButton.active = cooldown == 0 && !ForcedTamingService.freeWindow(client.player) &&
                    ForcedTamingService.stage(client.player) < 3;
            resistButton.setMessage(Text.translatable(cooldown > 0 ? "gui.bondofthebeast.resistance_cooldown" :
                    "gui.bondofthebeast.determination", cooldown));
        }
        GrimoireStyle.drawBook(context, width, height);
        GrimoireStyle.beginPanel(context, panel);
        GrimoireStyle.fittedText(context, textRenderer, title, 150, 12, 262, 0xFFF0DCAA);
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        boolean forced = bond.getForcedBond().forced;
        int stage = ForcedTamingService.stage(client.player);
        var trust = bond.getVoluntaryBond();
        Text state = forced ? Text.translatable(stage < 0 ? "gui.bondofthebeast.human_stage" : "gui.bondofthebeast.stage", stage)
                : Text.translatable("gui.bondofthebeast.trust." + trust.tier);
        GrimoireStyle.wrappedText(context, textRenderer, state, 150, 39, 268, 22, GrimoireStyle.INK);
        context.fill(30, 64, 270, 65, 0xFFAA8B5C);
        Text summary;
        Text hint;
        if (forced) {
            summary = Text.translatable("gui.bondofthebeast.escape_rule." + (stage >= 3 ? "contract" : stage == 2 ? "enchanted" : "apple"));
            hint = stage >= 3 ? Text.translatable("gui.bondofthebeast.owner_controls") :
                    ForcedTamingService.freeWindow(client.player) ? Text.translatable("gui.bondofthebeast.escape_seconds", bond.getForcedBond().escapeTicks / 20) :
                    Text.translatable("gui.bondofthebeast.escape_hint");
        } else {
            int seconds = VoluntaryBondService.remainingSeconds(trust.tier, trust.activeTicks);
            summary = Text.translatable(trust.tier >= 3 ? "gui.bondofthebeast.bond_complete" : "gui.bondofthebeast.bond_time", seconds / 3600, seconds / 60 % 60);
            hint = Text.translatable("gui.bondofthebeast.owner_controls");
        }
        GrimoireStyle.wrappedText(context, textRenderer, summary, 150, 75, 260, 30, UiColors.BORDER);
        GrimoireStyle.wrappedText(context, textRenderer, hint, 150, 110, 260, 20, GrimoireStyle.MUTED_INK);
        context.getMatrices().pop();
        super.render(context, mouseX, mouseY, delta);
    }
}
