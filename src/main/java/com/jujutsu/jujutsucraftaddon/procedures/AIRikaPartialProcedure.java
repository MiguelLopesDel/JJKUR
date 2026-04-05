package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class AIRikaPartialProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        CompoundTag persistentData = entity.getPersistentData();
        double bbHeight = entity.getBbHeight();

        if (Math.random() < (1.0 / 60.0)) {
            AIRoundDeerProcedure.execute(world, x, y, z, entity);
        }

        if (world instanceof ServerLevel _level && Math.random() < 0.5) {
            double centerY = y + bbHeight * 0.5;
            _level.sendParticles(ParticleTypes.SQUID_INK, x, centerY, z, 10, 1.0, 1.0, 1.0, 0.0);
            _level.sendParticles(ParticleTypes.LARGE_SMOKE, x, centerY, z, 10, 1.0, 1.0, 1.0, 0.0);

            _level.sendParticles(ParticleTypes.SQUID_INK, x, centerY + 1, z, 10, 1.0, 1.0, 1.0, 0.0);
            _level.sendParticles(ParticleTypes.LARGE_SMOKE, x, centerY + 1, z, 10, 1.0, 1.0, 1.0, 0.0);

            _level.sendParticles(ParticleTypes.SQUID_INK, x, centerY + 2, 2.0, 10, 1.0, 1.0, 1.0, 0.0);
            _level.sendParticles(ParticleTypes.LARGE_SMOKE, x, centerY + 2, z, 10, 1.0, 1.0, 1.0, 0.0);
        }

        if (!entity.isAlive()) return;

        AIActiveProcedure.execute(world, x, y, z, entity);

        if (entity instanceof LivingEntity livingEntity) {
            if (!livingEntity.hasEffect(MobEffects.DAMAGE_BOOST)) {
                entity.setMaxUpStep((float) Math.max(entity.getStepHeight(), 2.4));
                if (!world.isClientSide()) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 18, false, false));
                }
            }

            if (!livingEntity.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                if (!world.isClientSide()) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false));
                }
            }
        }

        double skill = persistentData.getDouble("skill");

        if (skill == 0.0) {
            if (persistentData.getDouble("despawn_flag") > 0.0 && persistentData.getDouble("friend_num_worker") != 0.0) {
                persistentData.putDouble("cnt_target", 0.0);
                double cntDie = persistentData.getDouble("cnt_die") + 1.0;
                persistentData.putDouble("cnt_die", cntDie);

                if (world instanceof ServerLevel _level) {
                    double centerY = y + bbHeight * 0.5;
                    _level.sendParticles(ParticleTypes.ASH, x, centerY, z, 40, 1.0, 1.0, 1.0, 0.0);
                    _level.sendParticles(ParticleTypes.LARGE_SMOKE, x, centerY, z, 10, 1.0, 1.0, 1.0, 0.0);
                }

                double dieLimit = (persistentData.getDouble("despawn_flag") == 1.0) ? 60.0 : 5.0;
                if (cntDie > dieLimit) {
                    DieRikaProcedure.execute(world, entity);
                    if (!world.isClientSide()) {
                        entity.discard();
                    }
                }
            } else {
                boolean flagAttack = persistentData.getBoolean("flag_attack");
                LivingEntity target = (entity instanceof Mob _mobEnt) ? _mobEnt.getTarget() : null;

                if ((!(target != null) || !(persistentData.getDouble("cnt_target") > 6.0)) && !flagAttack) {
                    persistentData.putDouble("cnt_x", 0.0);
                } else {
                    double cntX = persistentData.getDouble("cnt_x") + 1.0;
                    persistentData.putDouble("cnt_x", cntX);

                    if (cntX > 10.0 || flagAttack) {
                        persistentData.putBoolean("flag_attack", false);
                        ResetCounterProcedure.execute(entity);

                        boolean shouldTriggerSkill = true;
                        if (!flagAttack) {
                            if (Math.random() >= 0.2) {
                                shouldTriggerSkill = false;
                            } else if (entity instanceof LivingEntity _living && _living.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())) {
                                shouldTriggerSkill = false;
                            }
                        }

                        if (shouldTriggerSkill) {
                            persistentData.putDouble("cnt_x", 0.0);
                            double friendNumWorker = persistentData.getDouble("friend_num_worker");
                            double rnd = Math.abs(Math.round(Math.random() * 50.0) + (long) (friendNumWorker == 0.0 ? 20 : 100));

                            if (entity instanceof LivingEntity _living && !world.isClientSide()) {
                                _living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) Math.round(rnd) / 3, 0, false, false));
                            }

                            if (friendNumWorker == 0.0) {
                                persistentData.putDouble("skill_num", persistentData.getDouble("skill_num") + 1.0);
                            }

                            if (persistentData.getDouble("skill_num") < 8.0) {
                                persistentData.putDouble("skill", 11.0);
                            } else {
                                persistentData.putDouble("skill", 12.0);
                                persistentData.putDouble("skill_num", 0.0);
                            }
                        } else {
                            persistentData.putDouble("cnt_x", 0.0);
                            CalculateAttackProcedure.execute(world, entity);
                        }
                    }
                }

                if (persistentData.getDouble("friend_num") != 0.0 && !entity.level().dimension().equals(ResourceKey.create(Registries.DIMENSION, new ResourceLocation("jujutsucraft:cursed_spirit_manipulation_dimension")))) {
                    if (LogicOwnerExistProcedure.execute(world, entity)) {
                        Entity owner = null;
                        String ownerUuid = persistentData.getString("OWNER_UUID");
                        if (world instanceof ServerLevel _serverLevel) {
                            try {
                                owner = _serverLevel.getEntity(UUID.fromString(ownerUuid));
                            } catch (Exception ignored) {}
                        }

                        if (owner != null && persistentData.getDouble("friend_num") == owner.getPersistentData().getDouble("friend_num")) {
                            if (owner.isAlive()) {
                                double distanceSq = entity.distanceToSqr(owner);
                                if (distanceSq > 576.0) {
                                    persistentData.putBoolean("canFly", true);
                                    BlockPos teleportPos = owner.level().clip(new ClipContext(owner.getEyePosition(1.0F), owner.getEyePosition(1.0F).add(owner.getViewVector(1.0F).scale(-6.0)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, owner)).getBlockPos();

                                    GetPowerForwardProcedure.execute(teleportPos.getX(), teleportPos.getY(), teleportPos.getZ(), entity);
                                    entity.setDeltaMovement(new Vec3(persistentData.getDouble("x_power") * 0.5, persistentData.getDouble("y_power") * 0.5, persistentData.getDouble("z_power") * 0.5));

                                    if (distanceSq > 1296.0) {
                                        entity.teleportTo(teleportPos.getX(), teleportPos.getY(), teleportPos.getZ());
                                        if (entity instanceof ServerPlayer _serverPlayer) {
                                            _serverPlayer.connection.teleport(teleportPos.getX(), teleportPos.getY(), teleportPos.getZ(), entity.getYRot(), entity.getXRot());
                                        }

                                        Vec3 ownerMotion = owner.getDeltaMovement();
                                        entity.setDeltaMovement(new Vec3(ownerMotion.x(), ownerMotion.y(), ownerMotion.z()));
                                        entity.setYRot(owner.getYRot());
                                        entity.setXRot(owner.getXRot());
                                        entity.setYBodyRot(entity.getYRot());
                                        entity.setYHeadRot(entity.getYRot());
                                        entity.yRotO = entity.getYRot();
                                        entity.xRotO = entity.getXRot();

                                        if (entity instanceof LivingEntity _living) {
                                            _living.yBodyRotO = _living.getYRot();
                                            _living.yHeadRotO = _living.getYRot();
                                        }
                                    }
                                }
                            } else {
                                resetOwnerData(persistentData);
                            }
                        }
                    } else {
                        resetOwnerData(persistentData);
                    }
                }
            }
        } else {
            if (skill == 11.0) {
                AIRika1Procedure.execute(world, x, y, z, entity);
            } else if (skill == 12.0) {
                AIRika3Procedure.execute(world, x, y, z, entity);
            }

            if (persistentData.getDouble("skill") == 0.0) {
                CursedTechniquePotionExpiresProcedure.execute(world, x, y, z, entity);
            }
        }
    }

    private static void resetOwnerData(CompoundTag persistentData) {
        persistentData.putDouble("friend_num", 0.0);
        persistentData.putDouble("friend_num_worker", 0.0);
        persistentData.putString("OWNER_UUID", "");
    }
}
