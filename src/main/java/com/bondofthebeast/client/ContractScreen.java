package com.bondofthebeast.client;

import com.bondofthebeast.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ContractScreen extends Screen {
    private final boolean isPet;
    private GrimoireStyle.PanelLayout panel;

    public ContractScreen(boolean isPet) {
        super(Text.translatable(isPet ? "gui.bondofthebeast.contract.title_pet" : "gui.bondofthebeast.contract.title_owner"));
        this.isPet = isPet;
    }

    @Override protected void init() {
        panel = GrimoireStyle.panel(width, height, 260, 154);
        addDrawableChild(new GrimoireButtonWidget(panel.x(14), panel.y(120), panel.size(110), panel.size(22),
                Text.translatable("gui.bondofthebeast.contract.sign"), button -> {
            ClientPlayNetworking.send(ModPackets.SIGN_CONTRACT_C2S, PacketByteBufs.empty());
            close();
        }));
        addDrawableChild(new GrimoireButtonWidget(panel.x(136), panel.y(120), panel.size(110), panel.size(22),
                Text.translatable("gui.bondofthebeast.contract.decline"), button -> close()));
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        GrimoireStyle.beginPanel(context, panel);
        GrimoireStyle.fittedText(context, textRenderer, title, 130, 12, 224, 0xFFF0DCAA);
        String prefix = "gui.bondofthebeast.contract." + (isPet ? "pet" : "owner");
        GrimoireStyle.wrappedText(context, textRenderer, Text.translatable(prefix + ".line2"),
                130, 46, 226, 28, GrimoireStyle.INK);
        context.fill(34, 80, 226, 81, 0xFFAA8B5C);
        GrimoireStyle.wrappedText(context, textRenderer, Text.translatable(prefix + ".line3"),
                130, 91, 226, 22, GrimoireStyle.MUTED_INK);
        context.getMatrices().pop();
        super.render(context, mouseX, mouseY, delta);
    }
}
