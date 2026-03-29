package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import net.mcreator.jujutsucraft.entity.MeteorEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;
import net.mcreator.jujutsucraft.procedures.DamageFixProcedure;
import net.mcreator.jujutsucraft.procedures.MaximumMeteorProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.SetRangedAmmoProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = MaximumMeteorProcedure.class, priority = -10000)
public abstract class MaximumMeteorMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        double cnt1 = entity.getPersistentData().getDouble("cnt1") + 1.0;
        entity.getPersistentData().putDouble("cnt1", cnt1);
        entity.fallDistance = 0.0F;

        // 1. DAMAGE FIX AND NBT INITIALIZATION
        entity.getPersistentData().putDouble("Damage", 6.0);
        DamageFixProcedure.execute(entity);

        if (entity.getPersistentData().getDouble("cnt3") == 0.0) {
            entity.getPersistentData().putDouble("cnt3", 1.0);
        }

        if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
            _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
            _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
        }

        // DYNAMIC PHASE TRANSITION (Restore Original Physical Logic)
        // If peak reached or 30 ticks passed, jump to preparation phase
        if (cnt1 < 100.0 && cnt1 > 10.0 && (entity.getDeltaMovement().y() < 0.1)) {
            entity.getPersistentData().putDouble("cnt1", 100.0);
            cnt1 = 100.0;
        }

        // CHARGING PHASE (Ticks 1-100)
        if (cnt1 < 100.0) {
            if (cnt1 == 1.0) {
                entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), 3.0, entity.getDeltaMovement().z()));
                if (world instanceof ServerLevel _level) {
                    _level.sendParticles(ParticleTypes.CLOUD, x, y, z, 20, 0.0, 0.0, 0.0, 1.0);
                }

                if (world instanceof Level _level) {
                    _level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:flame_explode")), SoundSource.NEUTRAL, 5.0F, 1.0F);
                }

                if (entity instanceof LivingEntity _liv) {
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(215.0);
                    }
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(0.0);
                    }
                    PlayAnimationProcedure.execute(world, entity);
                    _liv.swing(InteractionHand.MAIN_HAND, true);
                }
            }

            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), Math.max(0.5 - cnt1 * 0.005, 0.0), entity.getDeltaMovement().z()));

            // Continuous Destruction
            if (entity instanceof LivingEntity _liv) {
                int destLevel = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_DESTRUCTION_LEVEL);
                _liv.getPersistentData().putDouble("BlockRange", 12.0 * destLevel);
                _liv.getPersistentData().putDouble("BlockDamage", 8.0 * destLevel);
                _liv.getPersistentData().putBoolean("noParticle", true);
                BlockDestroyAllDirectionProcedure.execute(world, x, y + entity.getDeltaMovement().y(), z, _liv);
                BlockDestroyAllDirectionProcedure.execute(world, x, y, z, _liv);
            }

            // Trigonometric Particle Spiral
            double old_x = entity.getPersistentData().getDouble("old_x");
            double old_y = entity.getPersistentData().getDouble("old_y");
            double old_z = entity.getPersistentData().getDouble("old_z");
            double x_pwr = old_x - x;
            double y_pwr = old_y - y;
            double z_pwr = old_z - z;
            double num1 = Math.toRadians(entity.getPersistentData().getDouble("cnt3"));
            double num2 = Math.min(8.0, cnt1);
            double dis = Math.min(1.5, cnt1 / 15.0);

            if (world instanceof ServerLevel _level) {
                double px = old_x;
                double py = old_y;
                double pz = old_z;
                for (int i = 0; i < 12; i++) {
                    px += x_pwr / 12.0;
                    py += y_pwr / 12.0;
                    pz += z_pwr / 12.0;
                    for (int j = 0; j < 2; j++) {
                        double angle = (j == 0) ? num1 : num1 + Math.PI;
                        double xp2 = px + Math.sin(angle) * num2;
                        double zp2 = pz + Math.cos(angle) * num2;
                        _level.sendParticles(ParticleTypes.FLAME, xp2, py, zp2, 3, dis, dis, dis, dis * 0.1);
                        _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_MAGMA.get(), xp2, py, zp2, 3, dis, dis, dis, dis * 0.1);
                    }
                    _level.sendParticles(ParticleTypes.FLAME, px, py, pz, 2, dis, dis, dis, dis * 0.1);
                    _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_MAGMA.get(), px, py, pz, 2, dis, dis, dis, dis * 0.1);
                    num1 += Math.toRadians(Math.random() * 10.0);
                }
            }
            entity.getPersistentData().putDouble("cnt3", Math.toDegrees(num1));
            entity.getPersistentData().putDouble("old_x", x);
            entity.getPersistentData().putDouble("old_y", y);
            entity.getPersistentData().putDouble("old_z", z);

        } else {
            // PREPARATION AND FIRING PHASE (Ticks 100-160)
            if (cnt1 == 100.0) {
                if (entity instanceof LivingEntity _liv) {
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(4.0);
                    }
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(0.0);
                    }
                    PlayAnimationProcedure.execute(world, entity);
                    _liv.swing(InteractionHand.MAIN_HAND, true);
                }
            }

            // Continuous Visual for Preparation Phase
            if (world instanceof ServerLevel _level) {
                double dis_prep = Math.min((cnt1 - 100.0) * 0.1, 1.5);
                _level.sendParticles(ParticleTypes.FLAME, x, y + 5.0, z, 10, dis_prep, dis_prep, dis_prep, 0.1);
                
                // Peak Concentration
                if (cnt1 == 119.0) {
                    _level.sendParticles(ParticleTypes.FLAME, x, y + 5.0, z, 25, 1.0, 1.0, 1.0, 0.5);
                }
            }

            if (cnt1 == 120.0) {
                if (entity instanceof LivingEntity _liv) {
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(-49.0);
                    }
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(0.0);
                    }
                    PlayAnimationProcedure.execute(world, entity);

                    int amp = _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                    double HP = 400.0 + amp * 40.0;
                    double mx = entity.getX();
                    double my = entity.getY() + entity.getBbHeight() + 5.0;
                    double mz = entity.getZ();

                    if (world instanceof ServerLevel _level) {
                        Entity meteor = ((EntityType) JujutsucraftModEntities.METEOR.get()).create(_level, null, null, BlockPos.containing(mx, my, mz), MobSpawnType.MOB_SUMMONED, false, false);
                        if (meteor != null) {
                            meteor.setInvulnerable(true);
                            if (meteor instanceof Mob _mob) _mob.setNoAi(true); 
                            
                            meteor.setYRot(world.getRandom().nextFloat() * 360.0F); 
                            SetRangedAmmoProcedure.execute(entity, meteor);
                            meteor.setYRot(entity.getYRot());
                            meteor.setXRot(entity.getXRot());
                            meteor.yRotO = entity.getYRot();
                            meteor.xRotO = entity.getXRot();
                            
                            if (meteor instanceof LivingEntity _miv) {
                                _miv.yBodyRot = _miv.yHeadRot = _miv.yBodyRotO = _miv.yHeadRotO = entity.getYRot();
                                if (_miv.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                                    _miv.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HP);
                                }
                                _miv.setHealth((float) HP);
                                if (_miv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.SIZE.get())) {
                                    _miv.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).setBaseValue(0.1);
                                }
                            }
                            _level.addFreshEntity(meteor);
                            entity.startRiding(meteor);
                        }
                    }

                    if (world instanceof Level _level) {
                        _level.playSound(null, BlockPos.containing(mx, my, mz), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:flame_explode")), SoundSource.NEUTRAL, 15.0F, 0.5F);
                    }
                    
                    // Reset skill to stop suction after launch
                    entity.getPersistentData().putDouble("skill", 0.0);
                }
            }

            if (cnt1 <= 120.0) {
                entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), 0.0, entity.getDeltaMovement().z()));
            }

            if (cnt1 >= 160.0) {
                entity.getPersistentData().putDouble("skill", 0.0);
            }
        }

        // 2. CONTINUOUS SUCTION WITH ELEVATION (Gravity)
        if (entity.getPersistentData().getDouble("skill") > 0.0) {
            Vec3 _center = new Vec3(x, y, z);
            List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(22.5), e -> true);
            for (Entity entityiterator : _entfound) {
                if (entityiterator instanceof FallingBlockEntity || entityiterator.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, new ResourceLocation("forge:no_living")))) {
                    double x_k = entityiterator.getX() - x;
                    double y_k = entityiterator.getY() - y;
                    double z_k = entityiterator.getZ() - z;
                    double dist_k = Math.sqrt(x_k * x_k + y_k * y_k + z_k * z_k);
                    if (dist_k != 0.0) {
                        x_k = x_k / dist_k * -2.4;
                        y_k = y_k / dist_k * -2.4;
                        z_k = z_k / dist_k * -2.4;
                        Vec3 delta = entityiterator.getDeltaMovement();
                        double nextX = (Math.abs(x_k - delta.x) < 0.1 * Math.abs(x_k)) ? delta.x : delta.x + x_k * 0.05;
                        double nextY = (Math.abs(y_k - delta.y) < 0.1 * Math.abs(y_k)) ? delta.y : delta.y + y_k * 0.05;
                        double nextZ = (Math.abs(z_k - delta.z) < 0.1 * Math.abs(z_k)) ? delta.z : delta.z + z_k * 0.05;
                        
                        if (entityiterator.onGround()) {
                            nextY = Math.max(nextY, 0.6); // Stronger elevation restored for better feeling
                        }
                        
                        entityiterator.setDeltaMovement(new Vec3(nextX, nextY, nextZ));
                    }
                }
            }
        }
    }
}
