package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.entity.FlameArrowEntity;
import net.mcreator.jujutsucraft.entity.JogoEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.Comparator;
import java.util.List;

@Mixin(value = OpenProcedure.class, priority = -10000)
public abstract class OpenProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        double x_pos = 0.0;
        double y_pos = 0.0;
        double z_pos = 0.0;
        double HP = 0.0;
        double yaw = 0.0;
        double pitch = 0.0;
        double CNT6 = 0.0;
        double distance = 0.0;
        double rad1 = 0.0;
        double width = 0.0;
        double x_power = 0.0;
        double y_power = 0.0;
        double z_power = 0.0;
        double speed = 0.0;

        entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);

        if (entity instanceof LivingEntity _liv) {
            if (!_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
                _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
            }

            LivingEntity target = (_liv instanceof Mob _mob) ? _mob.getTarget() : null;
            if (target != null) {
                RotateEntityProcedure.execute(target.getX(), target.getY() + (double) target.getBbHeight() * 0.5, target.getZ(), _liv);
            }

            _liv.getPersistentData().putDouble("x_power", _liv.getLookAngle().x * 3.0);
            _liv.getPersistentData().putDouble("y_power", _liv.getLookAngle().y * 3.0);
            _liv.getPersistentData().putDouble("z_power", _liv.getLookAngle().z * 3.0);

            yaw = Math.toRadians(_liv.getYRot() + 90.0F);
            pitch = Math.toRadians(_liv.getXRot());
            double offset = 1.0 + _liv.getBbWidth();
            x_pos = _liv.getX() + Math.cos(yaw) * Math.cos(pitch) * offset;
            y_pos = _liv.getY() + (double) _liv.getBbHeight() * 0.6 + Math.sin(pitch) * -1.0 * offset;
            z_pos = _liv.getZ() + Math.sin(yaw) * Math.cos(pitch) * offset;

            _liv.getPersistentData().putDouble("x_pos", x_pos);
            _liv.getPersistentData().putDouble("y_pos", y_pos);
            _liv.getPersistentData().putDouble("z_pos", z_pos);

            double cnt1 = _liv.getPersistentData().getDouble("cnt1");

            // TICK 1-31 LOGIC
            if (cnt1 <= 31.0) {
                if (cnt1 <= 15.0) {
                    // TICK 1
                    if (cnt1 == 1.0) {
                        if (world instanceof ServerLevel _serverLevel) {
                            _serverLevel.sendParticles(ParticleTypes.FLAME, x_pos, y_pos, z_pos, (int) (10.0 * _liv.getBbWidth()), 0.1 * _liv.getBbWidth(), 0.1 * _liv.getBbWidth(), 0.1 * _liv.getBbWidth(), 0.05);
                        }

                        if (world instanceof Level _level && !_level.isClientSide()) {
                            _level.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.fire.ambient")), SoundSource.NEUTRAL, 1.0F, 1.0F);
                        }

                        // Addon: Fuga Effect Logic
                        if (_liv instanceof GeoEntity) {
                            if (!_liv.hasEffect(JujutsucraftaddonModMobEffects.FUGA.get())) {
                                int duration = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) ? 240 : 100;
                                if (!_liv.level().isClientSide()) {
                                    _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.FUGA.get(), duration, 1, false, false));
                                }
                            }
                        }

                        PlayAnimationProcedure.execute(world, _liv);
                    }

                    // TICK 15
                    if (cnt1 == 15.0) {
                        _liv.swing(InteractionHand.MAIN_HAND, true);

                        int amp = _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                        // Addon: Higher HP (x10)
                        HP = (200.0 + amp * 20.0) * 10.0;

                        if (world instanceof ServerLevel _serverLevel) {
                            // Addon: Manual Summon to ensure health and attributes
                            String cmd = "summon jujutsucraft:flame_arrow ~ ~ ~ {Health:" + Math.round(HP) + "f,Attributes:[{Name:\"generic.max_health\",Base:" + Math.round(HP) + "}],Rotation:[" + _liv.getYRot() + "F," + _liv.getXRot() + "F]}";
                            _serverLevel.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _serverLevel, 4, "", Component.literal(""), _serverLevel.getServer(), null), cmd);

                            Vec3 center = new Vec3(x_pos, y_pos, z_pos);
                            List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(0.5), e -> e instanceof FlameArrowEntity);
                            for (Entity entityiterator : entities) {
                                if (entityiterator.getPersistentData().getDouble("NameRanged_ranged") == 0.0) {
                                    SetRangedAmmoProcedure.execute(_liv, entityiterator);
                                    if (!entityiterator.level().isClientSide() && entityiterator.getServer() != null) {
                                        entityiterator.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entityiterator.position(), entityiterator.getRotationVector(), (ServerLevel) entityiterator.level(), 4, entityiterator.getName().getString(), entityiterator.getDisplayName(), entityiterator.level().getServer(), entityiterator), "data merge entity @s {NoAI:1b}");
                                    }
                                    break;
                                }
                            }
                        }

                        if (world instanceof Level _level && !_level.isClientSide()) {
                            _level.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.blaze.shoot")), SoundSource.NEUTRAL, 1.0F, 1.0F);
                        }
                    }
                } else {
                    // CHARGING LOGIC (Tick 16-31)
                    if (target != null) {
                        // Addon: FUGA effect forces PRESS_Z
                        if (!_liv.hasEffect(JujutsucraftaddonModMobEffects.FUGA.get())) {
                            _liv.getPersistentData().putBoolean("PRESS_Z", false);
                        }

                        if (GetDistanceNearestEnemyProcedure.execute(world, _liv) > 8.0) {
                            if (target.getPersistentData().getDouble("skill") == 0.0 || (target.getPersistentData().getDouble("skill") != 0.0 && target.getPersistentData().getBoolean("attack")) || target.getPersistentData().getDouble("Damage") != 0.0) {
                                _liv.getPersistentData().putBoolean("PRESS_Z", true);
                            }
                        }

                        if (target instanceof JogoEntity && _liv.getPersistentData().getDouble("cnt6") < 5.0) {
                            _liv.getPersistentData().putBoolean("PRESS_Z", true);
                            if (target.getPersistentData().getDouble("skill") == 405.0) {
                                if (!(target.getPersistentData().getDouble("cnt6") >= 5.0) && target.getPersistentData().getBoolean("PRESS_Z")) {
                                    // Keep PRESS_Z true
                                } else {
                                    if (!_liv.hasEffect(JujutsucraftaddonModMobEffects.FUGA.get())) {
                                        _liv.getPersistentData().putBoolean("PRESS_Z", false);
                                    }
                                }
                            }
                        }

                        if (target instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity && _liv.getPersistentData().getDouble("cnt6") < 5.0) {
                            _liv.getPersistentData().putBoolean("PRESS_Z", true);
                        }

                        if (_liv.getPersistentData().getDouble("cnt6") >= 5.0 && !_liv.hasEffect(JujutsucraftaddonModMobEffects.FUGA.get())) {
                            // Addon: Higher charge limit at low health
                            if (!(_liv.getHealth() <= _liv.getMaxHealth() / 3.0)) {
                                _liv.getPersistentData().putBoolean("PRESS_Z", false);
                            }
                        }
                    }

                    if (_liv.getPersistentData().getBoolean("PRESS_Z")) {
                        _liv.getPersistentData().putDouble("cnt1", Math.min(_liv.getPersistentData().getDouble("cnt1"), 30.0));
                        if (_liv.getPersistentData().getDouble("cnt1") >= 30.0) {
                            _liv.getPersistentData().putDouble("cnt6", _liv.getPersistentData().getDouble("cnt6") + 0.1);

                            // Addon: Low health enhanced charge (up to 30.0)
                            if (_liv.getHealth() <= _liv.getMaxHealth() / 3.0) {
                                if (_liv.getPersistentData().getDouble("cnt6") >= 30.0) {
                                    _liv.getPersistentData().putDouble("cnt6", 30.0);
                                }
                                // Scale summoned arrows size
                                Vec3 center = new Vec3(_liv.getX(), _liv.getY(), _liv.getZ());
                                List<Entity> subEntities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(10.0), e -> (e.getPersistentData().getString("OWNER_UUID")).equals(_liv.getStringUUID()));
                                for (Entity entityiterator : subEntities) {
                                    if (entityiterator instanceof LivingEntity _livIter) {
                                        double currentSize = _livIter.getAttribute(JujutsucraftModAttributes.SIZE.get()).getBaseValue();
                                        _livIter.getAttribute(JujutsucraftModAttributes.SIZE.get()).setBaseValue(currentSize + 0.1);
                                    }
                                }
                            } else {
                                if (_liv.getPersistentData().getDouble("cnt6") >= 5.0) {
                                    _liv.getPersistentData().putDouble("cnt6", 5.0);
                                }
                            }
                        }
                    }
                }
            }

            // FINAL ATTACK (Tick 31)
            if (cnt1 >= 31.0) {
                if (cnt1 == 31.0) {
                    CNT6 = 1.0 + _liv.getPersistentData().getDouble("cnt6") * 0.1;
                    _liv.getPersistentData().putDouble("Damage", 17.5 * CNT6);
                    _liv.getPersistentData().putDouble("Range", 12.0 * CNT6);
                    _liv.getPersistentData().putDouble("projectile_type", 1.0);
                    _liv.getPersistentData().putDouble("knockback", 0.5);
                    _liv.getPersistentData().putDouble("effect", 3.0);
                    RangeAttackProcedure.execute(world, x_pos, y_pos, z_pos, _liv);
                }

                if (cnt1 > 45.0) {
                    _liv.getPersistentData().putDouble("skill", 0.0);
                }
            }

            // VISUAL EFFECTS & DESTRUCTION (Window: Tick 15-31)
            if (cnt1 >= 15.0 && cnt1 <= 31.0) {
                CNT6 = 1.0 + _liv.getPersistentData().getDouble("cnt6") * 0.2;
                distance = Math.min(cnt1 * 0.5, 4.0) * CNT6;

                if (Math.random() < 1.0) {
                    for (int i = 0; i < 4; i++) {
                        _liv.getPersistentData().putDouble("BlockRange", distance);
                        _liv.getPersistentData().putDouble("BlockDamage", 5.0 * CNT6);
                        _liv.getPersistentData().putBoolean("noParticle", true);
                        BlockDestroyAllDirectionProcedure.execute(world, x_pos, y_pos + distance - 0.5, z_pos, _liv);
                        rad1 = Math.toRadians(720.0 * Math.random());
                        width = distance * (Math.random() * 0.5 + 0.5);
                        x_pos = _liv.getX() + Math.cos(rad1) * width;
                        z_pos = _liv.getZ() + Math.sin(rad1) * width;
                        _liv.getPersistentData().putBoolean("noEffect", true);
                    }
                    _liv.getPersistentData().putBoolean("noEffect", false);
                }

                y_pos = _liv.getY();
                for (int i = 0; i < 36; i++) {
                    rad1 += Math.toRadians(20.0 * Math.random());
                    x_pos = _liv.getX() + Math.cos(rad1) * distance;
                    z_pos = _liv.getZ() + Math.sin(rad1) * distance;
                    if (Math.random() < 0.1 * CNT6) {
                        if (world instanceof ServerLevel _level) {
                            _level.sendParticles(ParticleTypes.FLAME, x_pos, y_pos, z_pos, 1, distance * 0.05, distance * 0.05, distance * 0.05, speed);
                            if (Math.random() < 0.1 && _liv.getPersistentData().getDouble("cnt6") > 2.5) {
                                _level.sendParticles(JujutsucraftModParticleTypes.PARTICLE_MAGMA.get(), x_pos, y_pos, z_pos, 1, distance * 0.5, distance * 0.05, distance * 0.5, 0.05);
                            }
                        }
                    }
                    if (Math.random() < 0.005 * CNT6 && world instanceof ServerLevel _level) {
                        _level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x_pos, y_pos, z_pos, 1, distance * 0.05, distance * 0.05, distance * 0.05, speed);
                    }
                    if (Math.random() < 0.05 * CNT6 && world instanceof ServerLevel _level) {
                        _level.sendParticles(ParticleTypes.CLOUD, x_pos, y_pos, z_pos, 1, distance * 0.05, distance * 0.05, distance * 0.05, speed);
                    }
                }

                // Ray particles
                x_pos = _liv.getX() + Math.cos(yaw) * Math.cos(pitch) * offset;
                y_pos = _liv.getY() + (double) _liv.getBbHeight() * 0.6 + Math.sin(pitch) * -1.0 * offset;
                z_pos = _liv.getZ() + Math.sin(yaw) * Math.cos(pitch) * offset;
                x_power = _liv.getX() + Math.cos(yaw) * Math.cos(pitch) * (0.9 + _liv.getBbWidth()) - x_pos;
                y_power = _liv.getY() + (double) _liv.getBbHeight() * 0.6 + Math.sin(pitch) * -1.0 * (0.9 + _liv.getBbWidth()) - y_pos;
                z_power = _liv.getZ() + Math.sin(yaw) * Math.cos(pitch) * (0.9 + _liv.getBbWidth()) - z_pos;
                distance = 0.0;

                for (int i = 0; i < Math.round((1.0 + _liv.getBbWidth()) * 30.0); i++) {
                    if (Math.random() < 0.05 && world instanceof ServerLevel _level) {
                        _level.sendParticles(ParticleTypes.FLAME, x_pos, y_pos, z_pos, 1, distance, distance, distance, distance);
                    }
                    if (Math.random() < 0.0025 && world instanceof ServerLevel _level) {
                        _level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x_pos, y_pos, z_pos, 1, distance, distance, distance, distance);
                    }
                    x_pos += x_power;
                    y_pos += y_power;
                    z_pos += z_power;
                    distance += 0.001;
                }
            }

            PlayAnimationProcedure.execute(world, _liv);
        }
    }
}
