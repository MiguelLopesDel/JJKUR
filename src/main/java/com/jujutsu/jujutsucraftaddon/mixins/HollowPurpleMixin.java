package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.entity.BlueEntity;
import net.mcreator.jujutsucraft.entity.GojoSatoruEntity;
import net.mcreator.jujutsucraft.entity.RedEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
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

@Mixin(value = HollowPurpleProcedure.class, priority = -10000)
public abstract class HollowPurpleMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        boolean logic_a = false;
        double rnd = 0.0;
        double z_pos = 0.0;
        double dis = 0.0;
        double y_pos = 0.0;
        double x_pos = 0.0;
        double HP = 0.0;
        double rad = 0.0;
        double rad_now = 0.0;
        double yaw = 0.0;
        double pitch = 0.0;
        double distance = 0.0;
        double range = ReturnEntitySizeProcedure.execute(entity);

        entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);

        if (entity instanceof LivingEntity _liv) {
            if (!_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
            }

            LivingEntity target = (_liv instanceof Mob _mob) ? _mob.getTarget() : null;
            if (target != null) {
                RotateEntityProcedure.execute(target.getX(), target.getY() + (double) target.getBbHeight() * 0.5, target.getZ(), _liv);
            }

            _liv.getPersistentData().putDouble("x_power", _liv.getLookAngle().x * 3.0);
            _liv.getPersistentData().putDouble("y_power", _liv.getLookAngle().y * 3.0);
            _liv.getPersistentData().putDouble("z_power", _liv.getLookAngle().z * 3.0);

            if (!_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 9, false, false));
            }

            // TICK 1: Spawning RED and BLUE
            if (entity.getPersistentData().getDouble("cnt1") == 1.0) {
                // Addon specific: Decisive Battle Chestplate logic
                ItemStack chestItem = _liv.getItemBySlot(EquipmentSlot.CHEST);
                if (entity instanceof GojoSatoruEntity && chestItem.getItem() == JujutsucraftModItems.CLOTHES_DECISIVE_BATTLE_CHESTPLATE.get()) {
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(20010.0);
                    }
                    if (!_liv.level().isClientSide() && _liv.getServer() != null) {
                        _liv.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _liv.position(), _liv.getRotationVector(), world instanceof ServerLevel ? (ServerLevel) world : null, 4, _liv.getName().getString(), _liv.getDisplayName(), _liv.level().getServer(), _liv), "particle jjkueffects:red_and_blue ~ ~1 ~ 0 0 0 1 1 force");
                    }
                    if (!_liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get())) {
                        if (!_liv.level().isClientSide()) {
                            _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get(), 240, 1, false, false));
                        }
                    }
                    if (!_liv.hasEffect(JujutsucraftaddonModMobEffects.WORLD_GOJO.get())) {
                        if (!_liv.level().isClientSide()) {
                            _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.WORLD_GOJO.get(), 240, 1, false, false));
                        }
                    }
                    PlayAnimationProcedure.execute(world, _liv);
                }

                int amp = _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                HP = 40.0 + amp * 20.0;
                pitch = Math.toRadians(_liv.getXRot());
                distance = 2.0 + _liv.getBbWidth(); // v43 uses 2.0 + width

                // Spawn RED
                yaw = Math.toRadians(_liv.getYRot() + 90.0F - 40.0F);
                x_pos = _liv.getX() + Math.cos(yaw) * Math.cos(pitch) * distance;
                y_pos = _liv.getY() + (double) _liv.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * distance;
                z_pos = _liv.getZ() + Math.sin(yaw) * Math.cos(pitch) * distance;
                if (world instanceof ServerLevel _level) {
                    Entity entityinstance = ((EntityType) JujutsucraftModEntities.RED.get()).create(_level, null, null, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                    if (entityinstance != null) {
                        SetRangedAmmoProcedure.execute(_liv, entityinstance);
                        if (!entityinstance.level().isClientSide() && entityinstance.getServer() != null) {
                            entityinstance.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entityinstance.position(), entityinstance.getRotationVector(), _level, 4, entityinstance.getName().getString(), entityinstance.getDisplayName(), entityinstance.level().getServer(), entityinstance), "data merge entity @s {Invulnerable:1b}");
                        }
                        entityinstance.setYRot(_liv.getYRot());
                        entityinstance.setXRot(_liv.getXRot());
                        if (entityinstance instanceof LivingEntity _livInstance) {
                            _livInstance.yBodyRot = _liv.getYRot();
                            _livInstance.yHeadRot = _liv.getYRot();
                            if (_livInstance.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                                _livInstance.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HP);
                            }
                            _livInstance.setHealth((float) HP);
                            if (_livInstance.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.SIZE.get())) {
                                _livInstance.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).setBaseValue(2.0);
                            }
                        }
                        if (entityinstance instanceof RedEntity _red) {
                            _red.getEntityData().set(RedEntity.DATA_flag_purple, true);
                        }
                        _level.addFreshEntity(entityinstance);
                    }
                }

                // Spawn BLUE
                yaw = Math.toRadians(_liv.getYRot() + 90.0F + 40.0F);
                x_pos = _liv.getX() + Math.cos(yaw) * Math.cos(pitch) * distance;
                y_pos = _liv.getY() + (double) _liv.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * distance;
                z_pos = _liv.getZ() + Math.sin(yaw) * Math.cos(pitch) * distance;
                if (world instanceof ServerLevel _level) {
                    Entity entityinstance = ((EntityType) JujutsucraftModEntities.BLUE.get()).create(_level, null, null, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                    if (entityinstance != null) {
                        SetRangedAmmoProcedure.execute(_liv, entityinstance);
                        if (!entityinstance.level().isClientSide() && entityinstance.getServer() != null) {
                            entityinstance.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entityinstance.position(), entityinstance.getRotationVector(), _level, 4, entityinstance.getName().getString(), entityinstance.getDisplayName(), entityinstance.level().getServer(), entityinstance), "data merge entity @s {Invulnerable:1b}");
                        }
                        entityinstance.setYRot(_liv.getYRot());
                        entityinstance.setXRot(_liv.getXRot());
                        if (entityinstance instanceof LivingEntity _livInstance) {
                            _livInstance.yBodyRot = _liv.getYRot();
                            _livInstance.yHeadRot = _liv.getYRot();
                            if (_livInstance.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                                _livInstance.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HP);
                            }
                            _livInstance.setHealth((float) HP);
                            if (_livInstance.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.SIZE.get())) {
                                _livInstance.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).setBaseValue(2.0);
                            }
                        }
                        if (entityinstance instanceof BlueEntity _blue) {
                            _blue.getEntityData().set(BlueEntity.DATA_flag_purple, true);
                        }
                        _level.addFreshEntity(entityinstance);
                    }
                }
            }

            // MERGING AND ANIMATION
            if (entity.getPersistentData().getDouble("cnt1") <= 20.0 && entity.getPersistentData().getDouble("cnt4") == 0.0) {
                // Addon check for custom animation bypass
                if (!(entity instanceof GojoSatoruEntity && _liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()))) {
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(20.0);
                    }
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(0.0);
                    }
                    PlayAnimationProcedure.execute(world, _liv);
                }
            }

            if (entity.getPersistentData().getDouble("cnt1") <= 10.0) {
                Vec3 center = new Vec3(x, y, z);
                for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, (new AABB(center, center)).inflate(24.0), (e) -> true)) {
                    if (entity != entityiterator && entity.getPersistentData().getDouble("NameRanged") == entityiterator.getPersistentData().getDouble("NameRanged_ranged")) {
                        boolean isTarget = false;
                        if (entityiterator instanceof RedEntity _red && (Boolean) _red.getEntityData().get(RedEntity.DATA_flag_purple)) isTarget = true;
                        if (entityiterator instanceof BlueEntity _blue && (Boolean) _blue.getEntityData().get(BlueEntity.DATA_flag_purple)) isTarget = true;

                        if (isTarget) {
                            yaw = Math.toRadians((double) (entity.getYRot() + 90.0F) + Math.max(40.0 - entity.getPersistentData().getDouble("cnt1") * 4.0, 0.0));
                            pitch = Math.toRadians(entity.getXRot());
                            distance = 2.0 + entity.getBbWidth();
                            x_pos = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * distance;
                            y_pos = entity.getY() + (double) entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * distance;
                            z_pos = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * distance;
                            entityiterator.teleportTo(x_pos, y_pos, z_pos);
                            if (entityiterator instanceof ServerPlayer _sp) {
                                _sp.connection.teleport(x_pos, y_pos, z_pos, entityiterator.getYRot(), entityiterator.getXRot());
                            }
                        }
                    }
                }

                if (entity.getPersistentData().getDouble("cnt1") == 10.0 && world instanceof ServerLevel _level) {
                    _level.sendParticles(ParticleTypes.FLASH, x_pos, y_pos, z_pos, 5, 0.25, 0.25, 0.25, 0.0);
                }
            } else if (entity.getPersistentData().getDouble("cnt1") <= 20.0) {
                // CHARGING (Ticks 11-20)
                if (entity.getPersistentData().getDouble("cnt1") >= 19.0) {
                    if (entity.getPersistentData().getDouble("cnt4") > 0.0 && entity.getPersistentData().getDouble("cnt6") >= 0.0) {
                        for (int i = 0; i < (int) (2.0 * range); i++) {
                            ParticleGeneratorCircleProcedure.execute(world, 1.0, 90.0, 0.5 * range, 0.75 * range, 4.0 * range, entity.getX(), entity.getX(), entity.getY(), entity.getY() + Math.random() * range, 0.0, entity.getZ(), entity.getZ(), entity.getPersistentData().getDouble("cnt6") >= 6.0 ? "enchanted_hit" : "crit");
                        }
                        for (int i = 0; i < (int) (2.0 * range); i++) {
                            ParticleGeneratorCircleProcedure.execute(world, 1.0, 90.0, 2.0 * range, 8.0 * range, 1.0 * range, entity.getX(), entity.getX(), entity.getY(), entity.getY() + Math.random() * range, 0.0, entity.getZ(), entity.getZ(), "cloud");
                        }

                        if (entity.getPersistentData().getDouble("cnt6") >= 5.0 && Math.random() < 0.1) {
                            // Addon check for sound silence
                            if (!(_liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()))) {
                                if (world instanceof Level _level) {
                                    if (!_level.isClientSide()) _level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:electric_shock")), SoundSource.NEUTRAL, 1.0F, 1.0F);
                                    else _level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:electric_shock")), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
                                }
                            }
                            for (int i = 0; i < (int) (4.0 * range); i++) {
                                ParticleGeneratorCircleProcedure.execute(world, 1.0, 90.0, 0.0, 8.0 * range, Mth.nextDouble(RandomSource.create(), 0.0, 2.0) * range, entity.getX(), entity.getX(), entity.getY(), entity.getY() + Math.random() * range, 0.0, entity.getZ(), entity.getZ(), "jujutsucraft:particle_thunder_purple");
                            }
                        }
                    }

                    LivingEntity current_target = (_liv instanceof Mob _mob) ? _mob.getTarget() : null;
                    if (current_target != null) {
                        if (!(_liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()))) {
                            entity.getPersistentData().putBoolean("PRESS_Z", false);
                        }
                        if (GetDistanceNearestEnemyProcedure.execute(world, entity) > 12.0) {
                            boolean targetLogic = false;
                            if (current_target.getPersistentData().getDouble("skill") == 0.0 || (current_target.getPersistentData().getDouble("skill") != 0.0 && current_target.getPersistentData().getBoolean("attack")) || current_target.getPersistentData().getDouble("Damage") != 0.0) {
                                if (current_target != entity) targetLogic = true;
                            }
                            if (targetLogic) entity.getPersistentData().putBoolean("PRESS_Z", true);
                        }

                        if (!(entity instanceof GojoSatoruEntity)) {
                            if (!(_liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()))) {
                                entity.getPersistentData().putBoolean("PRESS_Z", false);
                            }
                        }
                        if (entity.getPersistentData().getDouble("cnt6") >= 5.0) {
                            if (!(_liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()))) {
                                entity.getPersistentData().putBoolean("PRESS_Z", false);
                            }
                        }
                    }

                    if (entity.getPersistentData().getBoolean("PRESS_Z")) {
                        entity.getPersistentData().putDouble("cnt1", 19.0);
                        if (!_liv.level().isClientSide()) {
                            _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 9, 5, false, false));
                        }

                        yaw = Math.toRadians(entity.getYRot() + 90.0F);
                        pitch = Math.toRadians(entity.getXRot());
                        distance = 2.0 + entity.getBbWidth();
                        x_pos = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * distance;
                        y_pos = entity.getY() + (double) entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * distance;
                        z_pos = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * distance;

                        if (!(_liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()))) {
                            ChargeParticleProcedure.execute(world, entity, entity.getPersistentData().getDouble("cnt6") >= 6.0 ? 1.0 : 0.0);
                        }

                        if (entity.getPersistentData().getDouble("cnt6") < 4.0) {
                            entity.getPersistentData().putDouble("cnt5", entity.getPersistentData().getDouble("cnt5") + 1.0);
                            if (entity.getPersistentData().getDouble("cnt5") > 20.0) {
                                if (entity.getPersistentData().getDouble("cnt4") == 0.0) {
                                    entity.getPersistentData().putDouble("cnt4", 1.0);
                                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(215.0);
                                    }
                                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(0.0);
                                    }
                                    PlayAnimationProcedure.execute(world, _liv);
                                }

                                entity.getPersistentData().putDouble("cnt5", 0.0);
                                entity.getPersistentData().putDouble("cnt6", entity.getPersistentData().getDouble("cnt6") + 1.0);
                                if (entity instanceof Player _player && !_player.level().isClientSide()) {
                                    _player.displayClientMessage(Component.literal("§l\"" + Component.translatable("chant.jujutsucraft.purple" + Math.round(entity.getPersistentData().getDouble("cnt6"))).getString() + "\""), false);
                                }

                                if (entity instanceof Player _player) {
                                    double cost = 50.0;
                                    _player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                                        cap.PlayerCursePowerChange -= cost;
                                        cap.syncPlayerVariables(_player);
                                    });
                                }
                            }
                        }

                        if (entity.getPersistentData().getDouble("cnt6") >= 4.0 && entity.getPersistentData().getDouble("cnt6") < 6.0) {
                            entity.getPersistentData().putDouble("cnt6", 6.0);
                            if (world instanceof Level _level && !_level.isClientSide()) {
                                _level.explode(null, x_pos, y_pos, z_pos, 0.0F, Level.ExplosionInteraction.NONE);
                            }
                            if (world instanceof ServerLevel _level) {
                                _level.sendParticles(ParticleTypes.FLASH, x_pos, y_pos, z_pos, 10, 0.25, 0.25, 0.25, 1.5);
                            }
                            if (!(_liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()))) {
                                if (world instanceof Level _level && !_level.isClientSide()) {
                                    _level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:electric_shock")), SoundSource.NEUTRAL, 2.0F, 1.0F);
                                }
                            }
                        }
                    }
                }
            } else {
                // SPAWNING PURPLE (Tick 21+)
                if (entity.getPersistentData().getDouble("cnt2") == 0.0) {
                    boolean shouldExplode = false;
                    if (entity instanceof Player _player) {
                        if (entity.getPersistentData().getDouble("cnt6") >= 6.0 && _player.isShiftKeyDown()) shouldExplode = true;
                    } else if (entity instanceof GojoSatoruEntity _gojo) {
                        if (_gojo.getHealth() < _gojo.getMaxHealth() * 0.4 && !_gojo.hasEffect(JujutsucraftaddonModMobEffects.WORLD_GOJO.get())) {
                            shouldExplode = true;
                        }
                    }
                    entity.getPersistentData().putDouble("cnt2", shouldExplode ? 2.0 : 1.0);
                }

                if (entity.getPersistentData().getDouble("cnt2") == 1.0) {
                    if (entity.getPersistentData().getDouble("cnt1") == 21.0) {
                        if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                            _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(215.0);
                        }
                        if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                            _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(1.0);
                        }
                        PlayAnimationProcedure.execute(world, _liv);
                    }
                    if (entity.getPersistentData().getDouble("cnt1") == 28.0) logic_a = true;
                } else {
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(2015.0);
                    }
                    if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(0.0);
                    }
                    PlayAnimationProcedure.execute(world, _liv);
                    if (entity.getPersistentData().getDouble("cnt1") == 21.0) logic_a = true;
                }

                if (logic_a) {
                    int boost = _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                    HP = 400.0 + boost * 40.0;
                    yaw = Math.toRadians(entity.getYRot() + 90.0F);
                    pitch = Math.toRadians(entity.getXRot());

                    if (world instanceof ServerLevel _level) {
                        Entity entityinstance = ((EntityType) JujutsucraftModEntities.PURPLE.get()).create(_level, null, null, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                            SetRangedAmmoProcedure.execute(entity, entityinstance);
                            entityinstance.setYRot(entity.getYRot());
                            entityinstance.setXRot(entity.getXRot());
                            if (entityinstance instanceof LivingEntity _purpleLiv) {
                                _purpleLiv.yBodyRot = entity.getYRot();
                                _purpleLiv.yHeadRot = entity.getYRot();
                                if (_purpleLiv.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                                    _purpleLiv.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HP);
                                }
                                _purpleLiv.setHealth((float) HP);
                            }

                            entityinstance.getPersistentData().putDouble("cnt6", entity.getPersistentData().getDouble("cnt6"));
                            double purpleSize;
                            if (entity.getPersistentData().getDouble("cnt6") >= 4.0) {
                                purpleSize = 8.0 * (0.5 + entity.getPersistentData().getDouble("cnt6") * 0.5);
                            } else {
                                purpleSize = 8.0 * (0.5 + entity.getPersistentData().getDouble("cnt6") * 0.2);
                            }
                            entityinstance.getPersistentData().putDouble("size", purpleSize);

                            if (entityinstance instanceof LivingEntity _purpleLiv) {
                                if (_purpleLiv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.SIZE.get())) {
                                    _purpleLiv.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).setBaseValue(5.0);
                                }
                            }

                            if (entity.getPersistentData().getDouble("cnt2") == 2.0) {
                                entityinstance.getPersistentData().putDouble("size", 5.0);
                                entityinstance.getPersistentData().putDouble("cnt6", Math.max(entity.getPersistentData().getDouble("cnt6") - 4.0, 2.0));
                                entityinstance.getPersistentData().putString("OWNER_UUID", "");
                                entityinstance.getPersistentData().putBoolean("explode", true);
                                entityinstance.getPersistentData().putBoolean("betrayal", true);
                                x_pos = entity.getX();
                                y_pos = entity.getY() + 2.0 + entity.getBbHeight() + Math.max(entityinstance.getPersistentData().getDouble("size"), 5.0) * 0.2;
                                z_pos = entity.getZ();
                            } else {
                                distance = 2.0 + (entity.getBbWidth() + Math.max(entityinstance.getPersistentData().getDouble("size"), 5.0) * 0.2) * 0.75;
                                x_pos = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * distance;
                                y_pos = entity.getY() + (double) entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * distance;
                                z_pos = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * distance;
                            }

                            entityinstance.teleportTo(x_pos, y_pos, z_pos);
                            if (entityinstance instanceof ServerPlayer _sp) {
                                _sp.connection.teleport(x_pos, y_pos, z_pos, entityinstance.getYRot(), entityinstance.getXRot());
                            }
                            _level.addFreshEntity(entityinstance);
                        }
                    }
                }

                if (entity.getPersistentData().getDouble("cnt1") > 66.0) {
                    entity.getPersistentData().putDouble("skill", 0.0);
                }
            }
        }
    }
}
