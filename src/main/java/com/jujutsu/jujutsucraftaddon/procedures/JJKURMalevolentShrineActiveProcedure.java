package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;
import net.mcreator.jujutsucraft.procedures.CanSeeSukunaSlashProcedure;
import net.mcreator.jujutsucraft.procedures.RangeAttackProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Locale;

public class JJKURMalevolentShrineActiveProcedure {

    public static void onExecute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (ci != null) ci.cancel();
        if (entity == null) return;

        LivingEntity living = (entity instanceof LivingEntity _ent) ? _ent : null;
        MobEffectInstance domainEffect = (living != null) ? living.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) : null;
        if (domainEffect == null) return;

        int amplifier = domainEffect.getAmplifier();
        int duration = domainEffect.getDuration();
        double range = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius * (amplifier > 0 ? 18 : 2);

        handleEntityAttacks(world, entity, range, duration);

        if (amplifier > 0) {
            if (entity.level().getGameTime() % 2 == 0) {
                handleOptimizedBlockDestruction(world, entity, range);
            }

            updateDustOverlay(entity, duration);
            spawnAshParticles(world, entity, range);
            handleSlashParticles(world, entity, range);
        }
    }

    private static void handleEntityAttacks(LevelAccessor world, Entity entity, double range, int duration) {
        CompoundTag nbt = entity.getPersistentData();
        double oldSkill = nbt.getDouble("skill");
        double oldCooldown = nbt.getDouble("COOLDOWN_TICKS");
        double xCenter = nbt.getDouble("x_pos_doma");
        double yCenter = nbt.getDouble("y_pos_doma");
        double zCenter = nbt.getDouble("z_pos_doma");

        nbt.putDouble("y_knockback", -0.1);
        nbt.putDouble("Range", range * 0.5);
        nbt.putDouble("effect", 1.0);
        nbt.putDouble("effectConfirm", 1.0);
        nbt.putDouble("knockback", 0.001);
        nbt.putBoolean("swing", false);
        nbt.putBoolean("attack", false);
        nbt.putBoolean("DomainAttack", true);
        nbt.putBoolean("ExtinctionBlock", true);

        for (int i = 0; i < 4; i++) {
            double n1 = (i % 2 == 0) ? 1.0 : -1.0;
            double n2 = (i < 2) ? 1.0 : -1.0;
            double px = xCenter + range * 0.25 * n1;
            double py = yCenter + range * 0.25 - 15.0;
            double pz = zCenter + range * 0.25 * n2;

            nbt.putDouble("skill", (duration % 2 == 1) ? 105.0 : 106.0);
            nbt.putDouble("COOLDOWN_TICKS", (duration % 2 == 1) ? 50.0 : 100.0);
            nbt.putDouble("Damage", 10.5 + (duration % 10) * 0.1);

            RangeAttackProcedure.execute(world, px, py, pz, entity);
        }

        nbt.putBoolean("ExtinctionBlock", false);
        nbt.putDouble("y_knockback", 0.0);
        nbt.putDouble("skill", oldSkill);
        nbt.putDouble("COOLDOWN_TICKS", oldCooldown);
    }

    private static void handleOptimizedBlockDestruction(LevelAccessor world, Entity entity, double range) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        CompoundTag nbt = entity.getPersistentData();
        double xCenter = nbt.getDouble("x_pos_doma");
        double yCenter = nbt.getDouble("y_pos_doma");
        double zCenter = nbt.getDouble("z_pos_doma");

        if (nbt.getDouble("dust_amount") <= 0.0) nbt.putDouble("dust_amount", 1.0);

        int destroyedCount = 0;
        for (int i = 0; i < 128; i++) {
            double angle = Math.toRadians(Math.random() * 360.0);
            double dist = range * 0.5 * (Math.random() * 2.0 - 1.0);
            double px = xCenter + Math.sin(angle) * dist;
            double py = yCenter + Math.random() * range * 0.2;
            double pz = zCenter + Math.cos(angle) * dist;

            BlockPos targetPos = BlockPos.containing(px, py, pz);
            if (!world.isEmptyBlock(targetPos)) {
                nbt.putDouble("dust_amount", Math.min(nbt.getDouble("dust_amount") + 1.0, 200.0));

                serverLevel.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 2, 1.5, 1.5, 1.5, 0.0);
                serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 2, 1.5, 1.5, 1.5, 0.5);

                nbt.putBoolean("noParticle", true);
                nbt.putBoolean("ExtinctionBlock", true);
                nbt.putDouble("BlockRange", 16.0);
                nbt.putDouble("BlockDamage", 99.0);

                BlockDestroyAllDirectionProcedure.execute(world, px, Math.max(py, yCenter + 8.0), pz, entity);
                if (++destroyedCount > 16) break;
            }
        }
    }

    private static void updateDustOverlay(Entity entity, int duration) {
        if (!(entity instanceof Player player) || duration % 10 != 0) return;

        double dustLevel = Math.round(entity.getPersistentData().getDouble("dust_amount") / 200.0 * 10.0);
        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
            cap.OVERLAY1 = "DUST";
            StringBuilder bar = new StringBuilder("§l§4");
            for (int i = 0; i < 10; i++) {
                if ((double) i == dustLevel) bar.append("§r§7");
                bar.append("■");
            }
            cap.OVERLAY2 = bar.toString();
            cap.syncPlayerVariables(entity);
        });
    }

    private static void spawnAshParticles(LevelAccessor world, Entity entity, double range) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        CompoundTag nbt = entity.getPersistentData();
        double x = nbt.getDouble("x_pos_doma");
        double y = nbt.getDouble("y_pos_doma");
        double z = nbt.getDouble("z_pos_doma");
        double dustAmount = Math.round(nbt.getDouble("dust_amount") / 200.0 * 10.0);

        var particle = (nbt.getDouble("skill") == 107.0) ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH;
        for (int i = 0; i < (int) (dustAmount + 1.0); i++) {
            serverLevel.sendParticles(particle, x, y, z, 50, range * 0.25, range * 0.25, range * 0.25, 0.1);
        }
    }

    private static void handleSlashParticles(LevelAccessor world, Entity entity, double range) {
        CompoundTag nbt = entity.getPersistentData();
        if (nbt.getBoolean("Failed") || world.isClientSide()) return;

        double x = nbt.getDouble("x_pos_doma");
        double y = nbt.getDouble("y_pos_doma");
        double z = nbt.getDouble("z_pos_doma");

        for (Player player : world.players()) {
            if (CanSeeSukunaSlashProcedure.execute(world, entity, player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    String cmd = String.format(Locale.US, "particle jujutsucraft:particle_slash_large %f %f %f %f %f %f 0.01 %d normal",
                            x, y, z, range * 0.25, range * 0.25, range * 0.25, Math.round(4.0 * range));

                    serverPlayer.server.getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, serverPlayer.position(), serverPlayer.getRotationVector(),
                                    (ServerLevel) serverPlayer.level(), 4, serverPlayer.getName().getString(), serverPlayer.getDisplayName(),
                                    serverPlayer.server, serverPlayer), cmd);
                }
            }
        }
    }
}
