package com.bondofthebeast;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Last known appearance for grimoire cards, including offline or distant players. */
public final class PetPreviewState extends PersistentState {
    private final Map<String, String> forms = new HashMap<>();

    public static PetPreviewState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                PetPreviewState::read, PetPreviewState::new, "bondofthebeast_previews");
    }

    private static PetPreviewState read(NbtCompound tag) {
        var state = new PetPreviewState();
        for (String key : tag.getKeys()) state.forms.put(key, tag.getString(key));
        return state;
    }

    @Override public NbtCompound writeNbt(NbtCompound tag) {
        forms.forEach(tag::putString);
        return tag;
    }

    public void update(ServerPlayerEntity pet) {
        var form = RegPlayerFormComponent.PLAYER_FORM.get(pet).getCurrentForm();
        if (form == null) return;
        String value = form.FormID.toString();
        if (!value.equals(forms.put(pet.getUuidAsString(), value))) markDirty();
    }

    public String form(UUID pet) {
        return forms.getOrDefault(pet.toString(), "shape-shifter-curse:original_before_enable");
    }
}
