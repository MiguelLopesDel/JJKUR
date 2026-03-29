package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.PlayGojoRed2Procedure;
import net.mcreator.jujutsucraft.entity.RedEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TechniqueRedProcedure.class, priority = -10000)
public abstract class RedTechniqueMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43 with improved entity sync and Tactical Mode support.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        double x_pos = 0.0;
        double y_pos = 0.0;
        double z_pos = 0.0;
        double yaw = 0.0;
        double pitch = 0.0;
        boolean logic_a = false;

        entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);

        if (entity instanceof LivingEntity _liv) {
            // 1. Target Tracking and Rotation
            LivingEntity target = (_liv instanceof Mob _mob) ? _mob.getTarget() : null;
            if (target != null) {
                x_pos = target.getX();
                y_pos = target.getY() + target.getBbHeight() * 0.5;
                z_pos = target.getZ();
                RotateEntityProcedure.execute(x_pos, y_pos, z_pos, _liv);
            }

            // 2. Power and Offset Calculations
            _liv.getPersistentData().putDouble("x_power", _liv.getLookAngle().x * 3.0);
            _liv.getPersistentData().putDouble("y_power", _liv.getLookAngle().y * 3.0);
            _liv.getPersistentData().putDouble("z_power", _liv.getLookAngle().z * 3.0);

            yaw = Math.toRadians(_liv.getYRot() + 90.0F);
            pitch = Math.toRadians(_liv.getXRot());
            double offset = 1.0 + _liv.getBbWidth();
            x_pos = _liv.getX() + Math.cos(yaw) * Math.cos(pitch) * offset;
            y_pos = _liv.getY() + _liv.getBbHeight() * 0.9 + Math.sin(pitch) * -1.0 * offset;
            z_pos = _liv.getZ() + Math.sin(yaw) * Math.cos(pitch) * offset;

            // 3. Initial Spawn Logic (Tick 1)
            if (entity.getPersistentData().getDouble("cnt2") == 0.0) {
                entity.getPersistentData().putDouble("cnt2", 1.0);
                if (!_liv.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
                }
                _liv.swing(InteractionHand.MAIN_HAND, true);

                int amp = _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                double HP = 40.0 + amp * 20.0;

                if (world instanceof ServerLevel _level) {
                    Entity redBall = ((EntityType) JujutsucraftModEntities.RED.get()).create(_level, null, null, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                    if (redBall != null) {
                        redBall.setYRot(world.getRandom().nextFloat() * 360.0F);
                        SetRangedAmmoProcedure.execute(_liv, redBall);
                        redBall.setYRot(_liv.getYRot());
                        redBall.setXRot(_liv.getXRot());
                        
                        // v43 Smooth Rotation Sync
                        redBall.setYBodyRot(redBall.getYRot());
                        redBall.setYHeadRot(redBall.getYRot());
                        redBall.yRotO = redBall.getYRot();
                        redBall.xRotO = redBall.getXRot();

                        if (redBall instanceof LivingEntity _livBall) {
                            _livBall.yBodyRotO = redBall.getYRot();
                            _livBall.yHeadRotO = redBall.getYRot();
                            if (_livBall.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                                _livBall.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HP);
                            }
                            _livBall.setHealth((float) HP);
                        }
                        _level.addFreshEntity(redBall);
                    }
                }

                if (world instanceof Level _level && !_level.isClientSide()) {
                    _level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:slow_motion_end")), SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
            }

            // 4. Ongoing Effects
            if (!_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
                _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 4, false, false));
                if (!(_liv instanceof Player)) {
                    _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 9, false, false));
                }
            }

            // 5. Sync with Red Ball (v43 Optimized Loop)
            logic_a = false;
            Vec3 center = new Vec3(x_pos, y_pos, z_pos);
            for (Entity found : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(16.0), e -> true)) {
                if (found instanceof RedEntity && entity.getPersistentData().getDouble("NameRanged") == found.getPersistentData().getDouble("NameRanged_ranged")) {
                    logic_a = true;
                    found.getPersistentData().putDouble("cnt6", Math.max(found.getPersistentData().getDouble("cnt6"), entity.getPersistentData().getDouble("cnt6")));
                    found.setDeltaMovement(entity.getDeltaMovement());
                    found.teleportTo(x_pos, y_pos, z_pos);
                    if (found instanceof ServerPlayer _sp) {
                        _sp.connection.teleport(x_pos, y_pos, z_pos, found.getYRot(), found.getXRot());
                    }
                    break;
                }
            }

            // 6. Charging Logic (Tick 20+)
            if (entity.getPersistentData().getDouble("cnt1") >= 20.0 && logic_a) {
                if (target != null) {
                    _liv.getPersistentData().putBoolean("PRESS_Z", false);
                    if (GetDistanceNearestEnemyProcedure.execute(world, _liv) > 8.0) {
                        double skill = target.getPersistentData().getDouble("skill");
                        if (skill == 0.0 || (skill != 0.0 && target.getPersistentData().getBoolean("attack")) || target.getPersistentData().getDouble("Damage") != 0.0) {
                            _liv.getPersistentData().putBoolean("PRESS_Z", true);
                        }
                    }

                    if (_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                        _liv.getPersistentData().putBoolean("PRESS_Z", false);
                    }

                    if (_liv.getPersistentData().getDouble("cnt6") >= 5.0) {
                        _liv.getPersistentData().putBoolean("PRESS_Z", false);
                    }
                }

                if (_liv.getPersistentData().getBoolean("PRESS_Z")) {
                    _liv.getPersistentData().putDouble("cnt1", Math.min(_liv.getPersistentData().getDouble("cnt1"), 20.0));

                    // Particles (v43 Style)
                    String particle = _liv.getPersistentData().getDouble("cnt6") >= 5.0 ? "minecraft:enchanted_hit" : "minecraft:crit";
                    for (int i = 0; i < 2; i++) {
                        ParticleGeneratorProcedure.execute(world, 0.0, 1.0, 0.0, 2.0, x_pos + (Math.random() - 0.5) * 2.0, x_pos, y_pos + (Math.random() - 0.5) * 2.0, y_pos, z_pos + (Math.random() - 0.5) * 2.0, z_pos, particle);
                    }

                    // Chant Accumulation
                    if (_liv.getPersistentData().getDouble("cnt6") < 3.0) {
                        double cnt5 = _liv.getPersistentData().getDouble("cnt5") + 1.0;
                        _liv.getPersistentData().putDouble("cnt5", cnt5);
                        if (cnt5 > 20.0) {
                            _liv.getPersistentData().putDouble("cnt5", 0.0);
                            double cnt6 = _liv.getPersistentData().getDouble("cnt6") + 1.0;
                            _liv.getPersistentData().putDouble("cnt6", cnt6);
                            
                            if (_liv instanceof Player _player && !_player.level().isClientSide()) {
                                _player.displayClientMessage(Component.literal("§l\"" + Component.translatable("chant.jujutsucraft.red" + Math.round(cnt6)).getString() + "\""), false);
                            }

                            _liv.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                                cap.PlayerCursePowerChange -= 25.0;
                                cap.syncPlayerVariables(_liv);
                            });
                        }
                    } else if (_liv.getPersistentData().getDouble("cnt6") < 5.0) {
                        _liv.getPersistentData().putDouble("cnt6", 5.0);
                        if (!_liv.level().isClientSide()) {
                            _liv.level().explode(null, x_pos, y_pos, z_pos, 0.0F, Level.ExplosionInteraction.NONE);
                            _liv.level().playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:electric_shock")), SoundSource.NEUTRAL, 1.0F, 1.0F);
                        }
                    }
                }
            }

            if (entity.getPersistentData().getDouble("cnt1") > 20.0) {
                entity.getPersistentData().putDouble("skill", 0.0);
            }

            // 7. Addon Animations (Tactical Mode Support)
            JujutsucraftaddonModVariables.PlayerVariables addonVars = _liv.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());
            if ("Tatical Mode".equals(addonVars.Mode)) {
                PlayGojoRed2Procedure.execute(world, x, y, z, _liv);
            } else {
                PlayAnimationProcedure.execute(world, _liv);
            }
        }
    }
}
