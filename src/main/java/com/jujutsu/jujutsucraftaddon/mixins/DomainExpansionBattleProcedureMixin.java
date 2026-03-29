package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.DomainExpansionEntityEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModBlocks;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.DomainExpansionBattleProcedure;
import net.mcreator.jujutsucraft.procedures.GetDomainBlockProcedure;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

@Mixin(value = DomainExpansionBattleProcedure.class, priority = -10000)
public abstract class DomainExpansionBattleProcedureMixin {
    /**
     * @author Satushi / Refactored for v43 with AI Preservation
     * @reason Support Barrierless Domains and Custom Radius while following v43 logic
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity != null) {
            boolean logic_a = false;
            boolean failed = false;
            boolean noBarrier = false;
            Entity entity_a = null;
            double x_dis = 0.0;
            double x_dis_p = 0.0;
            double cnt2 = 0.0;
            double range = 0.0;
            double x_pos = 0.0;
            double z_dis = 0.0;
            double domain_num = 0.0;
            double z_pos = 0.0;
            double dis_p = 0.0;
            double dis = 0.0;
            double loop_num = 0.0;
            double y_floor = 0.0;
            double y_dis_p = 0.0;
            double y_pos = 0.0;
            double y_dis = 0.0;
            double z_dis_p = 0.0;
            double close_type = 0.0;
            double speed = 0.0;
            String inside = "";
            String outside = "";
            String old_block = "";
            String floor = "";
            String cnt_type = "";
            BlockState blockstate1 = Blocks.AIR.defaultBlockState();

            domain_num = entity.getPersistentData().getDouble("skill_domain") > 0.0
                    ? entity.getPersistentData().getDouble("skill_domain")
                    : entity.getPersistentData().getDouble("select");

            close_type = !(entity instanceof LivingEntity _livEnt3 && _livEnt3.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()))
                    ? entity.getPersistentData().getDouble("cnt2")
                    : (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                    ? _livEnt.getEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier()
                    : 0.0);

            GetDomainBlockProcedure.execute(entity);
            outside = entity.getPersistentData().getString("domain_outside");
            inside = entity.getPersistentData().getString("domain_inside");
            floor = entity.getPersistentData().getString("domain_floor");
            noBarrier = close_type > 0.0;
            failed = entity.getPersistentData().getBoolean("Failed") && !entity.getPersistentData().getBoolean("Cover");

            if (failed && entity instanceof LivingEntity _livEnt11 && _livEnt11.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                range = 5.0;
                x_pos = Math.round(x - range);

                for (int index0 = 0; index0 < (int) Math.round(range * 2.0); index0++) {
                    x_dis = Math.round(x) - x_pos;
                    x_dis *= x_dis;
                    y_pos = Math.round(y - range);

                    for (int index1 = 0; index1 < (int) Math.round(range * 2.0); index1++) {
                        y_dis = Math.round(y) - y_pos;
                        y_dis *= y_dis;
                        z_pos = Math.round(z - range);

                        for (int index2 = 0; index2 < (int) Math.round(range * 2.0); index2++) {
                            z_dis = Math.round(z) - z_pos;
                            z_dis *= z_dis;
                            if (Math.random() < 0.1) {
                                BlockPos currentPos = BlockPos.containing(x_pos, y_pos, z_pos);
                                blockstate1 = world.getBlockState(currentPos);
                                if (blockstate1.is(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))
                                        && blockstate1.getBlock() != JujutsucraftModBlocks.IN_BARRIER.get()
                                        && blockstate1.getBlock() != JujutsucraftModBlocks.JUJUTSU_BARRIER.get()) {
                                    outside = (blockstate1 + "").replace("}", "").replace("Block{", "");
                                    if (!floor.equals(outside)) {
                                        logic_a = true;
                                            if (!world.isClientSide()) {
                                                BlockEntity be = world.getBlockEntity(currentPos);
                                                old_block = be != null ? be.getPersistentData().getString("old_block") : "";
                                                placeBlockSafeMixin(world, currentPos, floor);
                                                BlockEntity newBe = world.getBlockEntity(currentPos);
                                                if (newBe != null) {
                                                    newBe.getPersistentData().putString("old_block", old_block);
                                                }
                                            }

                                            if (Math.random() < 0.01) {
                                                world.levelEvent(2001, currentPos, Block.getId(blockstate1));
                                            }
                                            break;
                                    }
                                }
                            }
                            z_pos++;
                        }
                        y_pos++;
                        if (logic_a) break;
                    }
                    x_pos++;
                    if (logic_a) break;
                }
            }

            if (!failed) {
                x_pos = entity.getPersistentData().getDouble("x_pos_doma");
                y_pos = entity.getPersistentData().getDouble("y_pos_doma");
                z_pos = entity.getPersistentData().getDouble("z_pos_doma");
                if (close_type <= 0.0 && (entity.getPersistentData().getDouble("cnt1") <= 1.0 || entity.getPersistentData().getDouble("cnt_cover") <= 1.0)) {
                    List<DomainExpansionEntityEntity> domainEntities = world.getEntitiesOfClass(
                            DomainExpansionEntityEntity.class, new AABB(x_pos - 0.1, y_pos - 0.1, z_pos - 0.1, x_pos + 0.1, y_pos + 0.1, z_pos + 0.1), e -> true
                    );
                    if (!domainEntities.isEmpty()) {
                        DomainExpansionEntityEntity d_ent = domainEntities.get(0);
                        if (!d_ent.getPersistentData().getBoolean("Break") && !d_ent.level().isClientSide()) {
                            d_ent.discard();
                        }
                    }

                    if (world instanceof ServerLevel _level) {
                        Entity entityToSpawn = ((EntityType) JujutsucraftModEntities.DOMAIN_EXPANSION_ENTITY.get())
                                .spawn(_level, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED);
                        if (entityToSpawn != null) {
                            entityToSpawn.setYRot(world.getRandom().nextFloat() * 360.0F);
                        }
                    }
                }

                speed = domain_num == 29.0 ? 10 : (entity instanceof LivingEntity le && le.hasEffect((MobEffect) JujutsucraftModMobEffects.ZONE.get()) ? 3 : 1);

                // Addon Logic: Radius Domain
                if ((entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables())).BarrierlessDomain) {
                    range = (entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables())).RadiusDomain;
                } else {
                    range = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius;
                }

                loop_num = Math.round(range * 2.0 + 1.0);
                cnt_type = entity.getPersistentData().getBoolean("Cover") ? "cnt_cover" : "cnt1";
                cnt2 = entity.getPersistentData().getDouble(cnt_type) * speed;
                if (cnt2 - speed + 1.0 <= loop_num) {
                    y_floor = entity.getPersistentData().getDouble("y_pos_doma") - 1.0;
                    x_pos = Math.round(x) - range;

                    for (int index3 = 0; index3 < (int) loop_num; index3++) {
                        x_dis = Math.round(x) - x_pos;
                        x_dis *= x_dis;
                        x_dis_p = x_pos - Math.round(entity.getPersistentData().getDouble("x_pos_doma2"));
                        x_dis_p *= x_dis_p;
                        if (x_dis_p <= cnt2 * cnt2) {
                            y_pos = Math.round(y) - range;

                            for (int index4 = 0; index4 < (int) loop_num; index4++) {
                                y_dis = Math.round(y) - y_pos;
                                y_dis *= y_dis;
                                y_dis_p = y_pos - Math.round(entity.getPersistentData().getDouble("y_pos_doma2"));
                                y_dis_p *= y_dis_p;
                                if (y_dis_p <= cnt2 * cnt2 && y_pos >= -64.0 && y_pos <= 319.0) {
                                    z_pos = Math.round(z) - range;

                                    for (int index5 = 0; index5 < (int) loop_num; index5++) {
                                        z_dis = Math.round(z) - z_pos;
                                        z_dis *= z_dis;
                                        z_dis_p = z_pos - Math.round(entity.getPersistentData().getDouble("z_pos_doma2"));
                                        z_dis_p *= z_dis_p;
                                        if (z_dis_p <= cnt2 * cnt2) {
                                            dis_p = x_dis_p + y_dis_p + z_dis_p;
                                            if (dis_p <= cnt2 * cnt2 && dis_p >= (cnt2 - speed) * (cnt2 - speed)) {
                                                dis = x_dis + z_dis + y_dis;
                                                if (dis < (range + 0.5) * (range + 0.5)) {
                                                    BlockPos currentPos = BlockPos.containing(x_pos, y_pos, z_pos);
                                                    blockstate1 = world.getBlockState(currentPos);
                                                    if (blockstate1.is(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))) {
                                                        BlockEntity be = world.getBlockEntity(currentPos);
                                                        old_block = be != null ? be.getPersistentData().getString("old_block") : "";
                                                    } else {
                                                        old_block = (blockstate1 + "").replace("}", "").replace("Block{", "");
                                                    }

                                                    if (noBarrier) {
                                                        if (entity.getPersistentData().getBoolean("Cover")
                                                                && blockstate1.is(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))) {
                                                            placeBlockSafeMixin(world, currentPos, y_pos <= y_floor ? old_block : inside);
                                                        }
                                                    } else {
                                                        logic_a = false;
                                                        if (blockstate1.is(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))) {
                                                            logic_a = true;
                                                        } else if (!(y_pos > y_floor - (domain_num != 8.0 && domain_num != 21.0 ? 1 : 2))
                                                                && (!(dis < (range + 0.0) * (range + 0.0)) || !(dis >= (range - 1.0) * (range - 1.0)))) {
                                                            if (!blockstate1.canOcclude()) {
                                                                logic_a = true;
                                                            } else if (!world.getBlockState(currentPos.east()).canOcclude()
                                                                    || !world.getBlockState(currentPos.west()).canOcclude()) {
                                                                logic_a = true;
                                                            } else if (!world.getBlockState(currentPos.above()).canOcclude()
                                                                    || !world.getBlockState(currentPos.below()).canOcclude()) {
                                                                logic_a = true;
                                                            } else if (!world.getBlockState(currentPos.south()).canOcclude()
                                                                    || !world.getBlockState(currentPos.north()).canOcclude()) {
                                                                logic_a = true;
                                                            }
                                                        } else {
                                                            logic_a = true;
                                                        }

                                                        if (logic_a) {
                                                            if (dis < range * range) {
                                                                if (dis >= (range - 1.0) * (range - 1.0)) {
                                                                    placeBlockSafeMixin(world, currentPos, outside);
                                                                } else if (dis >= (range - 2.0) * (range - 2.0)) {
                                                                    if (domain_num == 1.0) {
                                                                        if (y_pos != y_floor && Math.abs(x_pos - Math.round(x)) % 5.0 != 2.0) {
                                                                            placeBlockSafeMixin(world, currentPos, "jujutsucraft:in_barrier");
                                                                        } else {
                                                                            placeBlockSafeMixin(world, currentPos, inside);
                                                                        }
                                                                    } else if (domain_num == 15.0) {
                                                                        if (y_pos != y_floor
                                                                                && Math.abs(x_pos - Math.round(x)) % 5.0 != 2.0
                                                                                && Math.abs(y_pos - Math.round(y)) % 5.0 != 2.0
                                                                                && Math.abs(z_pos - Math.round(z)) % 5.0 != 4.0) {
                                                                            placeBlockSafeMixin(world, currentPos, "jujutsucraft:in_barrier");
                                                                        } else {
                                                                            placeBlockSafeMixin(world, currentPos, inside);
                                                                        }
                                                                    } else {
                                                                        placeBlockSafeMixin(world, currentPos, inside);
                                                                    }
                                                                } else if (y_pos <= y_floor) {
                                                                    if (y_pos >= y_floor - 4.0) {
                                                                        if (domain_num == 8.0) {
                                                                            if (dis_p < range * 0.675 * (range * 0.675)) {
                                                                                placeBlockSafeMixin(world, currentPos, "jujutsucraft:domain_grass");
                                                                            } else if (!(dis_p < range * 0.9 * (range * 0.9)) && !(y_pos < y_floor - 0.5)) {
                                                                                placeBlockSafeMixin(world, currentPos, "jujutsucraft:domain_water");
                                                                            } else {
                                                                                placeBlockSafeMixin(world, currentPos, "jujutsucraft:domain_sand");
                                                                            }
                                                                        } else if (domain_num == 21.0) {
                                                                            if (y_pos < y_floor - 0.5) {
                                                                                if (x_pos > x + range * 0.5 && y_pos >= y_floor - 1.5) {
                                                                                    placeBlockSafeMixin(world, currentPos, "jujutsucraft:domain_water");
                                                                                } else {
                                                                                    placeBlockSafeMixin(world, currentPos, floor);
                                                                                }
                                                                            } else if (x_pos < x + range * 0.5 - 0.0 && x_pos >= x + range * 0.5 - 1.0) {
                                                                                placeBlockSafeMixin(world, currentPos, "jujutsucraft:domain_fence");
                                                                            } else {
                                                                                placeBlockSafeMixin(world, currentPos, "jujutsucraft:in_barrier");
                                                                            }
                                                                        } else if (domain_num == 27.0) {
                                                                            placeBlockSafeMixin(world, currentPos, dis < range * 0.5 * range * 0.5 ? floor : "jujutsucraft:domain_stone_bricks");
                                                                        } else {
                                                                            placeBlockSafeMixin(world, currentPos, floor);
                                                                        }
                                                                    } else {
                                                                        logic_a = false;
                                                                    }
                                                                } else if (!world.isEmptyBlock(currentPos)) {
                                                                    if (close_type < 0.0
                                                                            && !entity.getPersistentData().getBoolean("Cover")
                                                                            && !entity.getPersistentData().getBoolean("Failed")) {
                                                                        placeBlockSafeMixin(world, currentPos, floor);
                                                                    } else {
                                                                        placeBlockSafeMixin(world, currentPos, "jujutsucraft:in_barrier");
                                                                    }
                                                                } else {
                                                                    logic_a = false;
                                                                }
                                                            } else {
                                                                logic_a = false;
                                                            }
                                                        }

                                                        if ((y_pos <= y_floor || dis < range * range && dis >= (range - 2.0) * (range - 2.0)) && !world.isClientSide()) {
                                                            for (Entity ent : world.getEntitiesOfClass(
                                                                    Entity.class, new AABB(x_pos, y_pos, z_pos, x_pos + 1.0, y_pos + 1.0, z_pos + 1.0), e -> true
                                                            )) {
                                                                if (!ent.isInvulnerable()
                                                                        && !ent.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:not_living")))) {
                                                                    ent.teleportTo(ent.getX(), y_floor + 1.0, ent.getZ());
                                                                    if (ent instanceof ServerPlayer _serverPlayer) {
                                                                        _serverPlayer.connection.teleport(ent.getX(), y_floor + 1.0, ent.getZ(), ent.getYRot(), ent.getXRot());
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (logic_a
                                                                && world.getBlockState(currentPos).is(BlockTags.create(new ResourceLocation("jujutsucraft:barrier")))
                                                                && !world.isClientSide()) {
                                                            BlockEntity _blockEntity = world.getBlockEntity(currentPos);
                                                            if (_blockEntity != null) {
                                                                _blockEntity.getPersistentData().putString("old_block", old_block);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        z_pos++;
                                    }
                                }
                                y_pos++;
                            }
                        }
                        x_pos++;
                    }
                } else {
                    entity.getPersistentData()
                            .putDouble(
                                    cnt_type,
                                    entity.getPersistentData().getDouble(cnt_type) < 34.0 ? 34.0 : Math.max(entity.getPersistentData().getDouble(cnt_type), loop_num)
                            );
                }
            }
        }
    }

    @Unique
    private static void placeBlockSafeMixin(LevelAccessor world, BlockPos pos, String blockName) {
        if (!world.isClientSide() && blockName != null && !blockName.isEmpty()) {
            try {
                BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(world.holderLookup(Registries.BLOCK), blockName, true);
                world.setBlock(pos, result.blockState(), 3);
            } catch (Exception var7) {
                String cleanName = blockName.contains("[") ? blockName.substring(0, blockName.indexOf("[")) : blockName;
                ResourceLocation res = new ResourceLocation(cleanName.contains(":") ? cleanName : "minecraft:" + cleanName);
                Block block = ForgeRegistries.BLOCKS.getValue(res);
                if (block != null) {
                    world.setBlock(pos, block.defaultBlockState(), 3);
                }
            }
        }
    }
}
