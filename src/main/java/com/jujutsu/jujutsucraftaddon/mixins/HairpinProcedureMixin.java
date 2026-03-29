package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.SummonRessonanceProcedure;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;
import net.mcreator.jujutsucraft.procedures.HairpinProcedure;
import net.mcreator.jujutsucraft.procedures.LogicAttackProcedure;
import net.mcreator.jujutsucraft.procedures.RangeAttackProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(value = HairpinProcedure.class, priority = -10000)
public abstract class HairpinProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        double cnt1 = entity.getPersistentData().getDouble("cnt1") + 1.0;
        entity.getPersistentData().putDouble("cnt1", cnt1);

        if (cnt1 == 1.0) {
            ItemStack mainHand = (entity instanceof LivingEntity _liv) ? _liv.getMainHandItem() : ItemStack.EMPTY;
            ItemStack offHand = (entity instanceof LivingEntity _liv) ? _liv.getOffhandItem() : ItemStack.EMPTY;
            SoundEvent blastSound = (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.blast"));

            if (mainHand.isEmpty()) {
                if (world instanceof Level _level) {
                    _level.playSound(null, BlockPos.containing(x, y, z), blastSound, SoundSource.NEUTRAL, 1.0F, 1.22F);
                }
                if (entity instanceof LivingEntity _liv) {
                    _liv.swing(InteractionHand.MAIN_HAND, true);
                }
            } else if (offHand.isEmpty()) {
                if (world instanceof Level _level) {
                    _level.playSound(null, BlockPos.containing(x, y, z), blastSound, SoundSource.NEUTRAL, 1.0F, 1.22F);
                }
                if (entity instanceof LivingEntity _liv) {
                    _liv.swing(InteractionHand.OFF_HAND, true);
                }
            }
        }

        if (cnt1 > 5.0) {
            Vec3 _center = new Vec3(x, y, z);
            boolean success = false;
            double x_pos = 0, y_pos = 0, z_pos = 0;
            double x_pwr = 0, y_pwr = 0, z_pwr = 0;
            double cnt6 = 0;

            for (Entity target : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(32.0), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(_center))).toList()) {
                if (entity != target && target.getPersistentData().getDouble("Nail") > 0.0) {
                    if (target instanceof Projectile _proj && _proj.getDeltaMovement().length() > 0.0) {
                        success = true;
                        x_pos = target.getX();
                        y_pos = target.getY() + target.getBbHeight();
                        z_pos = target.getZ();
                        x_pwr = target.getLookAngle().x;
                        y_pwr = target.getLookAngle().y;
                        z_pwr = target.getLookAngle().z;
                        cnt6 = 1.0;
                        if (!target.level().isClientSide()) target.discard();
                        break;
                    }

                    if (LogicAttackProcedure.execute(world, entity, target)) {
                        success = true;
                        x_pos = target.getX();
                        y_pos = target.getY() + target.getBbHeight();
                        z_pos = target.getZ();
                        x_pwr = (Math.random() - 0.5) * 2.0;
                        y_pwr = (Math.random() - 0.5) * 2.0;
                        z_pwr = (Math.random() - 0.5) * 2.0;
                        cnt6 = Math.sqrt(target.getPersistentData().getDouble("Nail"));
                        target.getPersistentData().putDouble("Nail", 0.0);
                        break;
                    }
                }
            }

            if (success) {
                double range = cnt6 * 2.0; // Buffed range factor from addon
                SoundEvent largeBlast = (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.large_blast"));

                if (world instanceof Level _level) {
                    _level.playSound(null, BlockPos.containing(x, y, z), largeBlast, SoundSource.NEUTRAL, (float) range, 1.0F);
                    if (!_level.isClientSide()) {
                        _level.explode(null, x_pos, y_pos, z_pos, 0.0F, Level.ExplosionInteraction.NONE);
                    }
                }

                if (world instanceof ServerLevel _level) {
                    _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_BLUE.get(), x_pos, y_pos, z_pos, (int) (10.0 * cnt6), 0.25 * range, 0.25 * range, 0.25 * range, range);
                    _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_BLACK_FLASH_1.get(), x_pos, y_pos, z_pos, (int) (5.0 * cnt6), 0.25 * range, 0.25 * range, 0.25 * range, range);
                }

                x_pos -= x_pwr * 0.25 * 16.0 * range;
                y_pos -= y_pwr * 0.25 * 16.0 * range;
                z_pos -= z_pwr * 0.25 * 16.0 * range;

                double num1 = 0;
                for (int i = 0; i < (int) (32.0 * range); i++) {
                    if (++num1 > 0.0) {
                        entity.getPersistentData().putDouble("Damage", 13.0 * cnt6 * 1.2); // Addon Buff: 1.2x
                        entity.getPersistentData().putDouble("Range", 3.0 * cnt6 * 1.2);  // Addon Buff: 1.2x
                        entity.getPersistentData().putDouble("effect", 1.0);
                        entity.getPersistentData().putDouble("effectConfirm", 2.0);
                        entity.getPersistentData().putBoolean("ignore", true);
                        RangeAttackProcedure.execute(world, x_pos, y_pos, z_pos, entity);

                        JujutsucraftaddonModVariables.PlayerVariables vars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());
                        if ("Ressonance Mode".equals(vars.Mode)) {
                            SummonRessonanceProcedure.execute(world, x_pos, y_pos, z_pos, entity);
                        }

                        entity.getPersistentData().putDouble("BlockRange", 1.0 * cnt6 * 10.0); // Addon Buff: 10x
                        entity.getPersistentData().putDouble("BlockDamage", 2.0 * cnt6 * 10.0); // Addon Buff: 10x
                        BlockDestroyAllDirectionProcedure.execute(world, x_pos, y_pos, z_pos, entity);
                        num1 = -4.0;
                    }

                    if (world instanceof ServerLevel _level) {
                        _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_BLUE.get(), x_pos, y_pos, z_pos, (int) (1.0 + cnt6), 0.1 * range, 0.1 * range, 0.1 * range, 0.0);
                    }

                    x_pos += x_pwr * 0.25;
                    y_pos += y_pwr * 0.25;
                    z_pos += z_pwr * 0.25;
                }
            } else {
                entity.getPersistentData().putDouble("skill", 0.0);
                if (cnt1 == 1.0) {
                    JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
                    double refund = baseVars.PlayerCursePowerChange + baseVars.PlayerSelectCurseTechniqueCost;
                    entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(c -> {
                        c.PlayerCursePowerChange = refund;
                        c.syncPlayerVariables(entity);
                    });
                }
            }
        }

        if (cnt1 > 15.0) {
            entity.getPersistentData().putDouble("skill", 0.0);
        }
    }
}
