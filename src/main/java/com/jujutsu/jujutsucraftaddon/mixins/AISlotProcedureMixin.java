package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.entity.SlotEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.AISLOTProcedure;
import net.mcreator.jujutsucraft.procedures.GetEntityFromUUIDProcedure;
import net.mcreator.jujutsucraft.procedures.LogicOwnerExistProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationEntity2Procedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = AISLOTProcedure.class, remap = false, priority = -10000)
public abstract class AISlotProcedureMixin {

    /**
     * @author Satushi
     * @reason Update to v43 and maintain Addon custom logic for Jackpot and Reach
     */
    @Overwrite
    public static void execute(LevelAccessor world, Entity entity) {
        if (entity != null) {
            String animation = "";
            boolean decide = false;
            boolean SUCCESS = false;
            boolean logic_a = false;
            Entity entity_a = null;
            Entity entity_1 = null;
            Entity entity_2 = null;
            Entity entity_3 = null;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double num1 = 0.0;
            double range = 0.0;
            double num2 = 0.0;
            double speed = 0.0;
            double num3 = 0.0;
            double HP = 0.0;

            if (LogicOwnerExistProcedure.execute(world, entity)) {
                entity_a = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"));
                range = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius * 2.0;
                x_pos = entity.getX();
                y_pos = entity.getY() + entity.getBbHeight() * 0.5;
                z_pos = entity.getZ();
                speed = entity instanceof LivingEntity _livEnt6 && _livEnt6.hasEffect(MobEffects.MOVEMENT_SPEED) ? 2 : 1;

                if (entity_a instanceof LivingEntity _livEnt7 && _livEnt7.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                    if (!entity_a.getPersistentData().getBoolean("Failed")) {
                        entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);
                        entity_1 = GetEntityFromUUIDProcedure.execute(world, entity_a.getPersistentData().getString("SLOT1"));
                        entity_2 = GetEntityFromUUIDProcedure.execute(world, entity_a.getPersistentData().getString("SLOT2"));
                        entity_3 = GetEntityFromUUIDProcedure.execute(world, entity_a.getPersistentData().getString("SLOT3"));

                        if (!(entity_1 instanceof SlotEntity) && !entity.level().isClientSide()) {
                            entity.discard();
                        }
                        if (!(entity_2 instanceof SlotEntity) && !entity.level().isClientSide()) {
                            entity.discard();
                        }
                        if (!(entity_3 instanceof SlotEntity) && !entity.level().isClientSide()) {
                            entity.discard();
                        }

                        if (entity instanceof SlotEntity _datEntSetS) {
                            _datEntSetS.getEntityData().set(SlotEntity.DATA_ANIMATION_NAME, ((SlotEntity) entity).getSyncedAnimation());
                        }

                        if (!(entity instanceof SlotEntity _datEntS ? (String) _datEntS.getEntityData().get(SlotEntity.DATA_ANIMATION_NAME) : "").contains("slot")) {
                            if ((entity instanceof SlotEntity _datEntI ? (Integer) _datEntI.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 1) {
                                if (entity.getPersistentData().getDouble("cnt1") >= 20.0 / speed) {
                                    decide = true;
                                }
                            } else if ((entity instanceof SlotEntity _datEntIx ? (Integer) _datEntIx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 2) {
                                if (entity.getPersistentData().getDouble("cnt1") >= 30.0 / speed) {
                                    decide = true;
                                }
                            } else if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 3) {
                                if (entity.getPersistentData().getDouble("cnt1") >= 30.0 / speed) {
                                    if (world instanceof ServerLevel _level) {
                                        _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_THUNDER_BLUE.get(), x_pos, y_pos, z_pos, 5, 1.0, 2.0, 1.0, 0.5);
                                    }

                                    for (int index0 = 0; index0 < 72; index0++) {
                                        if (Math.random() < 0.2) {
                                            if (entity.getPersistentData().getDouble("reach_action") == 1.0) {
                                                if (world instanceof ServerLevel _level) {
                                                    _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_THUNDER_BLUE.get(), entity_a.getPersistentData().getDouble("x_pos_doma") + Math.sin(num1) * (range * 0.5 - 4.0), y_pos + (Math.random() - 0.5) * 5.0, entity_a.getPersistentData().getDouble("z_pos_doma") + Math.cos(num1) * (range * 0.5 - 4.0), 1, 0.1, 0.1, 0.1, 0.0);
                                                }
                                            } else if (entity.getPersistentData().getDouble("reach_action") == 2.0 && world instanceof ServerLevel _level) {
                                                _level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity_a.getPersistentData().getDouble("x_pos_doma") + Math.sin(num1) * (range * 0.5 - 4.0), y_pos + (Math.random() - 0.5) * 5.0, entity_a.getPersistentData().getDouble("z_pos_doma") + Math.cos(num1) * (range * 0.5 - 4.0), 1, 0.1, 0.1, 0.1, 0.0);
                                            }
                                        }
                                        num1 += Math.toRadians(10.0 * Math.random());
                                    }
                                }
                                if (entity.getPersistentData().getDouble("cnt1") >= 60.0 / speed) {
                                    decide = true;
                                }
                            }
                        }

                        if (decide) {
                            if (world instanceof Level _level) {
                                if (!_level.isClientSide()) {
                                    _level.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.blast")), SoundSource.NEUTRAL, 5.0F, 2.0F);
                                } else {
                                    _level.playLocalSound(x_pos, y_pos, z_pos, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.blast")), SoundSource.NEUTRAL, 5.0F, 2.0F, false);
                                }
                            }

                            if (world instanceof Level _levelx) {
                                if (!_levelx.isClientSide()) {
                                    _levelx.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.blast_far")), SoundSource.NEUTRAL, 5.0F, 2.0F);
                                } else {
                                    _levelx.playLocalSound(x_pos, y_pos, z_pos, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.blast_far")), SoundSource.NEUTRAL, 5.0F, 2.0F, false);
                                }
                            }

                            if (world instanceof Level _levelxx) {
                                if (!_levelxx.isClientSide()) {
                                    _levelxx.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.anvil.place")), SoundSource.NEUTRAL, 5.0F, 1.0F);
                                } else {
                                    _levelxx.playLocalSound(x_pos, y_pos, z_pos, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.anvil.place")), SoundSource.NEUTRAL, 5.0F, 1.0F, false);
                                }
                            }

                            if (world instanceof Level _levelxxx) {
                                if (!_levelxxx.isClientSide()) {
                                    _levelxxx.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:gacon")), SoundSource.NEUTRAL, 5.0F, 2.0F);
                                } else {
                                    _levelxxx.playLocalSound(x_pos, y_pos, z_pos, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:gacon")), SoundSource.NEUTRAL, 5.0F, 2.0F, false);
                                }
                            }

                            if (world instanceof ServerLevel _levelxxxx) {
                                _levelxxxx.sendParticles(ParticleTypes.END_ROD, x_pos, y_pos, z_pos, 25, 1.0, 2.0, 1.0, 0.05);
                                _levelxxxx.sendParticles(ParticleTypes.CRIT, x_pos, y_pos, z_pos, 25, 1.0, 2.0, 1.0, 0.05);
                            }

                            if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 1) {
                                num1 = Math.random();
                                if (Math.random() < 0.5) {
                                    if (num1 < 0.3) {
                                        num1 = 1.0;
                                    } else if (num1 < 0.6) {
                                        num1 = 3.0;
                                    } else if (num1 < 0.9) {
                                        num1 = 5.0;
                                    } else {
                                        num1 = 7.0;
                                    }
                                } else if (num1 < 0.33) {
                                    num1 = 2.0;
                                } else if (num1 < 0.66) {
                                    num1 = 4.0;
                                } else {
                                    num1 = 6.0;
                                }

                                animation = "slot" + Math.round(num1);
                                if (entity instanceof SlotEntity _datEntSetI) {
                                    _datEntSetI.getEntityData().set(SlotEntity.DATA_SLOT_NUM, (int) num1);
                                }
                            } else if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 2) {
                                if (entity_1 instanceof SlotEntity && (entity_1 instanceof SlotEntity _datEntIxxx ? (Integer) _datEntIxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM) : 0) > 0) {
                                    animation = "slot" + Math.round(entity_1 instanceof SlotEntity _datEntIxxxx ? ((Integer) _datEntIxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM)).intValue() : 0.0F);
                                    if (entity instanceof SlotEntity _datEntSetI) {
                                        _datEntSetI.getEntityData().set(SlotEntity.DATA_SLOT_NUM, entity_1 instanceof SlotEntity _datEntIxxxxx ? (Integer) _datEntIxxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM) : 0);
                                    }
                                } else {
                                    num1 = Math.ceil(Math.random() * 7.0);
                                    animation = "slot" + Math.round(num1);
                                    if (entity instanceof SlotEntity _datEntSetI) {
                                        _datEntSetI.getEntityData().set(SlotEntity.DATA_SLOT_NUM, (int) num1);
                                    }
                                }

                                if ((entity instanceof SlotEntity _datEntIxxxxx ? (Integer) _datEntIxxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM) : 0) == (entity_1 instanceof SlotEntity _datEntIxxxx ? (Integer) _datEntIxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM) : 0)) {
                                    Vec3 _center = new Vec3(entity_a.getPersistentData().getDouble("x_pos_doma"), entity_a.getPersistentData().getDouble("y_pos_doma"), entity_a.getPersistentData().getDouble("z_pos_doma"));
                                    for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(range / 2.0), e -> true)) {
                                        if (!entityiterator.level().isClientSide() && entityiterator.getServer() != null) {
                                            entityiterator.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entityiterator.position(), entityiterator.getRotationVector(), entityiterator.level() instanceof ServerLevel ? (ServerLevel) entityiterator.level() : null, 4, entityiterator.getName().getString(), entityiterator.getDisplayName(), entityiterator.level().getServer(), entityiterator), "title @s actionbar {\"text\":\"REACH\",\"color\":\"light_purple\",\"bold\":true}");
                                        }
                                    }

                                    if (Math.random() < 0.3) {
                                        entity_3.getPersistentData().putDouble("reach_action", 1.0);
                                        if (world instanceof Level _levelxxxx) {
                                            if (!_levelxxxx.isClientSide()) {
                                                _levelxxxx.playSound(null, BlockPos.containing(entity_3.getX(), entity_3.getY() + entity_3.getBbHeight() * 0.5, entity_3.getZ()), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:electric_shock")), SoundSource.NEUTRAL, 5.0F, 1.0F);
                                            } else {
                                                _levelxxxx.playLocalSound(entity_3.getX(), entity_3.getY() + entity_3.getBbHeight() * 0.5, entity_3.getZ(), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:electric_shock")), SoundSource.NEUTRAL, 5.0F, 1.0F, false);
                                            }
                                        }
                                        if (world instanceof ServerLevel _levelxxxxxx) {
                                            _levelxxxxxx.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_THUNDER_BLUE.get(), entity_3.getX(), entity_3.getY() + entity_3.getBbHeight() * 0.5, entity_3.getZ(), 50, 2.0, 2.0, 2.0, 1.0);
                                        }
                                    } else {
                                        entity_3.getPersistentData().putDouble("reach_action", 2.0);
                                        if (world instanceof Level _levelxxxxxx) {
                                            if (!_levelxxxxxx.isClientSide()) {
                                                _levelxxxxxx.playSound(null, BlockPos.containing(entity_3.getX(), entity_3.getY() + entity_3.getBbHeight() * 0.5, entity_3.getZ()), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("item.totem.use")), SoundSource.NEUTRAL, 5.0F, 1.0F);
                                            } else {
                                                _levelxxxxxx.playLocalSound(entity_3.getX(), entity_3.getY() + entity_3.getBbHeight() * 0.5, entity_3.getZ(), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("item.totem.use")), SoundSource.NEUTRAL, 5.0F, 1.0F, false);
                                            }
                                        }
                                        if (world instanceof ServerLevel _levelxxxxxxxx) {
                                            _levelxxxxxxxx.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity_3.getX(), entity_3.getY() + entity_3.getBbHeight() * 0.5, entity_3.getZ(), 50, 2.0, 2.0, 2.0, 1.0);
                                        }
                                    }
                                }
                            } else if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 3) {
                                if (entity_1 instanceof SlotEntity) {
                                    num2 = Math.round(entity_1 instanceof SlotEntity _datEntIxxxx ? ((Integer) _datEntIxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM)).intValue() : 0.0F);
                                }

                                if ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.UNLUCK) ? _livEnt.getEffect(MobEffects.UNLUCK).getAmplifier() : 0) >= 9) {
                                    num1 = num2 - 1.0;
                                    if (num2 < 1.0) {
                                        num1 = 7.0;
                                    }
                                } else if ((entity instanceof LivingEntity _livEntx && _livEntx.hasEffect(MobEffects.LUCK) ? _livEntx.getEffect(MobEffects.LUCK).getAmplifier() : 0) >= 9) {
                                    num1 = num2;
                                } else {
                                    int index1 = 0;
                                    while (true) {
                                        int luckAmp = entity instanceof LivingEntity _livEnt101 && _livEnt101.hasEffect(MobEffects.LUCK) ? (_livEnt101.getEffect(MobEffects.LUCK).getAmplifier() + 1) * 2 : 2;
                                        if (index1 >= luckAmp) {
                                            break;
                                        }
                                        num1 = Math.ceil(Math.random() * 7.0);
                                        if (num1 != num2 ? !(entity instanceof LivingEntity _livEnt103) || !_livEnt103.hasEffect(MobEffects.LUCK) : Math.random() < 0.15) {
                                            break;
                                        }
                                        index1++;
                                    }
                                }

                                animation = "slot" + Math.round(num1);
                                if (entity instanceof SlotEntity _datEntSetI) {
                                    _datEntSetI.getEntityData().set(SlotEntity.DATA_SLOT_NUM, (int) num1);
                                }
                            }

                            if (entity instanceof SlotEntity) {
                                PlayAnimationEntity2Procedure.execute(entity, animation);
                            }
                        }

                        if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 3 && entity.getPersistentData().getDouble("cnt1") == 80.0 / speed) {
                            num1 = 0.0;
                            SUCCESS = false;
                            if (entity_1 instanceof SlotEntity) {
                                num1 = entity_1 instanceof SlotEntity _datEntIxxxx ? ((Integer) _datEntIxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM)).intValue() : 0.0;
                                SUCCESS = true;
                            }

                            if (SUCCESS) {
                                SUCCESS = false;
                                if (entity_2 instanceof SlotEntity && num1 == (entity_2 instanceof SlotEntity _datEntIxxxx ? (Integer) _datEntIxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM) : 0)) {
                                    SUCCESS = true;
                                }
                            }

                            if (SUCCESS) {
                                SUCCESS = false;
                                if (entity_3 instanceof SlotEntity && num1 == (entity_3 instanceof SlotEntity _datEntIxxxx ? (Integer) _datEntIxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM) : 0)) {
                                    SUCCESS = true;
                                }
                            }

                            if (SUCCESS) {
                                if (world instanceof Level _levelxxxxxxxx) {
                                    if (!_levelxxxxxxxx.isClientSide()) {
                                        _levelxxxxxxxx.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:hakari1")), SoundSource.NEUTRAL, 5.0F, 1.0F);
                                    } else {
                                        _levelxxxxxxxx.playLocalSound(x_pos, y_pos, z_pos, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:hakari1")), SoundSource.NEUTRAL, 5.0F, 1.0F, false);
                                    }
                                }

                                entity_1.getPersistentData().putDouble("cnt2", 1.0);
                                entity_2.getPersistentData().putDouble("cnt2", 1.0);
                                entity_3.getPersistentData().putDouble("cnt2", 1.0);
                            }
                        }

                        if (entity.getPersistentData().getDouble("cnt2") > 0.0) {
                            entity.getPersistentData().putDouble("cnt2", entity.getPersistentData().getDouble("cnt2") + 1.0);
                            if (world instanceof ServerLevel _levelxxxxxxxxx) {
                                _levelxxxxxxxxx.sendParticles(ParticleTypes.END_ROD, x_pos, y_pos, z_pos, 10, 1.0, 1.0, 1.0, 0.5);
                                _levelxxxxxxxxx.sendParticles(ParticleTypes.FIREWORK, x_pos, y_pos, z_pos, 1, 1.0, 1.0, 1.0, 0.5);
                            }

                            if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 3 && entity_a instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                                _entity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get(), 1200, entity_a instanceof LivingEntity _livEntx && _livEntx.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) ? _livEntx.getEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier() : 0, true, true));
                            }

                            if (Math.random() < 0.05) {
                                range = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius * 2.0;
                                num1 = Math.toRadians(Math.random() * 360.0);
                                num2 = Math.random() * range - range / 2.0;
                                x_pos = entity_a.getPersistentData().getDouble("x_pos_doma") + Math.sin(num1) * num2;
                                y_pos = entity_a.getPersistentData().getDouble("y_pos_doma") + 5.0;
                                z_pos = entity_a.getPersistentData().getDouble("z_pos_doma") + Math.cos(num1) * num2;
                                if (world instanceof ServerLevel _levelxxxxxxxxx) {
                                    int fireworksColor;
                                    double randColor = Math.random();
                                    if (randColor < 0.25) fireworksColor = 16711680;
                                    else if (randColor < 0.5) fireworksColor = 16774912;
                                    else if (randColor < 0.75) fireworksColor = 43775;
                                    else fireworksColor = 65442;

                                    _levelxxxxxxxxx.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _levelxxxxxxxxx, 4, "", Component.literal(""), _levelxxxxxxxxx.getServer(), null).withSuppressedOutput(), "summon firework_rocket ~ ~ ~ {LifeTime:10,FireworksItem:{id:\"firework_rocket\",Count:1,tag:{Fireworks:{Explosions:[{Type:" + Math.round(Math.random() * 4.0) + ",Flicker:0b,Trail:0b,Colors:[I;" + fireworksColor + "],FadeColors:[I;" + fireworksColor + "]}]}}}}");
                                }
                            }

                            if (entity.getPersistentData().getDouble("cnt2") >= 50.0) {
                                if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 3) {
                                    if ((entity instanceof SlotEntity _datEntIxxxx ? (Integer) _datEntIxxxx.getEntityData().get(SlotEntity.DATA_SLOT_NUM) : 0) % 2 == 1) {
                                        entity_a.getPersistentData().putDouble("mode_hakari", 2.0);
                                    } else if (Math.random() < 0.5) {
                                        entity_a.getPersistentData().putDouble("mode_hakari", 1.0);
                                    } else {
                                        entity_a.getPersistentData().putDouble("mode_hakari", 40.0);
                                    }

                                    if (entity_a instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                                        // JJKUR Custom Jackpot logic
                                        _entity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.JACKPOT.get(), 5020, 0, true, true));
                                        if ((entity_a instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(MobEffects.LUCK) ? _livEnt2.getEffect(MobEffects.LUCK).getAmplifier() : 0) == 4) {
                                            _entity.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.GOJO_AWAKENING.get(), 5020, 0, true, true));
                                        }
                                        _entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 5020, (entity_a instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(MobEffects.LUCK) ? _livEnt2.getEffect(MobEffects.LUCK).getAmplifier() : 0) + 1, false, false));
                                        _entity.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.JACKPOT.get(), 20, 0, true, true));
                                    }

                                    if (entity_a instanceof LivingEntity _entity) {
                                        _entity.removeEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                                    }
                                }

                                if (!entity.level().isClientSide()) {
                                    entity.discard();
                                }
                            }
                        } else if (entity.getPersistentData().getDouble("cnt1") >= Math.max(80.0 / speed, 50.0)) {
                            if ((entity instanceof LivingEntity _livEntx && _livEntx.hasEffect(MobEffects.UNLUCK) ? _livEntx.getEffect(MobEffects.UNLUCK).getAmplifier() : 0) >= 9 && Math.random() > 0.0) {
                                if (entity.getPersistentData().getDouble("cnt1") >= Math.max(100.0 / speed, 50.0)) {
                                    if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 3 && entity_a.isAlive()) {
                                        if (world instanceof Level _levelxxxxxxxxx) {
                                            if (!_levelxxxxxxxxx.isClientSide()) {
                                                _levelxxxxxxxxx.playSound(null, BlockPos.containing(entity_a.getX(), entity_a.getY() + entity_a.getBbHeight() * 0.5, entity_a.getZ()), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:glass_crash")), SoundSource.NEUTRAL, 1.0F, 1.0F);
                                            } else {
                                                _levelxxxxxxxxx.playLocalSound(entity_a.getX(), entity_a.getY() + entity_a.getBbHeight() * 0.5, entity_a.getZ(), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:glass_crash")), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
                                            }
                                        }

                                        if (world instanceof ServerLevel _levelxxxxxxxxxx) {
                                            _levelxxxxxxxxxx.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_BROKEN_GLASS.get(), entity_a.getX(), entity_a.getY() + entity_a.getBbHeight() * 0.5, entity_a.getZ(), 50, 2.0, 2.0, 2.0, 0.25);
                                            _levelxxxxxxxxxx.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_BROKEN_GLASS_SMALL.get(), entity_a.getX(), entity_a.getY() + entity_a.getBbHeight() * 0.5, entity_a.getZ(), 50, 1.0, 1.0, 1.0, 0.25);
                                        }

                                        if (entity_a instanceof LivingEntity _entity) {
                                            _entity.setHealth((float) ((entity_a instanceof LivingEntity _livEntxxx ? _livEntxxx.getHealth() : -1.0F) + (entity_a instanceof LivingEntity _livEntxx ? _livEntxx.getMaxHealth() : -1.0F) * 0.5));
                                        }
                                    }

                                    if (world instanceof ServerLevel _levelxxxxxxxxxx) {
                                        _levelxxxxxxxxxx.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_BROKEN_GLASS.get(), x_pos, y_pos, z_pos, 20, 1.0, 2.0, 1.0, 0.25);
                                        _levelxxxxxxxxxx.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_BROKEN_GLASS_SMALL.get(), x_pos, y_pos, z_pos, 40, 1.0, 2.0, 1.0, 0.25);
                                    }

                                    x_pos = entity.getX();
                                    y_pos = entity.getY();
                                    z_pos = entity.getZ();
                                    if (world instanceof Level _levelxxxxxxxxxx) {
                                        if (!_levelxxxxxxxxxx.isClientSide()) {
                                            _levelxxxxxxxxxx.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:glass_crash")), SoundSource.NEUTRAL, 5.0F, 1.0F);
                                        } else {
                                            _levelxxxxxxxxxx.playLocalSound(x_pos, y_pos, z_pos, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:glass_crash")), SoundSource.NEUTRAL, 5.0F, 1.0F, false);
                                        }
                                    }

                                    if (world instanceof Level _levelxxxxxxxxxxx) {
                                        if (!_levelxxxxxxxxxxx.isClientSide()) {
                                            _levelxxxxxxxxxxx.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:gacon")), SoundSource.NEUTRAL, 5.0F, 1.0F);
                                        } else {
                                            _levelxxxxxxxxxxx.playLocalSound(x_pos, y_pos, z_pos, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:gacon")), SoundSource.NEUTRAL, 5.0F, 1.0F, false);
                                        }
                                    }

                                    if (world instanceof ServerLevel _serverLevel) {
                                        Entity entityinstance = ((EntityType) JujutsucraftModEntities.SLOT.get()).create(_serverLevel, null, null, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                                        if (entityinstance != null) {
                                            entityinstance.setYRot(world.getRandom().nextFloat() * 360.0F);
                                            if (!entityinstance.level().isClientSide() && entityinstance.getServer() != null) {
                                                entityinstance.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entityinstance.position(), entityinstance.getRotationVector(), entityinstance.level() instanceof ServerLevel ? (ServerLevel) entityinstance.level() : null, 4, entityinstance.getName().getString(), entityinstance.getDisplayName(), entityinstance.level().getServer(), entityinstance), "data merge entity @s {NoAI:1b,Invulnerable:1b}");
                                            }

                                            entityinstance.setYRot(entity.getYRot());
                                            entityinstance.setXRot(0.0F);
                                            entityinstance.setYBodyRot(entityinstance.getYRot());
                                            entityinstance.setYHeadRot(entityinstance.getYRot());
                                            entityinstance.yRotO = entityinstance.getYRot();
                                            entityinstance.xRotO = entityinstance.getXRot();
                                            if (entityinstance instanceof LivingEntity _entity) {
                                                _entity.yBodyRotO = _entity.getYRot();
                                                _entity.yHeadRotO = _entity.getYRot();
                                            }

                                            entityinstance.getPersistentData().putDouble("NameRanged_ranged", entity.getPersistentData().getDouble("NameRanged_ranged"));
                                            entityinstance.getPersistentData().putDouble("cnt1", -20.0);
                                            entityinstance.getPersistentData().putString("OWNER_UUID", entity.getPersistentData().getString("OWNER_UUID"));
                                            if (entityinstance instanceof SlotEntity _datEntSetI) {
                                                _datEntSetI.getEntityData().set(SlotEntity.DATA_SLOT_MODE, entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0);
                                            }

                                            entity_a.getPersistentData().putString("SLOT" + Math.round(entity instanceof SlotEntity _datEntIxx ? ((Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE)).intValue() : 0.0F), entityinstance.getStringUUID());
                                            if ((entity instanceof LivingEntity _livEntxx && _livEntxx.hasEffect(MobEffects.UNLUCK) ? _livEntxx.getEffect(MobEffects.UNLUCK).getAmplifier() : 0) > 9) {
                                                if (entityinstance instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                                                    _entity.addEffect(new MobEffectInstance(MobEffects.UNLUCK, Integer.MAX_VALUE, (entity instanceof LivingEntity _livEntxxx && _livEntxxx.hasEffect(MobEffects.UNLUCK) ? _livEntxxx.getEffect(MobEffects.UNLUCK).getAmplifier() : 0) - 1, false, false));
                                                }
                                            } else if (entityinstance instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                                                _entity.addEffect(new MobEffectInstance(MobEffects.LUCK, Integer.MAX_VALUE, 29, false, false));
                                            }

                                            _serverLevel.addFreshEntity(entityinstance);
                                        }
                                    }

                                    if (!entity.level().isClientSide()) {
                                        entity.discard();
                                    }
                                }
                            } else {
                                if ((entity instanceof SlotEntity _datEntIxx ? (Integer) _datEntIxx.getEntityData().get(SlotEntity.DATA_SLOT_MODE) : 0) == 3 && entity_a.getPersistentData().getDouble("mode_hakari") > 10.0) {
                                    entity_a.getPersistentData().putDouble("mode_hakari", entity_a.getPersistentData().getDouble("mode_hakari") - 1.0);
                                    if (entity_a.getPersistentData().getDouble("mode_hakari") <= 10.0) {
                                        entity_a.getPersistentData().putDouble("mode_hakari", 0.0);
                                    }
                                }

                                if (!entity.level().isClientSide()) {
                                    entity.discard();
                                }
                            }
                        }
                    }
                } else if (!entity.level().isClientSide()) {
                    entity.discard();
                }
            } else if (!entity.level().isClientSide()) {
                entity.discard();
            }
        }
    }
}
