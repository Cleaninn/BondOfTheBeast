package com.bondofthebeast.item;

import com.bondofthebeast.ForcedTamingService;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class CatalystCookieItem extends Item {
    private final boolean echo;
    public CatalystCookieItem(Settings settings, boolean echo) { super(settings); this.echo = echo; }
    public boolean isEcho() { return echo; }

    @Override public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity target, Hand hand) {
        if (!(target instanceof PlayerEntity)) return ActionResult.PASS;
        if (user.getWorld().isClient) return ActionResult.SUCCESS;
        return target instanceof ServerPlayerEntity pet ? ForcedTamingService.feedCatalyst(user, pet, stack) : ActionResult.FAIL;
    }

    @Override public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.bondofthebeast." + (echo ? "echo_catalyst_cookie" : "catalyst_cookie")).formatted(Formatting.GRAY));
    }
}
