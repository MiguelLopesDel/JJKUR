package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.ErroEntity;
import com.jujutsu.jujutsucraftaddon.entity.ErrorEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.ProjectileSlashEntity;
import net.mcreator.jujutsucraft.entity.SukunaEntity;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DismantleProcedure.class, priority = -10000)
public abstract class DismantleProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        Entity target_entity = null;
        boolean vertical = false;
        boolean canUseWorld = false;
        boolean worldCutter = false;
        double distance = 0.0;
        double HP = 0.0;
        double x_pos = 0.0;
        double z_pos = 0.0;
        double yaw = 0.0;
        double CNT6 = 0.0;
        double size = 0.0;
        double y_pos = 0.0;
        double pitch = 0.0;

        LivingEntity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;

        entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);

        // World Cut Permission Logic
        if (entity instanceof Player _player) {
            if (_player instanceof ServerPlayer _serverPlayer) {
                if (_serverPlayer.level() instanceof ServerLevel && _serverPlayer.getAdvancements().getOrStartProgress(_serverPlayer.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:skill_dismantle_cut_the_world"))).isDone()) {
                    canUseWorld = true;
                }
            }
        } else {
            if (entity instanceof SukunaFushiguroEntity _sukunaF && (Boolean) _sukunaF.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut)) {
                canUseWorld = true;
            } else if (entity instanceof SukunaPerfectEntity || entity instanceof ErrorEntity || entity instanceof ErroEntity) {
                canUseWorld = true;
            }
        }

        if (target instanceof LivingEntity) {
            if (canUseWorld && entity instanceof LivingEntity _liv) {
                if (!_liv.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
                }
            }

            entity.lookAt(Anchor.EYES, new Vec3(target.getX(), target.getY() + (double) target.getBbHeight() * 0.5, target.getZ()));
            entity.getPersistentData().putBoolean("PRESS_Z", false);

            if (GetDistanceNearestEnemyProcedure.execute(world, entity) > 6.0) {
                if (target.getPersistentData().getDouble("skill") == 0.0 || (target.getPersistentData().getDouble("skill") != 0.0 && target.getPersistentData().getBoolean("attack")) || target.getPersistentData().getDouble("Damage") == 0.0) {
                    entity.getPersistentData().putBoolean("PRESS_Z", true);
                }
            }

            if (canUseWorld) {
                if (target.hasEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get()) && !entity.getPersistentData().getBoolean("flag_dismantle")) {
                    entity.getPersistentData().putBoolean("flag_dismantle", true);
                    entity.getPersistentData().putDouble("cnt6", 5.0);
                    RotateEntityProcedure.execute(target.getX(), target.getY() + (double) target.getBbHeight() * 0.5, target.getZ(), entity);
                }
            }

            if (entity.getPersistentData().getDouble("cnt6") >= 5.0) {
                entity.getPersistentData().putBoolean("PRESS_Z", false);
            }
        }

        // Shift / RNG mode selection
        if (entity.getPersistentData().getDouble("cnt7") == 0.0) {
            if (entity instanceof Player _player) {
                entity.getPersistentData().putDouble("cnt7", _player.isShiftKeyDown() ? 1.0 : 2.0);
            } else {
                entity.getPersistentData().putDouble("cnt7", Math.random() < 0.5 ? 1.0 : 2.0);
            }

            if (target instanceof LivingEntity && canUseWorld && target.hasEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
                entity.getPersistentData().putDouble("cnt7", 2.0);
            }
        }

        if (entity.getPersistentData().getDouble("cnt7") == 1.0) {
            if (entity instanceof LivingEntity _liv) {
                if (!_liv.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 6, false, false));
                    _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
                }
            }
            entity.getPersistentData().putDouble("cnt6", -1.25);
        } else {
            // Charging Logic
            if (entity.getPersistentData().getDouble("cnt1") <= 1.0) {
                yaw = Math.toRadians((double) (entity.getYRot() + 90.0F));
                pitch = Math.toRadians((double) entity.getXRot());
                distance = (double) (1.0F + entity.getBbWidth());
                x_pos = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * distance;
                y_pos = entity.getY() + (double) entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * distance;
                z_pos = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * distance;

                if (entity.getPersistentData().getBoolean("PRESS_Z")) {
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    if (entity instanceof LivingEntity _liv) {
                        if (!_liv.level().isClientSide()) {
                            _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 4, false, false));
                            _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
                        }
                    }

                    if (entity.getPersistentData().getDouble("cnt6") < 3.0) {
                        entity.getPersistentData().putDouble("cnt5", entity.getPersistentData().getDouble("cnt5") + 1.0);
                        if (entity.getPersistentData().getDouble("cnt5") > 20.0) {
                            entity.getPersistentData().putDouble("cnt5", 0.0);
                            entity.getPersistentData().putDouble("cnt6", entity.getPersistentData().getDouble("cnt6") + 1.0);
                            if (entity instanceof Player _player && !_player.level().isClientSide()) {
                                _player.displayClientMessage(Component.literal("§l\"" + Component.translatable("chant.jujutsucraft.dismantle" + Math.round(entity.getPersistentData().getDouble("cnt6"))).getString() + "\""), false);
                            }
                            if (entity instanceof Player) {
                                double cost = 30.0;
                                entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                                    cap.PlayerCursePowerChange -= cost;
                                    cap.syncPlayerVariables(entity);
                                });
                            }
                        }
                    } else if (entity.getPersistentData().getDouble("cnt6") < 5.0) {
                        entity.getPersistentData().putDouble("cnt6", 5.0);
                        if (world instanceof Level _level && !_level.isClientSide()) {
                            _level.explode(null, x_pos, y_pos, z_pos, 0.0F, Level.ExplosionInteraction.NONE);
                        }
                        if (world instanceof ServerLevel _serverLevel && !(entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get()))) {
                            _serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, x_pos, y_pos, z_pos, 20, 0.25, 0.25, 0.25, 1.0);
                        }
                    }

                    if (!(entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get()))) {
                        ChargeParticleProcedure.execute(world, entity, entity.getPersistentData().getDouble("cnt6") >= 5.0 ? 1.0 : 0.0);
                    }
                }
            }
        }

        // Animation Pre-Slash
        if (entity.getPersistentData().getDouble("cnt1") == 0.0 && canUseWorld) {
            if (entity instanceof LivingEntity _liv) {
                if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                    _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(entity.getPersistentData().getDouble("cnt6") >= 5.0 ? 207 : 120);
                }
            }
            PlayAnimationProcedure.execute(world, entity);
        }

        CNT6 = 1.0 + entity.getPersistentData().getDouble("cnt6") * 0.2;
        if (entity.getPersistentData().getDouble("cnt1") == 1.0) {
            if (entity.getPersistentData().getDouble("cnt7") == 1.0) {
                if (entity.getPersistentData().getDouble("cnt8") == 1.0) {
                    if (entity instanceof SukunaFushiguroEntity || entity instanceof SukunaEntity || entity instanceof SukunaPerfectEntity) {
                        if (entity instanceof LivingEntity _liv) {
                            _liv.swing(InteractionHand.MAIN_HAND, true);
                            if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                                _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(240.0);
                            }
                        }
                        PlayAnimationProcedure.execute(world, entity);
                    }
                }
                entity.getPersistentData().putDouble("cnt1", Math.max(entity.getPersistentData().getDouble("cnt1"), 5.0));
            }

            if (entity.getPersistentData().getDouble("cnt6") >= 0.0) {
                if (entity instanceof LivingEntity _liv) {
                    _liv.swing(InteractionHand.MAIN_HAND, true);
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(207.0);
                    }
                }
                if (canUseWorld && entity.getPersistentData().getDouble("cnt6") >= 4.0) {
                    entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                        cap.PlayerCursePower -= 500.0;
                        cap.syncPlayerVariables(entity);
                    });
                }
                PlayAnimationProcedure.execute(world, entity);
            }
        }

        // EXECUTION - SLASH SPAWN
        if (entity.getPersistentData().getDouble("cnt1") == 5.0) {
            vertical = Math.random() < 0.5;
            double destructionLevel = (double) world.getLevelData().getGameRules().getInt(com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules.JJKU_DESTRUCTION_LEVEL);

            if (entity.getPersistentData().getDouble("cnt6") >= 5.0 && canUseWorld) {
                worldCutter = true;
                entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                    cap.PlayerCursePower -= 500.0;
                    cap.syncPlayerVariables(entity);
                });
            }

            if (entity.getPersistentData().getDouble("cnt6") >= 0.0) {
                if (world instanceof Level _level) {
                    SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:sword_sweep"));
                    float volume = (float) (0.5 * CNT6);
                    if (!_level.isClientSide()) {
                        _level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, volume, 0.5F);
                        _level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, volume, 0.75F);
                        _level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, volume, 1.0F);
                    } else {
                        _level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, 0.5F, false);
                        _level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, 0.75F, false);
                        _level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, 1.0F, false);
                    }
                }

                if (entity instanceof LivingEntity _liv) {
                    _liv.swing(InteractionHand.MAIN_HAND, true);
                    if (!canUseWorld) {
                        if (vertical) {
                            if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                                _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(207.0);
                            }
                        } else {
                            if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                                _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(-5.0);
                            }
                            if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                                _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(Mth.nextInt(RandomSource.create(), 0, 1));
                            }
                        }
                    }
                    PlayAnimationProcedure.execute(world, entity);
                }
            }

            if (world instanceof Level _level) {
                SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:sword_sweep"));
                if (!_level.isClientSide()) {
                    _level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, 2.0F, 2.0F);
                } else {
                    _level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, 2.0F, 2.0F, false);
                }
            }

            // Spawn Slash Entity
            int amp = (entity instanceof LivingEntity _liv && _liv.hasEffect(MobEffects.DAMAGE_BOOST)) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
            HP = (double) (30 + amp * 7) * CNT6;
            size = 0.5 * ReturnEntitySizeProcedure.execute(entity);

            yaw = Math.toRadians((double) (entity.getYRot() + 90.0F) + Mth.nextDouble(RandomSource.create(), -90.0, 90.0));
            pitch = Math.toRadians((double) entity.getXRot());
            double dist = 0.5 * entity.getBbWidth();
            x_pos = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * dist;
            y_pos = entity.getY() + (double) entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * dist;
            z_pos = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * dist;

            if (world instanceof ServerLevel _serverLevel) {
                Entity entityinstance = ((EntityType) JujutsucraftModEntities.PROJECTILE_SLASH.get()).create(_serverLevel, null, null, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                if (entityinstance != null) {
                    entityinstance.setYRot(world.getRandom().nextFloat() * 360.0F);
                    SetRangedAmmoProcedure.execute(entity, entityinstance);
                    if (!entityinstance.level().isClientSide() && entityinstance.getServer() != null) {
                        entityinstance.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entityinstance.position(), entityinstance.getRotationVector(), _serverLevel, 4, entityinstance.getName().getString(), entityinstance.getDisplayName(), entityinstance.level().getServer(), entityinstance), "data merge entity @s {NoAI:1b}");
                    }

                    entityinstance.setYRot(entity.getYRot());
                    entityinstance.setXRot(entity.getXRot());
                    entityinstance.setYBodyRot(entityinstance.getYRot());
                    entityinstance.setYHeadRot(entityinstance.getYRot());
                    entityinstance.yRotO = entityinstance.getYRot();
                    entityinstance.xRotO = entityinstance.getXRot();

                    if (entityinstance instanceof LivingEntity _livInstance) {
                        _livInstance.yBodyRotO = _livInstance.getYRot();
                        _livInstance.yHeadRotO = _livInstance.getYRot();

                        if (_livInstance.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                            _livInstance.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HP);
                        }
                        _livInstance.setHealth((float) HP);
                        if (_livInstance.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.SIZE.get())) {
                            _livInstance.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).setBaseValue(size);
                        }
                    }

                    entityinstance.getPersistentData().putDouble("size", size * 5.0 * (1.0 + 0.5 * entity.getPersistentData().getDouble("cnt6")));
                    entityinstance.getPersistentData().putDouble("cnt6", entity.getPersistentData().getDouble("cnt6") + entity.getPersistentData().getDouble("cnt8") * 0.025);
                    entityinstance.getPersistentData().putDouble("x_power", entity.getLookAngle().x * 9.0);
                    entityinstance.getPersistentData().putDouble("y_power", entity.getLookAngle().y * 9.0);
                    entityinstance.getPersistentData().putDouble("z_power", entity.getLookAngle().z * 9.0);

                    // Repasse de NBTs Críticos para a Entidade
                    double kb = 0.25 * Math.min(CNT6, 1.0);
                    if (entity.getPersistentData().getDouble("cnt7") == 1.0) kb *= 0.5;
                    entityinstance.getPersistentData().putDouble("knockback", kb);
                    entityinstance.getPersistentData().putDouble("projectile_type", 1.0);
                    entityinstance.getPersistentData().putDouble("effect", 1.0);

                    if (worldCutter && entityinstance instanceof ProjectileSlashEntity _slash) {
                        _slash.getEntityData().set(ProjectileSlashEntity.DATA_mode, 1);

                        // Restaurar Dano Instantâneo e Destruição de Blocos do World Cut
                        double ray_dis = 0.0;
                        for (int i = 0; i < 40; i++) {
                            double ray_x = entity.getX() + entity.getLookAngle().x * ray_dis;
                            double ray_y = entity.getY() + entity.getBbHeight() * 0.75 + entity.getLookAngle().y * ray_dis;
                            double ray_z = entity.getZ() + entity.getLookAngle().z * ray_dis;

                            entity.getPersistentData().putBoolean("ignore", true);
                            entity.getPersistentData().putDouble("effectConfirm", 3.0);
                            entity.getPersistentData().putDouble("Damage", 15.0 * CNT6);
                            entity.getPersistentData().putDouble("Range", 3.0 * CNT6 * destructionLevel);
                            RangeAttackProcedure.execute(world, ray_x, ray_y, ray_z, entity);

                            entity.getPersistentData().putBoolean("ExtinctionBlock", true);
                            entity.getPersistentData().putDouble("BlockDamage", 99999.0);
                            entity.getPersistentData().putDouble("BlockRange", 2.0 * CNT6 * destructionLevel);
                            BlockDestroyAllDirectionProcedure.execute(world, ray_x, ray_y, ray_z, entity);

                            ray_dis += 1.0; // Incremento de 1.0 para precisão total
                        }
                    }

                    if (entity.getPersistentData().getDouble("cnt6") >= 0.0) {
                        PlayAnimationEntity2Procedure.execute(entityinstance, vertical ? "vertical1" : "idle1");
                    } else {
                        RandomSource rand = RandomSource.create();
                        PlayAnimationEntity2Procedure.execute(entityinstance, (vertical ? "vertical" : "idle") + Math.round((float) Mth.nextInt(rand, 1, 5)));
                    }

                    _serverLevel.addFreshEntity(entityinstance);
                }
            }

            // Mastery Tracking
            if (entity.getPersistentData().getDouble("cnt7") == 1.0) {
                if (target instanceof LivingEntity) {
                    entity.getPersistentData().putBoolean("PRESS_Z", true);
                }

                if ((!entity.getPersistentData().getBoolean("PRESS_Z") || !(entity.getPersistentData().getDouble("cnt8") < 20.0)) && !(entity.getPersistentData().getDouble("cnt8") < 5.0)) {
                    updateMastery(entity);
                    entity.getPersistentData().putDouble("skill", 0.0);
                } else {
                    entity.getPersistentData().putDouble("cnt1", 0.0); // Reset original para 0.0
                    entity.getPersistentData().putDouble("cnt8", entity.getPersistentData().getDouble("cnt8") + 1.0);

                    if (entity instanceof Player) {
                        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                            cap.PlayerCursePowerChange -= 5.0;
                            cap.syncPlayerVariables(entity);

                            if (cap.PlayerCursePower + cap.PlayerCursePowerChange <= 0) {
                                updateMastery(entity);
                                entity.getPersistentData().putDouble("skill", 0.0);
                            }
                        });
                    }
                }
            }
        }

        if (entity.getPersistentData().getDouble("cnt1") > 10.0) {
            updateMastery(entity);
            entity.getPersistentData().putDouble("skill", 0.0);
        }
    }

    private static void updateMastery(Entity entity) {
        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
            if (cap.BodyItem.getCount() >= 19.0) {
                entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonCap -> {
                    addonCap.TechniqueMastery += 1.0;
                    addonCap.syncPlayerVariables(entity);
                });
            }
        });
    }
}
