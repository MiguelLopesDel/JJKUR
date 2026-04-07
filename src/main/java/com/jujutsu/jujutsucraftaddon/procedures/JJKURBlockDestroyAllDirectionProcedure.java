package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModBlocks;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.GetEntityFromUUIDProcedure;
import net.mcreator.jujutsucraft.procedures.ParticleGeneratorProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class JJKURBlockDestroyAllDirectionProcedure {

    private static final TagKey<Block> BARRIER_TAG =
            TagKey.create(Registries.BLOCK, new ResourceLocation("jujutsucraft:barrier"));
    private static final TagKey<Block> IMPERMEABLE_TAG =
            TagKey.create(Registries.BLOCK, new ResourceLocation("minecraft:impermeable"));

    private static SoundEvent SOUND_STONE, SOUND_WOOD, SOUND_GLASS, SOUND_WATER;
    private static boolean soundsCached = false;

    private static void ensureSoundsCached() {
        if (soundsCached) return;
        SOUND_STONE = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:stone_crash"));
        SOUND_WOOD  = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.zombie.break_wooden_door"));
        SOUND_GLASS = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:glass_crash"));
        SOUND_WATER = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:water_splash"));
        soundsCached = true;
    }

    private static final int FAST_SET_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public static void execute(LevelAccessor world, double x, double y, double z,
                               Entity entity, CallbackInfo ci) {
        if (ci != null) ci.cancel();
        if (entity == null) return;

        if (!world.getLevelData().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            cleanupData(entity);
            return;
        }

        ensureSoundsCached();

        GameRules.BooleanValue dropRule = world.getLevelData().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        boolean wasDropEnabled = dropRule.get();
        if (wasDropEnabled) dropRule.set(false, world.getServer());

        var data = entity.getPersistentData();
        boolean typeFlame  = data.getDouble("effect") == 3.0;
        double  knockback  = data.getDouble("knockback");
        double  damage     = data.getDouble("BlockDamage");
        double  blockRange = data.getDouble("BlockRange");
        boolean extinction = data.getBoolean("ExtinctionBlock");
        boolean noEffect   = data.getBoolean("noEffect");
        double  xKnock     = data.getDouble("x_knockback");
        double  yKnock     = data.getDouble("y_knockback");
        double  zKnock     = data.getDouble("z_knockback");

        boolean logicWater = !(entity instanceof Gravestone1Entity ||
                entity instanceof Gravestone2Entity ||
                entity instanceof Gravestone3Entity ||
                entity instanceof Gravestone4Entity ||
                entity instanceof RockFragmentEntity);

        boolean insideBarrier = checkInsideBarrier(entity, world);

        double RANGE    = Math.round(blockRange >= 1.0 ? blockRange * 2.0 : 1.0);
        double halfRange = RANGE * 0.5;
        double maxRadius = Math.max(halfRange, 1.0);
        double maxDistSq = maxRadius * maxRadius;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int rangeInt = (int) RANGE;
        double startX = Math.round(x - Math.floor(halfRange));
        double startY = Math.round(y - Math.floor(halfRange));
        double startZ = Math.round(z - Math.floor(halfRange));

        boolean rock = false, wood = false, glass = false, water = false;

        ServerLevel serverLevel = world instanceof ServerLevel sl ? sl : null;
        int cChunkX = Integer.MIN_VALUE, cChunkZ = Integer.MIN_VALUE;
        LevelChunk cachedChunk = null;

        long deadline = System.nanoTime() + 40_000_000L;
        int processCount = 0;

        outerLoop:
        for (int ix = 0; ix < rangeInt; ix++) {
            double xPos = startX + ix;
            double xDis = xPos - x;
            double xDisSq = xDis * xDis;
            if (xDisSq > maxDistSq) continue;

            for (int iy = 0; iy < rangeInt; iy++) {
                double yPos = startY + iy;
                double yDis = yPos - y;
                double xyDisSq = xDisSq + yDis * yDis;
                if (xyDisSq > maxDistSq) continue;

                for (int iz = 0; iz < rangeInt; iz++) {
                    double zPos = startZ + iz;
                    int bx = (int) xPos, by = (int) yPos, bz = (int) zPos;
                    pos.set(bx, by, bz);

                    int chX = bx >> 4, chZ = bz >> 4;
                    if (serverLevel != null && (chX != cChunkX || chZ != cChunkZ)) {
                        cChunkX = chX;
                        cChunkZ = chZ;
                        cachedChunk = serverLevel.getChunkSource().getChunkNow(chX, chZ);
                    }

                    BlockState blockState = cachedChunk != null
                            ? cachedChunk.getBlockState(pos)
                            : world.getBlockState(pos);
                    if (blockState.isAir()) continue;

                    double zDis = zPos - z;
                    double distSq = xyDisSq + zDis * zDis;
                    if (distSq > maxDistSq) continue;

                    double hardness = blockState.getDestroySpeed(world, pos);
                    if (blockState.is(BlockTags.LOGS) || blockState.is(BlockTags.PLANKS)) {
                        hardness *= 0.5;
                    }

                    boolean isLiquid  = blockState.getBlock() instanceof LiquidBlock || isWaterlogged(blockState);
                    boolean isBarrier = blockState.is(BARRIER_TAG);

                    boolean shouldDestroy = false;
                    if (isLiquid) shouldDestroy = true;
                    if (hardness >= 0.0 && hardness < damage) shouldDestroy = true;
                    if (isBarrier) {
                        shouldDestroy = !insideBarrier && damage >= 1.5;
                    }
                    if (!shouldDestroy) continue;

                    double distance = Math.sqrt(distSq);

                    int cntX = 0;
                    if (isOccludingOrSource(world, pos.east()))  cntX += 2;
                    if (isOccludingOrSource(world, pos.west()))  cntX += 2;
                    if (isOccludingOrSource(world, pos.south())) cntX += 2;
                    if (isOccludingOrSource(world, pos.north())) cntX += 2;
                    if (!isBarrier) {
                        if (isOccludingOrSource(world, pos.above())) cntX++;
                        if (isOccludingOrSource(world, pos.below())) cntX++;
                    }

                    if (blockState.getBlock() == Blocks.WATER && cntX < 10) {
                        if (knockback >= 0.0) {
                            ParticleGeneratorProcedure.execute(world,
                                    1.0, 4.0, 10.0,
                                    rng.nextDouble(0.5, 5.0) * knockback,
                                    xPos, xPos + xPos - x,
                                    yPos, yPos + Math.abs(yPos - y),
                                    zPos, zPos + zPos - z,
                                    "jujutsucraft:particle_water");
                        } else {
                            ParticleGeneratorProcedure.execute(world,
                                    1.0, 4.0, 10.0,
                                    rng.nextDouble(0.05, 0.5) * distance,
                                    xPos, x, yPos, y, zPos, z,
                                    "jujutsucraft:particle_water");
                        }
                        if (knockback >= 1.0) water = true;
                    }

                    if (cntX >= 8) continue;

                    if (!isLiquid) {
                        boolean isImpermeable = blockState.is(IMPERMEABLE_TAG);

                        if (!isImpermeable && typeFlame) {
                            if (knockback >= 0.0) {
                                ParticleGeneratorProcedure.execute(world,
                                        1.0, 1.0, 10.0,
                                        rng.nextDouble(0.5, 5.0) * knockback,
                                        xPos, xPos + xPos - x,
                                        yPos, yPos + yPos - y,
                                        zPos, zPos + zPos - z,
                                        "jujutsucraft:particle_magma");
                            } else {
                                ParticleGeneratorProcedure.execute(world,
                                        1.0, 1.0, 10.0,
                                        rng.nextDouble(0.2, 2.0) * distance,
                                        xPos, x, yPos, y, zPos, z,
                                        "jujutsucraft:particle_magma");
                            }
                        }

                        if (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                            if (knockback >= 0.0) {
                                ParticleGeneratorProcedure.execute(world,
                                        1.0, 1.0, 10.0,
                                        rng.nextDouble(0.1, 1.0) * knockback,
                                        xPos, xPos + xPos - x,
                                        yPos, yPos + yPos - y,
                                        zPos, zPos + zPos - z,
                                        "jujutsucraft:particle_big_smoke");
                            } else {
                                ParticleGeneratorProcedure.execute(world,
                                        1.0, 1.0, 10.0,
                                        rng.nextDouble(0.01, 0.1) * distance,
                                        xPos, x, yPos, y, zPos, z,
                                        "jujutsucraft:particle_big_smoke");
                            }
                            rock = true;

                        } else if (blockState.is(BlockTags.MINEABLE_WITH_AXE) && hardness > 0.0) {
                            wood = true;

                        } else if (isImpermeable ||
                                blockState.getBlock() == JujutsucraftModBlocks.JUJUTSU_BARRIER.get()) {
                            if (knockback >= 0.0) {
                                ParticleGeneratorProcedure.execute(world,
                                        1.0, 4.0, 10.0,
                                        rng.nextDouble(0.2, 2.0) * knockback,
                                        xPos, xPos + xPos - x,
                                        yPos, yPos + yPos - y,
                                        zPos, zPos + zPos - z,
                                        "jujutsucraft:particle_broken_glass_small");
                            } else {
                                ParticleGeneratorProcedure.execute(world,
                                        1.0, 4.0, 10.0,
                                        rng.nextDouble(0.025, 0.25) * distance,
                                        xPos, x, yPos, y, zPos, z,
                                        "jujutsucraft:particle_broken_glass_small");
                            }
                            glass = true;
                        }

                        if (isBarrier) {
                            handleBarrierDestruction(world, pos, blockState, distance);
                        } else if (!world.isClientSide()) {
                            world.setBlock(pos, Blocks.AIR.defaultBlockState(), FAST_SET_FLAGS);
                        }

                        if (!extinction) {
                            BlockState fallingState;
                            if (typeFlame && rng.nextDouble() < 0.1) {
                                fallingState = Blocks.FIRE.defaultBlockState();
                            } else if (!isBarrier && rng.nextDouble() < 0.1) {
                                fallingState = blockState;
                            } else {
                                fallingState = null;
                            }

                            if (fallingState != null && !fallingState.isAir()
                                    && !fallingState.is(IMPERMEABLE_TAG)) {
                                spawnFallingBlock(serverLevel, fallingState,
                                        xPos, yPos, zPos, x, y, z,
                                        distance, knockback,
                                        xKnock, yKnock, zKnock, rng);
                            }
                        }

                    } else if (logicWater) {
                        if (!world.isClientSide()) {
                            world.setBlock(pos, Blocks.AIR.defaultBlockState(), FAST_SET_FLAGS);
                        }
                        water = true;
                    }

                    processCount++;
                    if ((processCount & 63) == 0 && System.nanoTime() > deadline) {
                        break outerLoop;
                    }
                }
            }
        }

        if (!noEffect && world instanceof Level level) {
            float pitch = 0.9f + rng.nextFloat() * 0.2f;
            if (rock  && SOUND_STONE != null) playSound(level, x, y, z, SOUND_STONE, pitch);
            if (wood  && SOUND_WOOD  != null) playSound(level, x, y, z, SOUND_WOOD,  pitch);
            if (glass && SOUND_GLASS != null) playSound(level, x, y, z, SOUND_GLASS, pitch);
            if (water && SOUND_WATER != null) playSound(level, x, y, z, SOUND_WATER, pitch);
        }

        if (wasDropEnabled) dropRule.set(true, world.getServer());
        cleanupData(entity);
    }

    private static boolean isOccludingOrSource(LevelAccessor world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.canOcclude() || state.getFluidState().isSource();
    }

    private static void spawnFallingBlock(ServerLevel level, BlockState state,
                                          double xPos, double yPos, double zPos,
                                          double ox, double oy, double oz,
                                          double distance, double knockback,
                                          double xKnock, double yKnock, double zKnock,
                                          ThreadLocalRandom rng) {
        if (level == null) return;

        FallingBlockEntity falling = new FallingBlockEntity(
                level,
                Math.round(xPos) + 0.5,
                Math.round(yPos),
                Math.round(zPos) + 0.5,
                state);
        falling.time = 560;
        falling.dropItem = false;

        if (distance != 0.0) {
            double speed = rng.nextDouble(0.1, 1.0) * Math.min(knockback, 3.0);
            double invDist = 1.0 / distance;
            falling.setDeltaMovement(new Vec3(
                    (xPos - ox) * invDist * speed + xKnock,
                    (yPos - oy) * invDist * speed + yKnock,
                    (zPos - oz) * invDist * speed + zKnock));
        }

        level.addFreshEntity(falling);
    }

    private static boolean isWaterlogged(BlockState state) {
        Property<?> prop = state.getBlock().getStateDefinition().getProperty("waterlogged");
        return prop instanceof BooleanProperty bp && (Boolean) state.getValue(bp);
    }

    private static void handleBarrierDestruction(LevelAccessor world, BlockPos pos,
                                                 BlockState state, double distance) {
        if (world.isClientSide()) return;
        BlockEntity be = world.getBlockEntity(pos);
        String oldBlock = be != null ? be.getPersistentData().getString("old_block") : "";

        world.setBlock(pos, Objects.requireNonNull(
                        ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jujutsucraft:domain_hole")))
                .defaultBlockState(), 3);

        BlockEntity newBe = world.getBlockEntity(pos);
        if (newBe != null) {
            newBe.getPersistentData().putString("old_block", oldBlock);
            newBe.getPersistentData().putString("old_barrier",
                    (state + "").replace("}", "").replace("Block{", ""));
            newBe.getPersistentData().putDouble("delay_time", distance);
        }
    }

    private static boolean checkInsideBarrier(Entity entity, LevelAccessor world) {
        if (entity instanceof PurpleEntity || entity instanceof FlameArrowEntity ||
                entity instanceof PureLoveCannonEntity || entity instanceof UzumakiEntity ||
                entity instanceof MeteorEntity || entity instanceof BlackHoleEntity) return false;

        if (entity instanceof ProjectileSlashEntity pSlash &&
                pSlash.getEntityData().get(ProjectileSlashEntity.DATA_mode) == 1) return false;

        if (entity instanceof LivingEntity living) {
            if (living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()))
                return true;
            if (living.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()) &&
                    living.getEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get())
                            .getAmplifier() >= 10)
                return true;
        }

        String ownerUuid = entity.getPersistentData().getString("OWNER_UUID");
        if (!ownerUuid.isEmpty() && entity.getType().is(
                TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
            Entity owner = GetEntityFromUUIDProcedure.execute(world, ownerUuid);
            if (!(owner instanceof LivingEntity)) owner = entity;
            if (owner instanceof LivingEntity lo) {
                return lo.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) ||
                        (lo.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()) &&
                                lo.getEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get())
                                        .getAmplifier() >= 10);
            }
        }
        return false;
    }

    private static void playSound(Level level, double x, double y, double z,
                                  SoundEvent sound, float pitch) {
        if (!level.isClientSide()) {
            level.playSound(null, x, y, z, sound, SoundSource.NEUTRAL, 1.0F, pitch);
        } else {
            level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, 1.0F, pitch, false);
        }
    }

    private static void cleanupData(Entity entity) {
        var d = entity.getPersistentData();
        d.putDouble("BlockRange", 0.0);
        d.putDouble("BlockDamage", 0.0);
        d.putDouble("effect", 0.0);
        d.putBoolean("noParticle", false);
        d.putBoolean("noEffect", false);
        d.putBoolean("ExtinctionBlock", false);
    }
}