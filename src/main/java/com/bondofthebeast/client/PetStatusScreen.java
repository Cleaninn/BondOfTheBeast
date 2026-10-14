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
        panel = GrimoireStyle.panel(width, height, 240, 220);
        resistButton = null;
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        if (bond.getForcedBond().forced && !bond.getForcedBond().contractSigned) {
            resistButton = addDrawableChild(new GrimoireButtonWidget(panel.x(30), panel.y(155), panel.size(178), panel.size(22),
                    Text.translatable("gui.bondofthebeast.diary.resist"), button -> {
                ClientPlayNetworking.send(ModPackets.REQUEST_ESCAPE_C2S, PacketByteBufs.create());
                close();
            }, true));
            resistButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("gui.bondofthebeast.determination")));
        }
        addDrawableChild(new GrimoireButtonWidget(panel.x(30), panel.y(185), panel.size(178), panel.size(20),
                Text.translatable("gui.back"), button -> close(), true));
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (resistButton != null) {
            int cooldown = ForcedTamingService.resistanceCooldownSeconds(client.player);
            resistButton.active = cooldown == 0 && !ForcedTamingService.freeWindow(client.player) &&
                    !ModComponents.PLAYER_BOND.get(client.player).getForcedBond().contractSigned;
            resistButton.setMessage(Text.translatable(cooldown > 0 ? "gui.bondofthebeast.diary.resistance_cooldown" :
                    "gui.bondofthebeast.diary.resist", cooldown));
        }
        PetDiaryStyle.begin(context, panel, width, height);
        GrimoireStyle.fittedText(context, textRenderer, title, 120, 12, 174, 0xFFF0DCAA);
        var bond = ModComponents.PLAYER_BOND.get(client.player);
        boolean forced = bond.getForcedBond().forced;
        int stage = ForcedTamingService.stage(client.player);
        var trust = bond.getVoluntaryBond();
        Text state = forced ? Text.translatable(stage < 0 ? "gui.bondofthebeast.human_stage" : "gui.bondofthebeast.stage", stage)
                : Text.translatable("gui.bondofthebeast.trust." + trust.tier);
        GrimoireStyle.wrappedText(context, textRenderer, state, 119, 42, 160, 20, GrimoireStyle.INK);
        context.fill(43, 67, 195, 68, 0xFFAA8B5C);
        Text summary;
        Text hint;
        if (forced) {
            summary = Text.translatable(bond.getForcedBond().contractSigned ? "gui.bondofthebeast.permanent_forced_bond" : "gui.bondofthebeast.temporary_forced_bond");
            hint = bond.getForcedBond().contractSigned ? Text.translatable("gui.bondofthebeast.escape_rule.contract") :
                    ForcedTamingService.freeWindow(client.player) ? Text.translatable("gui.bondofthebeast.escape_seconds", (bond.getForcedBond().escapeTicks + 19) / 20) :
                    Text.translatable("gui.bondofthebeast.escape_rule." + (stage >= 2 ? "enchanted" : "apple"));
        } else {
            int seconds = VoluntaryBondService.remainingSeconds(trust.tier, trust.activeTicks);
            summary = Text.translatable(trust.tier >= 3 ? "gui.bondofthebeast.bond_complete" : "gui.bondofthebeast.bond_time", seconds / 3600, seconds / 60 % 60);
            hint = Text.translatable("gui.bondofthebeast.owner_controls");
        }
        GrimoireStyle.wrappedText(context, textRenderer, summary, 119, 77, 158, 26, GrimoireStyle.INK);
        GrimoireStyle.wrappedText(context, textRenderer, hint, 119, 110, 158, 18, GrimoireStyle.MUTED_INK);
        context.getMatrices().pop();
        super.render(context, mouseX, mouseY, delta);
    }
}
