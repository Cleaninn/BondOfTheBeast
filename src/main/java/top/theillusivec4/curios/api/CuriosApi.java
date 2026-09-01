package top.theillusivec4.curios.api;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;

public class CuriosApi {
    public static LazyOptional<Object> getCuriosInventory(LivingEntity entity) {
        return LazyOptional.empty();
    }
}