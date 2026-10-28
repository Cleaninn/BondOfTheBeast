package com.bondofthebeast.client;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.CollarItem;
import com.bondofthebeast.item.InfusedCollarItem;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms;
import net.onixary.shapeShifterCurseFabric.player_form.utils.FormUtils;
import net.onixary.shapeShifterCurseFabric.render.form_render.FormAnimatable;
import net.onixary.shapeShifterCurseFabric.render.form_render.FormRenderer;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

/** New subforms attach to their animated neck rather than the hidden vanilla body. */
public final class FormCollarLayer extends GeoRenderLayer<FormAnimatable> {
    private static final Identifier TEXTURE = BondOfTheBeast.identifier("textures/entity/collar.png");
    private static final Identifier RESERVOIR_TEXTURE = new Identifier("minecraft", "textures/block/white_concrete.png");
    private final CollarWildModel normal = new CollarWildModel();
    private final InfusedCollarModel infused = new InfusedCollarModel();

    private FormCollarLayer(FormRenderer renderer) { super(renderer); }

    public static void register() {
        GeoRenderEvent.Object.CompileRenderLayers.EVENT.register(event -> {
            if (event.getRenderer() instanceof FormRenderer renderer) event.addLayer(new FormCollarLayer(renderer));
        });
    }

    public static boolean supports(IForm form) {
        if (form == null) return false;
        return RegPlayerForms.BAT_3_SUB_AVALI.isEquals(form) || RegPlayerForms.SNOW_FOX_3_SUB_MARBLED_POLECAT.isEquals(form);
    }

    @Override
    public void renderForBone(MatrixStack matrices, FormAnimatable animatable, GeoBone bone, RenderLayer renderType,
                              VertexConsumerProvider buffers, VertexConsumer buffer, float tickDelta, int light, int overlay) {
        var player = animatable.e;
        if (player == null || player.isInvisible() || player.isSpectator()) return;
        var form = FormUtils.getPlayerForm(player);
        if (!supports(form) || renderType != RenderLayer.getEntityTranslucent(getTextureResource(animatable))) return;
        boolean polecat = RegPlayerForms.SNOW_FOX_3_SUB_MARBLED_POLECAT.isEquals(form);
        if (!bone.getName().equals(polecat ? "neck_1" : "bipedHead")) return;
        var collars = TrinketsApi.getTrinketComponent(player).map(c ->
                c.getEquipped(stack -> stack.getItem() instanceof CollarItem)).orElse(java.util.List.of());
        for (var entry : collars) {
            boolean reservoir = entry.getRight().getItem() instanceof InfusedCollarItem;
            matrices.push();
            RenderUtils.translateToPivotPoint(matrices, bone);
            matrices.scale(polecat ? 0.75f : 0.65f, -1, polecat ? -0.95f : -0.85f);
            var model = reservoir ? infused : normal;
            model.render(matrices, buffers.getBuffer(model.getLayer(reservoir ? RESERVOIR_TEXTURE : TEXTURE)), light,
                    OverlayTexture.DEFAULT_UV, reservoir ? 0.65f : 1, reservoir ? 0.3f : 1, reservoir ? 0.85f : 1, 1);
            matrices.pop();
            // GeckoLib continues rendering this bone's children with the original layer.
            buffers.getBuffer(renderType);
        }
    }
}
