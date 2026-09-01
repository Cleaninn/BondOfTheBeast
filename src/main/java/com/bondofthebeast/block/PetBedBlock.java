package com.bondofthebeast.block;

import com.bondofthebeast.ModItems;
import com.bondofthebeast.ModPackets;
import com.bondofthebeast.component.ModComponents;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.shape.VoxelShape;
import java.util.UUID;

public class PetBedBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = Properties.BED_PART;
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);

    public PetBedBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(PART, BedPart.FOOT));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

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

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockPos headPos = state.get(PART) == BedPart.HEAD ? pos : pos.offset(state.get(FACING));

        if (world.getBlockEntity(headPos) instanceof PetBedBlockEntity bed) {
            String petId = bed.getBoundPetUUID();
            boolean isBound = petId != null && !petId.isEmpty();

            var bond = ModComponents.PLAYER_BOND.get(player);
            boolean isMaster = !bond.getRegisteredPets().isEmpty();
            boolean isPet = bond.hasOwner();

            int formCategory = getPlayerFormCategory(player);

            if (isBound && petId.equals(player.getUuidAsString())) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    if (formCategory == 0) player.sendMessage(Text.translatable("text.bondofthebeast.bed_sleep_start").formatted(net.minecraft.util.Formatting.GOLD), true);
                    serverPlayer.trySleep(headPos).ifLeft(reason -> {
                        player.sendMessage(Text.translatable("text.bondofthebeast.cannot_sleep_here"), true);
                    });
                }
                return ActionResult.CONSUME;
            }

            if (isMaster) {
                if (isBound && !bond.getRegisteredPets().containsKey(petId)) {
                    player.sendMessage(Text.translatable("text.bondofthebeast.bed_not_yours").formatted(net.minecraft.util.Formatting.RED), true);
                    return ActionResult.FAIL;
                }

                var pets = bond.getRegisteredPets();
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBlockPos(headPos);
                buf.writeString(petId == null ? "" : petId);
                buf.writeInt(bed.getChainRadius());
                buf.writeInt(pets.size());

                for (var entry : pets.entrySet()) {
                    buf.writeString(entry.getKey());
                    buf.writeString(entry.getValue());
                    ServerPlayerEntity petEntity = world.getServer().getPlayerManager().getPlayer(UUID.fromString(entry.getKey()));
                    buf.writeBoolean(petEntity != null && TrinketsApi.getTrinketComponent(petEntity).map(c -> c.isEquipped(ModItems.COLLAR)).orElse(false));
                }

                ServerPlayNetworking.send((ServerPlayerEntity) player, ModPackets.OPEN_BED_GUI, buf);
                return ActionResult.CONSUME;
            }

            if (formCategory == -1) {
                player.sendMessage(Text.translatable("text.bondofthebeast.humans_sleep_on_beds").formatted(net.minecraft.util.Formatting.RED), true);
                return ActionResult.FAIL;
            }

            if (isBound) {
                if (formCategory == 2) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.trySleep(headPos).ifLeft(reason -> {
                            player.sendMessage(Text.translatable("text.bondofthebeast.cannot_sleep_here"), true);
                        });
                    }
                    return ActionResult.CONSUME;
                } else {
                    player.sendMessage(Text.translatable("text.bondofthebeast.bed_occupied").formatted(net.minecraft.util.Formatting.RED), true);
                    return ActionResult.FAIL;
                }
            } else {
                if (isPet && formCategory != 2) {
                    player.sendMessage(Text.translatable("text.bondofthebeast.must_be_bound_by_master").formatted(net.minecraft.util.Formatting.RED), true);
                    return ActionResult.FAIL;
                } else {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        if (formCategory == 0) player.sendMessage(Text.translatable("text.bondofthebeast.bed_sleep_start").formatted(net.minecraft.util.Formatting.GOLD), true);
                        serverPlayer.trySleep(headPos).ifLeft(reason -> {
                            player.sendMessage(Text.translatable("text.bondofthebeast.cannot_sleep_here"), true);
                        });
                    }
                    return ActionResult.CONSUME;
                }
            }
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            BedPart part = state.get(PART);
            Direction facing = state.get(FACING);
            BlockPos otherPos = part == BedPart.FOOT ? pos.offset(facing) : pos.offset(facing.getOpposite());
            BlockState otherState = world.getBlockState(otherPos);

            if (otherState.isOf(this) && otherState.get(PART) != part) {
                world.setBlockState(otherPos, Blocks.AIR.getDefaultState(), 35);
                world.syncWorldEvent(player, 2001, otherPos, Block.getRawIdFromState(otherState));
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == getDirectionTowardsOtherPart(state.get(PART), state.get(FACING))) {
            return neighborState.isOf(this) && neighborState.get(PART) != state.get(PART) ? state : Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    private static Direction getDirectionTowardsOtherPart(BedPart part, Direction facing) {
        return part == BedPart.FOOT ? facing : facing.getOpposite();
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction direction = ctx.getHorizontalPlayerFacing();
        BlockPos blockPos = ctx.getBlockPos();
        BlockPos blockPos2 = blockPos.offset(direction);
        World world = ctx.getWorld();

        if (world.getBlockState(blockPos2).canReplace(ctx) && world.getWorldBorder().contains(blockPos2)) {
            return this.getDefaultState().with(FACING, direction);
        }
        return null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            BlockPos blockPos = pos.offset(state.get(FACING));
            world.setBlockState(blockPos, state.with(PART, BedPart.HEAD), 3);
            world.updateNeighbors(pos, Blocks.AIR);
            state.updateNeighbors(world, pos, 3);
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PetBedBlockEntity(pos, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.PET_BED_BLOCK_ENTITY, PetBedBlockEntity::tick);
    }
}