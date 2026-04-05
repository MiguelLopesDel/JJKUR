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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

public class JJKURBlockDestroyAllDirectionProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (!world.getLevelData().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            cleanupData(entity);
            return;
        }

        boolean wasDropEnabled = world.getLevelData().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
        if (wasDropEnabled) {
            ((GameRules.BooleanValue) world.getLevelData().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS)).set(false, world.getServer());
        }

        // Lógica de Água: Pedras e Lápides não destroem água
        boolean logicWater = !(entity instanceof Gravestone1Entity || entity instanceof Gravestone2Entity || 
                               entity instanceof Gravestone3Entity || entity instanceof Gravestone4Entity || 
                               entity instanceof RockFragmentEntity);

        boolean insideBarrier = checkInsideBarrier(entity, world);
        boolean typeFlame = entity.getPersistentData().getDouble("effect") == 3.0;
        double knockback = entity.getPersistentData().getDouble("knockback");
        double damage = entity.getPersistentData().getDouble("BlockDamage");
        double blockRange = entity.getPersistentData().getDouble("BlockRange");
        double range = Math.round(blockRange >= 1.0 ? blockRange * 2.0 : 1.0);
        double radiusSq = (range * 0.5) * (range * 0.5);

        DestructionContext ctx = new DestructionContext();
        handleLoopDestruction(world, x, y, z, entity, range, radiusSq, damage, insideBarrier, typeFlame, knockback, logicWater, ctx);

        if (!entity.getPersistentData().getBoolean("noEffect")) {
            playDestructionSounds(world, x, y, z, ctx);
        }

        if (wasDropEnabled) {
            ((GameRules.BooleanValue) world.getLevelData().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS)).set(true, world.getServer());
        }
        cleanupData(entity);
    }

    private static void handleLoopDestruction(LevelAccessor world, double ox, double oy, double oz, Entity entity, 
                                              double range, double radiusSq, double damage, boolean insideBarrier, 
                                              boolean typeFlame, double knockback, boolean logicWater, DestructionContext ctx) {
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int halfRange = (int) Math.floor(range * 0.5);
        int minX = (int) Math.round(ox - halfRange);
        int minY = (int) Math.round(oy - halfRange);
        int minZ = (int) Math.round(oz - halfRange);

        for (int dx = 0; dx < (int) range; dx++) {
            double curX = minX + dx;
            double xDistSq = (curX - ox) * (curX - ox);
            for (int dy = 0; dy < (int) range; dy++) {
                double curY = minY + dy;
                double yDistSq = (curY - oy) * (curY - oy);
                for (int dz = 0; dz < (int) range; dz++) { // FIX: Corrigido dx para dz
                    double curZ = minZ + dz;
                    double totalDistSq = xDistSq + yDistSq + (curZ - oz) * (curZ - oz);

                    if (totalDistSq <= radiusSq) {
                        pos.set(curX, curY, curZ);
                        BlockState state = world.getBlockState(pos);
                        if (state.isAir()) continue;

                        if (shouldDestroy(world, pos, state, damage, insideBarrier, logicWater)) {
                            processSingleBlock(world, pos, state, entity, totalDistSq, ox, oy, oz, typeFlame, knockback, ctx);
                        }
                    }
                }
            }
        }
    }

    private static boolean shouldDestroy(LevelAccessor world, BlockPos pos, BlockState state, double damage, boolean insideBarrier, boolean logicWater) {
        if (state.getBlock() instanceof LiquidBlock || isWaterlogged(state)) {
            return logicWater;
        }

        double hardness = state.getDestroySpeed(world, pos);
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) hardness *= 0.5;

        if (hardness >= 0.0 && hardness < damage) return true;

        if (state.is(TagKey.create(Registries.BLOCK, new ResourceLocation("jujutsucraft:barrier")))) {
            return !insideBarrier && damage >= 1.5;
        }

        return false;
    }

    private static void processSingleBlock(LevelAccessor world, BlockPos pos, BlockState state, Entity entity, 
                                           double distSq, double ox, double oy, double oz, boolean typeFlame, 
                                           double knockback, DestructionContext ctx) {
        updateDestructionContext(state, ctx);
        handleMaterialParticles(world, pos, ox, oy, oz, state, typeFlame, knockback, Math.sqrt(distSq));

        if (state.is(TagKey.create(Registries.BLOCK, new ResourceLocation("jujutsucraft:barrier")))) {
            handleBarrierDestruction(world, pos, state, distSq);
        } else {
            if (!world.isClientSide()) world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }

        if (!entity.getPersistentData().getBoolean("ExtinctionBlock")) {
            spawnFallingBlockNatively(world, pos, state, entity, distSq, ox, oy, oz, typeFlame, knockback);
        }
    }

    private static void spawnFallingBlockNatively(LevelAccessor world, BlockPos pos, BlockState state, Entity owner, 
                                                  double distSq, double ox, double oy, double oz, boolean typeFlame, double knockback) {
        if (!(world instanceof ServerLevel level)) return;

        BlockState resultState = state;
        if (typeFlame && Math.random() < 0.1) resultState = Blocks.FIRE.defaultBlockState();
        else if (Math.random() >= 0.1) return;

        if (resultState.isAir() || resultState.is(BlockTags.create(new ResourceLocation("minecraft:impermeable")))) return;

        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, resultState);
        falling.time = 560;
        falling.dropItem = false;
        // FIX: Não chamamos setHurtsEntities para manter o dano como false (valor padrão)
        
        double dist = Math.sqrt(distSq);
        if (dist > 0) {
            double speed = Mth.nextDouble(level.getRandom(), 0.1, 1.0) * Math.min(knockback, 3.0);
            falling.setDeltaMovement(new Vec3(
                (pos.getX() - ox) / dist * speed + owner.getPersistentData().getDouble("x_knockback"),
                (pos.getY() - oy) / dist * speed + owner.getPersistentData().getDouble("y_knockback"),
                (pos.getZ() - oz) / dist * speed + owner.getPersistentData().getDouble("z_knockback")
            ));
        }
        level.addFreshEntity(falling);
    }

    private static boolean checkInsideBarrier(Entity entity, LevelAccessor world) {
        // Bypass para Golpes Supremos
        if (entity instanceof PurpleEntity || entity instanceof FlameArrowEntity || 
            entity instanceof PureLoveCannonEntity || entity instanceof UzumakiEntity || 
            entity instanceof MeteorEntity || entity instanceof BlackHoleEntity) {
            return false;
        }
        if (entity instanceof ProjectileSlashEntity pSlash && pSlash.getEntityData().get(ProjectileSlashEntity.DATA_mode) == 1) {
            return false;
        }

        if (entity instanceof LivingEntity living && living.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) return true;
        if (entity instanceof LivingEntity living && (living.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()) && Objects.requireNonNull(living.getEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get())).getAmplifier() >= 10)) return true;

        String ownerUuid = entity.getPersistentData().getString("OWNER_UUID");
        if (!ownerUuid.isEmpty() && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
            Entity owner = GetEntityFromUUIDProcedure.execute(world, ownerUuid);
            if (owner instanceof LivingEntity livingOwner) {
                return livingOwner.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) ||
                       (livingOwner.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()) && Objects.requireNonNull(livingOwner.getEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get())).getAmplifier() >= 10);
            }
        }
        return false;
    }

    private static void handleBarrierDestruction(LevelAccessor world, BlockPos pos, BlockState state, double distSq) {
        if (world.isClientSide()) return;
        BlockEntity be = world.getBlockEntity(pos);
        String old = be != null ? be.getPersistentData().getString("old_block") : "";
        world.setBlock(pos, ((Block) Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jujutsucraft:domain_hole")))).defaultBlockState(), 3);
        BlockEntity newBe = world.getBlockEntity(pos);
        if (newBe != null) {
            newBe.getPersistentData().putString("old_block", old);
            newBe.getPersistentData().putString("old_barrier", state.toString().replaceAll("Block\\{|}", ""));
            newBe.getPersistentData().putDouble("delay_time", Math.sqrt(distSq));
        }
    }

    private static boolean isWaterlogged(BlockState state) {
        Property<?> prop = state.getBlock().getStateDefinition().getProperty("waterlogged");
        return prop instanceof BooleanProperty && (Boolean) state.getValue((BooleanProperty) prop);
    }

    private static void updateDestructionContext(BlockState state, DestructionContext ctx) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) ctx.rock = true;
        else if (state.is(BlockTags.MINEABLE_WITH_AXE)) ctx.wood = true;
        else if (state.is(BlockTags.create(new ResourceLocation("minecraft:impermeable"))) || state.getBlock() == JujutsucraftModBlocks.JUJUTSU_BARRIER.get()) ctx.glass = true;
        if (state.getBlock() == Blocks.WATER) ctx.water = true;
    }

    private static void handleMaterialParticles(LevelAccessor world, BlockPos pos, double ox, double oy, double oz, BlockState state, boolean typeFlame, double knockback, double dist) {
        if (state.getBlock() == Blocks.WATER) spawnLogicParticles(world, pos, ox, oy, oz, knockback, dist, "jujutsucraft:particle_water");
        if (typeFlame && !state.is(BlockTags.create(new ResourceLocation("minecraft:impermeable")))) spawnLogicParticles(world, pos, ox, oy, oz, knockback, dist, "jujutsucraft:particle_magma");
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) spawnLogicParticles(world, pos, ox, oy, oz, knockback, dist, "jujutsucraft:particle_big_smoke");
    }

    private static void spawnLogicParticles(LevelAccessor world, BlockPos pos, double ox, double oy, double oz, double knockback, double dist, String particleId) {
        // CORREÇÃO DE FPS: Só spawna partículas para 25% dos blocos destruídos (reduz drasticamente o lag visual)
        if (Math.random() > 0.25) return;

        double px = pos.getX(); double py = pos.getY(); double pz = pos.getZ();
        if (knockback >= 0) ParticleGeneratorProcedure.execute(world, 1.0, 1.0, 10.0, (0.5 + Math.random() * 4.5) * knockback, px, px + px - ox, py, py + Math.abs(py - oy), pz, pz + pz - oz, particleId);
        else ParticleGeneratorProcedure.execute(world, 1.0, 1.0, 10.0, (0.05 + Math.random() * 0.45) * dist, px, ox, py, oy, pz, oz, particleId);
    }

    private static void playDestructionSounds(LevelAccessor world, double x, double y, double z, DestructionContext ctx) {
        if (!(world instanceof Level level)) return;
        float pitch = (float) (0.9 + Math.random() * 0.2);
        if (ctx.rock) playMaterialSound(level, x, y, z, "jujutsucraft:stone_crash", pitch);
        if (ctx.wood) playMaterialSound(level, x, y, z, "entity.zombie.break_wooden_door", pitch);
        if (ctx.glass) playMaterialSound(level, x, y, z, "jujutsucraft:glass_crash", pitch);
        if (ctx.water) playMaterialSound(level, x, y, z, "jujutsucraft:water_splash", pitch);
    }

    private static void playMaterialSound(Level level, double x, double y, double z, String soundId, float pitch) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundId));
        if (sound == null) return;
        if (!level.isClientSide()) level.playSound(null, x, y, z, sound, SoundSource.NEUTRAL, 1.0F, pitch);
        else level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, 1.0F, pitch, false);
    }

    private static void cleanupData(Entity entity) {
        entity.getPersistentData().putDouble("BlockRange", 0.0);
        entity.getPersistentData().putDouble("BlockDamage", 0.0);
        entity.getPersistentData().putDouble("effect", 0.0);
        entity.getPersistentData().putBoolean("noParticle", false);
        entity.getPersistentData().putBoolean("noEffect", false);
        entity.getPersistentData().putBoolean("ExtinctionBlock", false);
    }

    private static class DestructionContext {
        boolean rock, wood, glass, water;
    }
}
