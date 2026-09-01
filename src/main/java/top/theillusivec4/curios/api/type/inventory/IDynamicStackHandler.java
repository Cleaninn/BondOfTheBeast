package top.theillusivec4.curios.api.type.inventory;

import net.minecraft.item.ItemStack;

public interface IDynamicStackHandler {
    // Добавляем метод-пустышку, который ищет загрузчик, чтобы он успокоился
    ItemStack getStackInSlot(int slot);
}