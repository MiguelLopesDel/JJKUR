package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;
import net.mcreator.jujutsucraft.procedures.CanSeeSukunaSlashProcedure;
import net.mcreator.jujutsucraft.procedures.RangeAttackProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class JJKURMalevolentShrineActiveProcedure {

    private static SimpleParticleType PARTICLE_SLASH_LARGE;
    private static boolean particleCached = false;

    public static void onExecute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (ci != null) ci.cancel();
        if (entity == null) return;

        LivingEntity living = (entity instanceof LivingEntity le) ? le : null;
        MobEffectInstance domainEffect = (living != null)
                ? living.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                : null;
        if (domainEffect == null) return;

        int amplifier = domainEffect.getAmplifier();
        int duration = domainEffect.getDuration();
        double range = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius
                * (amplifier > 0 ? 18 : 2);

        CompoundTag nbt = entity.getPersistentData();

        handleEntityAttacks(world, entity, nbt, range, duration);

        if (amplifier > 0) {
            handleBlockDestruction(world, entity, nbt, range);

            updateDustOverlay(entity, nbt, duration);

            if (world instanceof ServerLevel serverLevel) {
                spawnAshParticles(serverLevel, nbt, range);
            }
            handleSlashParticles(world, entity, nbt, range);
        }
    }

    private static void handleEntityAttacks(LevelAccessor world, Entity entity,
                                            CompoundTag nbt, double range, int duration) {
        double oldSkill = nbt.getDouble("skill");
        double oldCooldown = nbt.getDouble("COOLDOWN_TICKS");
        double xCenter = nbt.getDouble("x_pos_doma");
        double yCenter = nbt.getDouble("y_pos_doma");
        double zCenter = nbt.getDouble("z_pos_doma");

        nbt.putDouble("y_knockback", -0.1);
        nbt.putDouble("Range", range * 0.5);
        nbt.putDouble("knockback", 0.001);
        nbt.putBoolean("swing", false);
        nbt.putBoolean("attack", false);
        nbt.putBoolean("ExtinctionBlock", true);

        for (int i = 0; i < 4; i++) {
            double n1 = (i % 2 == 0) ? 1.0 : -1.0;
            double n2 = (i < 2) ? 1.0 : -1.0;
            double px = xCenter + range * 0.25 * n1;
            double py = yCenter + range * 0.25 - 15.0;
            double pz = zCenter + range * 0.25 * n2;

            if (duration % 2 == 1) {
                nbt.putDouble("skill", 105.0);
                nbt.putDouble("COOLDOWN_TICKS", 50.0);
            } else {
                nbt.putDouble("skill", 106.0);
                nbt.putDouble("COOLDOWN_TICKS", 100.0);
            }
            nbt.putDouble("Damage", 10.5 + (duration % 10) * 0.1);
            nbt.putDouble("effect", 1.0);
            nbt.putDouble("effectConfirm", 1.0);
            nbt.putBoolean("DomainAttack", true);

            RangeAttackProcedure.execute(world, px, py, pz, entity);
        }

        nbt.putBoolean("ExtinctionBlock", false);
        nbt.putDouble("y_knockback", 0.0);
        nbt.putDouble("skill", oldSkill);
        nbt.putDouble("COOLDOWN_TICKS", oldCooldown);
    }

    private static void handleBlockDestruction(LevelAccessor world, Entity entity,
                                               CompoundTag nbt, double range) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        double xCenter = nbt.getDouble("x_pos_doma");
        double yCenter = nbt.getDouble("y_pos_doma");
        double zCenter = nbt.getDouble("z_pos_doma");

        if (nbt.getDouble("dust_amount") <= 0.0) {
            nbt.putDouble("dust_amount", 1.0);
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double iterCount = 0.0;

        for (int i = 0; i < 512; i++) {
            double angle = Math.toRadians(rng.nextDouble() * 360.0);
            double dist = range * 0.5 * (rng.nextDouble() * 2.0 - 1.0);
            double px = xCenter + Math.sin(angle) * dist;
            double py = yCenter + rng.nextDouble() * range * 0.2;
            double pz = zCenter + Math.cos(angle) * dist;

            if (!world.isEmptyBlock(BlockPos.containing(px, py, pz))) {
                nbt.putDouble("dust_amount",
                        Math.min(nbt.getDouble("dust_amount") + 1.0, 200.0));
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        px, py, pz, 2, 1.5, 1.5, 1.5, 0.0);
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        px, py, pz, 2, 1.5, 1.5, 1.5, 0.5);
                nbt.putBoolean("noParticle", true);
                nbt.putBoolean("ExtinctionBlock", true);
                nbt.putDouble("BlockRange", 16.0);
                nbt.putDouble("BlockDamage", 99.0);

                BlockDestroyAllDirectionProcedure.execute(
                        world, px, Math.max(py, yCenter + 8.0), pz, entity);

                if (iterCount < 32.0 || iterCount / 512.0 < rng.nextDouble()) {
                    break;
                }
            }

            iterCount++;
        }
    }

    private static void updateDustOverlay(Entity entity, CompoundTag nbt, int duration) {
        if (!(entity instanceof Player) || duration % 10 != 0 || entity.level().isClientSide) return;

        double dustLevel = Math.round(nbt.getDouble("dust_amount") / 200.0 * 10.0);

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .ifPresent(cap -> {
                    cap.OVERLAY1 = "DUST";
                    cap.syncPlayerVariables(entity);
                });

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .ifPresent(cap -> {
                    cap.OVERLAY2 = "";
                    cap.syncPlayerVariables(entity);
                });

        StringBuilder bar = new StringBuilder("§l§4");
        for (int i = 0; i < 10; i++) {
            if ((double) i == dustLevel) bar.append("§r§7");
            bar.append("■");
        }
        String barStr = bar.toString();

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .ifPresent(cap -> {
                    cap.OVERLAY2 = barStr;
                    cap.syncPlayerVariables(entity);
                });
    }

    private static void spawnAshParticles(ServerLevel level, CompoundTag nbt, double range) {
        double x = nbt.getDouble("x_pos_doma");
        double y = nbt.getDouble("y_pos_doma");
        double z = nbt.getDouble("z_pos_doma");
        double dustAmount = Math.round(nbt.getDouble("dust_amount") / 200.0 * 10.0);

        var particle = (nbt.getDouble("skill") == 107.0)
                ? ParticleTypes.WHITE_ASH
                : ParticleTypes.ASH;

        double spread = range * 0.25;
        for (int i = 0; i < (int) (dustAmount + 1.0); i++) {
            level.sendParticles(particle, x, y, z, 50, spread, spread, spread, 0.1);
        }
    }

    private static void handleSlashParticles(LevelAccessor world, Entity entity,
                                             CompoundTag nbt, double range) {
        if (nbt.getBoolean("Failed") || !(world instanceof ServerLevel serverLevel)) return;

        ensureParticleCached();
        if (PARTICLE_SLASH_LARGE == null) return;

        double x = nbt.getDouble("x_pos_doma");
        double y = nbt.getDouble("y_pos_doma");
        double z = nbt.getDouble("z_pos_doma");
        double spread = range * 0.25;
        int count = (int) Math.round(4.0 * range);

        for (Player player : world.players()) {
            if (CanSeeSukunaSlashProcedure.execute(world, entity, player)
                    && player instanceof ServerPlayer sp) {

                serverLevel.sendParticles(
                        sp,
                        PARTICLE_SLASH_LARGE,
                        true,
                        x, y, z,
                        count,
                        spread, spread, spread,
                        0.01
                );
            }
        }
    }

    private static void ensureParticleCached() {
        if (particleCached) return;
        PARTICLE_SLASH_LARGE = (SimpleParticleType) BuiltInRegistries.PARTICLE_TYPE
                .get(new ResourceLocation("jujutsucraft", "particle_slash_large"));
        particleCached = true;
    }
}