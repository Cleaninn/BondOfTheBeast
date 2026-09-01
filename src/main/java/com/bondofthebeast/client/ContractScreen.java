package com.bondofthebeast.client;

import com.bondofthebeast.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ContractScreen extends Screen {
    private final boolean isPet;

    public ContractScreen(boolean isPet) {
        super(Text.translatable(isPet ? "gui.bondofthebeast.contract.title_pet" : "gui.bondofthebeast.contract.title_owner"));
        this.isPet = isPet;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.bondofthebeast.contract.sign").formatted(isPet ? Formatting.DARK_RED : Formatting.GOLD), button -> {
            ClientPlayNetworking.send(ModPackets.SIGN_CONTRACT_C2S, PacketByteBufs.empty());
            this.close();
        }).dimensions(centerX - 105, centerY + 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.bondofthebeast.contract.decline").formatted(Formatting.GRAY), button -> {
            this.close();
        }).dimensions(centerX + 5, centerY + 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (isPet) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.bondofthebeast.contract.pet.line1").formatted(Formatting.DARK_RED, Formatting.BOLD),
                    centerX, centerY - 40, 0xFFFFFF);

            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.bondofthebeast.contract.pet.line2").formatted(Formatting.GRAY),
                    centerX, centerY - 20, 0xFFFFFF);

            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.bondofthebeast.contract.pet.line3").formatted(Formatting.RED),
                    centerX, centerY - 5, 0xFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.bondofthebeast.contract.owner.line1").formatted(Formatting.GOLD, Formatting.BOLD),
                    centerX, centerY - 40, 0xFFFFFF);

            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.bondofthebeast.contract.owner.line2").formatted(Formatting.GRAY),
                    centerX, centerY - 20, 0xFFFFFF);

            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.bondofthebeast.contract.owner.line3").formatted(Formatting.YELLOW),
                    centerX, centerY - 5, 0xFFFFFF);
        }
    }
}