package com.bondofthebeast.mixin;

import com.bondofthebeast.client.PetStatusScreen;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.onixary.shapeShifterCurseFabric.custom_ui.BookOfShapeShifterScreenV2_P2.class)
public class BookOfShapeShifterScreenMixin extends Screen {

    protected BookOfShapeShifterScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addPetPageArrow(CallbackInfo ci) {
        if (this.client != null && this.client.player != null) {
            var bond = ModComponents.PLAYER_BOND.get(this.client.player);
            if (bond.hasOwner()) {
                // Изменили путь на новую текстуру для GUI
                Identifier buttonTexture = new Identifier("bondofthebeast", "textures/gui/pet_diary_button.png");
                int btnSize = 28;
                int btnX = this.width / 2 + 82;
                int btnY = this.height / 2 + 65;

                this.addDrawableChild(new TexturedButtonWidget(
                        btnX, btnY,
                        btnSize, btnSize,
                        0, 0, 0,
                        buttonTexture,
                        btnSize, btnSize,
                        b -> this.client.setScreen(new PetStatusScreen(this))
                ) {
                    @Override
                    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                        boolean hovered = this.isHovered();

                        // Если навели мышку, рисуем легкую подсветку (как было раньше)
                        if (hovered) {
                            context.fill(this.getX() - 2, this.getY() - 2, this.getX() + this.width + 2, this.getY() + this.height + 2, 0x44FFD700);
                        }

                        // Рисуем саму твою новую кнопку
                        context.drawTexture(buttonTexture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);

                        // Показываем переведенный текст при наведении
                        if (hovered) {
                            context.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.translatable("gui.bondofthebeast.pet_diary.title"), mouseX, mouseY);
                        }
                    }
                });
            }
        }
    }
}