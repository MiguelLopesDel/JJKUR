package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import net.mcreator.jujutsucraft.entity.BulletFlameProjectileEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JogoFlame1Procedure.class, priority = -10000)
public abstract class JogoFlame1ProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        double damage = 0.0;
        double CNT6 = 0.0;
        double picth = 0.0;
        double x_pos = 0.0;
        double y_pos = 0.0;
        double z_pos = 0.0;
        double yaw = 0.0;

        entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);
        double cnt1 = entity.getPersistentData().getDouble("cnt1");

        if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
            _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5, false, false));
        }

        Entity target = entity instanceof Mob _mob ? _mob.getTarget() : null;
        if (target instanceof LivingEntity && entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
            _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
        }

        // 1. TRACKING DURING CHARGE (0-9 ticks)
        if (cnt1 <= 9.0 && target instanceof LivingEntity) {
            entity.getPersistentData().putDouble("x_pos", target.getX());
            entity.getPersistentData().putDouble("y_pos", target.getY() + target.getBbHeight() * 0.8);
            entity.getPersistentData().putDouble("z_pos", target.getZ());
            
            RotateEntityProcedure.execute(
                    entity.getPersistentData().getDouble("x_pos"),
                    entity.getPersistentData().getDouble("y_pos"),
                    entity.getPersistentData().getDouble("z_pos"),
                    entity
            );
        }

        yaw = Math.toRadians(entity.getYRot() + 90.0F);
        picth = Math.toRadians(entity.getXRot());
        double offset = 1.5 + entity.getBbWidth();
        x_pos = entity.getX() + Math.cos(yaw) * Math.cos(picth) * offset;
        y_pos = entity.getY() + entity.getBbHeight() * 0.75 + Math.sin(picth) * -1.0 * offset;
        z_pos = entity.getZ() + Math.sin(yaw) * Math.cos(picth) * offset;

        CNT6 = 1.0 + entity.getPersistentData().getDouble("cnt6") * 0.1;

        // 2. ANIMATION TIMING (cnt1 < 25.0 * CNT6 - 10.0)
        if (entity instanceof LivingEntity _liv && cnt1 < 25.0 * CNT6 - 10.0) {
            ItemStack headItem = _liv.getItemBySlot(EquipmentSlot.HEAD);
            if (!headItem.isEmpty()) {
                headItem.getOrCreateTag().putDouble("P_ANIME1", -4.0);
                headItem.getOrCreateTag().putDouble("P_ANIME2", 0.0);
            }
            if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(-4.0);
            }
            PlayAnimationProcedure.execute(world, entity);
        }

        if (cnt1 < 10.0) {
            if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
            }

            // Restore Visual Particles
            if (world instanceof ServerLevel _level) {
                _level.sendParticles(ParticleTypes.FLAME, x_pos, y_pos, z_pos, (int) (2.0 * CNT6), 0.1, 0.1, 0.1, 0.01 * CNT6);
                _level.sendParticles(ParticleTypes.DRAGON_BREATH, x_pos, y_pos, z_pos, (int) (2.0 * CNT6), 0.1, 0.1, 0.1, 0.01 * CNT6);
            }

            if (target instanceof LivingEntity) {
                entity.getPersistentData().putBoolean("PRESS_Z", false);
                if (GetDistanceNearestEnemyProcedure.execute(world, entity) > 8.0
                        && (target.getPersistentData().getDouble("skill") == 0.0
                        || (target.getPersistentData().getDouble("skill") != 0.0 && target.getPersistentData().getBoolean("attack"))
                        || target.getPersistentData().getDouble("Damage") == 0.0)) {
                    entity.getPersistentData().putBoolean("PRESS_Z", true);
                }

                if (target.getPersistentData().getDouble("skill") == 107.0) {
                    entity.getPersistentData().putBoolean("PRESS_Z", true);
                }

                if (entity.getPersistentData().getDouble("cnt6") >= 5.0) {
                    entity.getPersistentData().putBoolean("PRESS_Z", false);
                }
            }

            if (entity.getPersistentData().getBoolean("PRESS_Z")) {
                entity.getPersistentData().putDouble("cnt1", Math.min(cnt1, 8.0));
            }

            if (cnt1 >= 8.0) {
                if (entity.getPersistentData().getDouble("cnt6") < 5.0) {
                    entity.getPersistentData().putDouble("cnt6", entity.getPersistentData().getDouble("cnt6") + 0.1);
                    if (entity instanceof Player _player) {
                        double cost = 2.0;
                        _player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                            cap.PlayerCursePowerChange -= cost;
                            cap.syncPlayerVariables(_player);
                        });
                    }
                } else {
                    entity.getPersistentData().putDouble("cnt6", 5.0);
                    if (entity.getPersistentData().getDouble("cnt7") == 0.0) {
                        entity.getPersistentData().putDouble("cnt7", 1.0);
                        if (world instanceof Level _level && !_level.isClientSide()) {
                            _level.explode(null, x_pos, y_pos, z_pos, 0.0F, Level.ExplosionInteraction.NONE);
                        }
                    }
                }

                if (entity.getPersistentData().getDouble("cnt6") >= 5.0 && world instanceof ServerLevel _level) {
                    _level.sendParticles(ParticleTypes.FLAME, x_pos, y_pos, z_pos, 2, 0.1, 0.1, 0.1, 0.1);
                }
            }
        } else if (cnt1 < 25.0 * CNT6) {
            // 3. TRACKING DURING FIRING
            if (target instanceof LivingEntity) {
                RotateEntityProcedure.execute(
                        entity.getPersistentData().getDouble("x_pos"),
                        entity.getPersistentData().getDouble("y_pos"),
                        entity.getPersistentData().getDouble("z_pos"),
                        entity
                );
            }

            if (world instanceof Level _level) {
                SoundEvent shootSound = (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.blaze.shoot"));
                if (!_level.isClientSide()) {
                    _level.playSound(null, BlockPos.containing(x, y, z), shootSound, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else {
                    _level.playLocalSound(x, y, z, shootSound, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
                }
            }

            if (world instanceof ServerLevel _level) {
                _level.sendParticles(ParticleTypes.FLAME, x_pos, y_pos, z_pos, (int) (10.0 * CNT6), 0.1, 0.1, 0.1, 0.25 * CNT6);
            }

            entity.getPersistentData().putDouble("Damage", 1.5 * CNT6);
            DamageFixProcedure.execute(entity);
            damage = entity.getPersistentData().getDouble("Damage");
            double yawOrig = entity.getYRot();
            double pitchOrig = entity.getXRot();

            for (int i = 0; i < 4; i++) {
                entity.setYRot((float) (yawOrig + (Math.random() - 0.5) * 6.0 * CNT6));
                entity.setXRot((float) (pitchOrig + (Math.random() - 0.5) * 6.0 * CNT6));
                
                // 4. SYNC ROTATION (Correct Methods)
                entity.yRotO = entity.getYRot();
                entity.xRotO = entity.getXRot();
                if (entity instanceof LivingEntity _liv) {
                    _liv.setYBodyRot(entity.getYRot());
                    _liv.setYHeadRot(entity.getYRot());
                    _liv.yBodyRotO = entity.getYRot();
                    _liv.yHeadRotO = entity.getYRot();
                }

                if (!world.isClientSide()) {
                    BulletFlameProjectileEntity _projectile = new BulletFlameProjectileEntity(JujutsucraftModEntities.BULLET_FLAME_PROJECTILE.get(), (Level) world);
                    _projectile.setOwner(entity);
                    _projectile.setBaseDamage(damage);
                    _projectile.setKnockback(0);
                    _projectile.setSilent(true);
                    _projectile.setPierceLevel((byte) 100);
                    _projectile.setSecondsOnFire(100);
                    _projectile.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                    _projectile.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, (float) ((2.5 + Math.random() * 0.2) * CNT6), 0.0F);
                    world.addFreshEntity(_projectile);
                }
            }

            entity.setYRot((float) yawOrig);
            entity.setXRot((float) pitchOrig);
            
            entity.yRotO = entity.getYRot();
            entity.xRotO = entity.getXRot();
            if (entity instanceof LivingEntity _liv) {
                _liv.setYBodyRot(entity.getYRot());
                _liv.setYHeadRot(entity.getYRot());
                _liv.yBodyRotO = entity.getYRot();
                _liv.yHeadRotO = entity.getYRot();
            }

            entity.getPersistentData().putDouble("Damage", 15.0 * CNT6);
            int destLevel = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_DESTRUCTION_LEVEL);
            entity.getPersistentData().putDouble("Range", 5.0 * CNT6 * destLevel);
            entity.getPersistentData().putDouble("knockback", 1.0 * CNT6);
            entity.getPersistentData().putDouble("effect", 10.0);
            RangeAttackProcedure.execute(world, x_pos, y_pos, z_pos, entity);
        } else if (cnt1 > 25.0 * CNT6 + 10.0) {
            entity.getPersistentData().putDouble("skill", 0.0);
        }
    }
}
