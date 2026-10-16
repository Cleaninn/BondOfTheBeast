package com.bondofthebeast;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NecklaceOfClarity extends TrinketItem {
    public NecklaceOfClarity(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.bondofthebeast.necklace_of_clarity.tooltip").formatted(Formatting.YELLOW));
        tooltip.add(Text.translatable("item.bondofthebeast.necklace_of_clarity.conditions").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.bondofthebeast.necklace_of_clarity.limits").formatted(Formatting.GRAY));
    }
}
