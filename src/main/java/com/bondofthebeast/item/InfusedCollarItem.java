package com.bondofthebeast.item;

import com.bondofthebeast.CollarItem;

/** Catalyst reservoir; the shared collar flow also supports existing, consenting pets. */
public class InfusedCollarItem extends CollarItem {
    public InfusedCollarItem(Settings settings) { super(settings); }
    @Override public void appendTooltip(net.minecraft.item.ItemStack stack, net.minecraft.world.World world,
            java.util.List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(net.minecraft.text.Text.translatable("tooltip.bondofthebeast.reservoir"));
        tooltip.add(net.minecraft.text.Text.translatable("tooltip.bondofthebeast.await_curse"));
    }
}
