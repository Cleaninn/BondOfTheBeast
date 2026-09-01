package com.bondofthebeast.mixin;

import com.bondofthebeast.CollarItem;
import com.bondofthebeast.block.PetBedBlock;
import com.bondofthebeast.block.PetBedBlockEntity;
import com.bondofthebeast.component.ModComponents;
import com.mojang.datafixers.util.Either;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.onixary.shapeShifterCurseFabric.player_form.ability.PlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Unique
    private long botb$lastSleepDay = -1L;

    private int getPlayerFormCategory(PlayerEntity player) {
        int stage = -1;
        String formIdStr = "";
        try {
            var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(player);
            if (sscComp != null && sscComp.getCurrentForm() != null) {
                stage = sscComp.getCurrentForm().getIndex();
                if (sscComp.getCurrentForm().FormID != null) {
                    formIdStr = sscComp.getCurrentForm().FormID.toString().toLowerCase();
                }
            }
        } catch (Exception ignored) {}

        if (formIdStr.contains("original_before_enable") || formIdStr.contains("original_shifter") || formIdStr.contains("allay")) {
            return -1;
        }
        if (formIdStr.contains("_sp") || stage == 3) {
            return 3;
        }
        if (stage == 0 || stage == 1) {
            return 0;
        }
        if (stage == 2) {
            return 2;
        }
        return -1;
    }

    @Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
    private void preventHumanBedSleep(BlockPos pos, CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        var bond = ModComponents.PLAYER_BOND.get(player);
        if (bond.hasOwner()) {
            int formCategory = getPlayerFormCategory(player);
            if (formCategory == 2) {
                return;
            }
            Block block = player.getWorld().getBlockState(pos).getBlock();
            if (!(block instanceof PetBedBlock)) {
                player.sendMessage(Text.translatable("text.bondofthebeast.cannot_sleep_here_human").formatted(Formatting.RED), true);
                cir.setReturnValue(Either.left(PlayerEntity.SleepFailureReason.OTHER_PROBLEM));
            }
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void botb$writeSleepData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putLong("BotbLastSleepDay", this.botb$lastSleepDay);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void botb$readSleepData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("BotbLastSleepDay")) {
            this.botb$lastSleepDay = nbt.getLong("BotbLastSleepDay");
        } else {
            this.botb$lastSleepDay = -1L;
        }
    }

    @Inject(method = "wakeUp", at = @At("HEAD"), cancellable = true)
    private void onWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        World world = player.getWorld();

        if (skipSleepTimer && player.isSleeping() && player.getSleepingPosition().isPresent()) {
            BlockPos pos = player.getSleepingPosition().get();
            if (world.getBlockState(pos).getBlock() instanceof PetBedBlock) {
                ci.cancel();
                return;
            }
        }

        if (!world.isClient && player.isSleeping() && player.getSleepingPosition().isPresent()) {
            if (player.getSleepTimer() < 100) {
                return;
            }

            long currentDay = world.getTime() / 12000;
            if (this.botb$lastSleepDay == currentDay) {
                return;
            }

            BlockPos sleepPos = player.getSleepingPosition().get();
            Block block = world.getBlockState(sleepPos).getBlock();
            int formCategory = getPlayerFormCategory(player);
            this.botb$lastSleepDay = currentDay;

            if (block instanceof PetBedBlock) {
                if (formCategory == 0) {
                    try {
                        net.onixary.shapeShifterCurseFabric.player_form.instinct.InstinctManager.applyImmediateEffect(player, "pet_bed_sleep", 15.0f);
                    } catch (Exception ignored) {}
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 60 * 2, 0));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 60 * 2, 0));
                    player.sendMessage(Text.translatable("text.bondofthebeast.sleep.strange_dreams").formatted(Formatting.YELLOW), false);
                    player.sendMessage(Text.translatable("text.bondofthebeast.sleep.collar_pulses"), true);
                } else if (formCategory == 2) {
                    player.sendMessage(Text.translatable("text.bondofthebeast.sleep.curled_up").formatted(Formatting.GOLD), false);
                } else if (formCategory == 3) {
                    player.sendMessage(Text.translatable("text.bondofthebeast.sleep.feel_safe").formatted(Formatting.GREEN), true);
                }

                int checkRadius = 10;
                for (BlockPos checkPos : BlockPos.iterate(sleepPos.add(-checkRadius, -2, -checkRadius), sleepPos.add(checkRadius, 2, checkRadius))) {
                    if (world.getBlockEntity(checkPos) instanceof PetBedBlockEntity bed) {
                        String petUUIDStr = bed.getBoundPetUUID();
                        if (petUUIDStr.isEmpty()) continue;
                        ServerPlayerEntity pet = world.getServer().getPlayerManager().getPlayer(UUID.fromString(petUUIDStr));

                        if (pet != null && pet.isSleeping()) {
                            var petComponent = ModComponents.PLAYER_BOND.get(pet);
                            if (player.getUuid().equals(petComponent.getOwnerUUID())) {
                                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 60 * 5, 0));
                                pet.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 60 * 5, 0));
                                petComponent.addBondExperience(50);
                                player.sendMessage(Text.translatable("text.bondofthebeast.sleep.owner_energy"), true);
                            }
                        }
                    }
                }
            } else {
                if (formCategory == 3) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 60 * 3, 1));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20 * 60 * 3, 1));
                    player.sendMessage(Text.translatable("text.bondofthebeast.sleep.wild_blood").formatted(Formatting.RED), false);
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickCheckForm(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        var bond = ModComponents.PLAYER_BOND.get(player);
        PlayerFormComponent formComponent = (PlayerFormComponent) RegPlayerFormComponent.PLAYER_FORM.get(player);

        if (formComponent != null && formComponent.getCurrentForm() != null) {
            if (formComponent.getCurrentForm().FormID != null && formComponent.getCurrentForm().FormID.getPath().toLowerCase().contains("allay")) return;

            int index = formComponent.getCurrentForm().getIndex();
            boolean isFeral = formComponent.getCurrentForm().getBodyType() == PlayerFormBodyType.FERAL;

            if (!bond.getRegisteredPets().isEmpty() && (index >= 3 || isFeral)) {
                List<String> petsToRemove = new ArrayList<>(bond.getRegisteredPets().keySet());

                for (String petUuidStr : petsToRemove) {
                    try {
                        ServerPlayerEntity pet = player.getServer().getPlayerManager().getPlayer(UUID.fromString(petUuidStr));
                        if (pet != null) {
                            var petBond = ModComponents.PLAYER_BOND.get(pet);
                            if (petBond.hasOwner() && petBond.getOwnerUUID().equals(player.getUuidAsString())) {
                                if (petBond.isAbsorbed()) {
                                    pet.changeGameMode(GameMode.SURVIVAL);
                                    petBond.setAbsorbed(false);
                                }
                                petBond.setBedPos(null);
                                petBond.clearOwner();

                                TrinketsApi.getTrinketComponent(pet).ifPresent(c -> {
                                    c.getInventory().values().forEach(g -> g.values().forEach(inv -> {
                                        for (int i = 0; i < inv.size(); i++) {
                                            if (inv.getStack(i).getItem() instanceof CollarItem) {
                                                ItemStack dropped = inv.getStack(i).copy();
                                                if (dropped.hasNbt()) dropped.getNbt().remove("OwnerName");
                                                pet.dropItem(dropped, true);
                                                inv.setStack(i, ItemStack.EMPTY);
                                            }
                                        }
                                    }));
                                });
                                ModComponents.PLAYER_BOND.sync(pet);
                                pet.sendMessage(Text.translatable("text.bondofthebeast.owner_went_feral_pet").formatted(Formatting.DARK_RED), false);
                            }
                        }
                        bond.removePetFromRegistry(petUuidStr);
                    } catch (Exception ignored) {}
                }
                if (!petsToRemove.isEmpty()) {
                    ModComponents.PLAYER_BOND.sync(player);
                    player.sendMessage(Text.translatable("text.bondofthebeast.owner_went_feral_owner").formatted(Formatting.DARK_RED), false);
                }
            }
        }
    }
}