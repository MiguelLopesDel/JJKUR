package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.DomainActiveProcedure;
import net.mcreator.jujutsucraft.procedures.DomainExpansionBattleProcedure;
import net.mcreator.jujutsucraft.procedures.DomainExpansionOnEffectActiveTickProcedure;
import net.mcreator.jujutsucraft.procedures.EffectCharactorProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

@Mixin(value = DomainExpansionOnEffectActiveTickProcedure.class, priority = -10000)
public abstract class DomainExpansionOnEffectActiveTickProcedureMixin {

    /**
     * @author Satushi
     * @reason Fixes Domain Barrier Size and Updates for v43 with Safety Audit Fixes
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity != null) {
            // JJKU_DOMAIN_NERF logic
            if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_DOMAIN_NERF)) {
                if (entity instanceof LivingEntity _livEnt && !_livEnt.hasEffect(JujutsucraftaddonModMobEffects.DOMAIN_BREAK.get())) {
                    if (!_livEnt.level().isClientSide())
                        _livEnt.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.DOMAIN_BREAK.get(), -1, 1, false, false));
                }
            }

            double range = 0.0;
            double level = 0.0;
            double tick = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double tick_1 = 0.0;
            double tick_2 = 0.0;
            double distance = 0.0;
            double old_skill = 0.0;
            double domainPower1 = 0.0;
            double domainPower2 = 0.0;
            double str_lv = 0.0;
            double x_dis = 0.0;
            double y_dis = 0.0;
            double z_dis = 0.0;
            boolean failed = false;
            boolean logic_a = false;
            boolean logic_b = false;
            boolean noClosing = false;
            boolean update1 = false;
            boolean old_failed = false;
            boolean use_old = false;

            if (entity.isAlive()) {
                tick_1 = entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                        ? _livEnt.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getDuration()
                        : 0.0;
                
                str_lv = (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.DAMAGE_BOOST)
                        ? _livEnt.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier()
                        : 0) + 10;
                
                if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && _livEnt.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier() > 0) {
                    str_lv *= 1.15;
                }

                if (entity.getPersistentData().getDouble("select") == 27.0 || entity.getPersistentData().getDouble("skill_domain") == 27.0) {
                    str_lv *= 1.5;
                } else if (entity.getPersistentData().getDouble("select") == 29.0 || entity.getPersistentData().getDouble("skill_domain") == 29.0) {
                    str_lv *= 2.0;
                }

                if (entity.getPersistentData().getDouble("skill_domain") == 0.0 && entity.getPersistentData().getDouble("skill") == 0.0) {
                    entity.getPersistentData().putDouble("skill_domain", entity.getPersistentData().getDouble("select"));
                    entity.getPersistentData().putDouble("select", 0.0);
                    update1 = true;
                }

                JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());

                if (entity.getPersistentData().getDouble("skill_domain") > 0.0) {
                    if (!update1) {
                        DomainActiveProcedure.execute(world, x, y, z, entity);
                    }

                    double baseRadius;
                    if (addonVars.BarrierlessDomain) {
                        baseRadius = addonVars.RadiusDomain;
                    } else if (addonVars.DomainType == 1) {
                        baseRadius = 12.0;
                    } else {
                        baseRadius = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius;
                    }

                    int amp = (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()))
                            ? _livEnt.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier() : 0;
                    
                    range = baseRadius * (amp > 0 ? 18.0 : 2.0);
                }

                old_skill = entity.getPersistentData().getDouble("skill");
                entity.getPersistentData().putDouble("skill", 0.0);

                if (tick_1 % 10.0 == 0.0 || update1) {
                    logic_a = false;
                    Vec3 _center = new Vec3(entity.getPersistentData().getDouble("x_pos_doma"), entity.getPersistentData().getDouble("y_pos_doma"), entity.getPersistentData().getDouble("z_pos_doma"));
                    
                    List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(range / 2.0), e -> true).stream()
                            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();

                    for (Entity entityiterator : _entfound) {
                        x_dis = entity.getPersistentData().getDouble("x_pos_doma") - entityiterator.getX();
                        y_dis = entity.getPersistentData().getDouble("y_pos_doma") - (entityiterator.getY() + entityiterator.getBbHeight() * 0.5);
                        z_dis = entity.getPersistentData().getDouble("z_pos_doma") - entityiterator.getZ();
                        distance = x_dis * x_dis + y_dis * y_dis + z_dis * z_dis;

                        if (distance < (range * 0.5 * range * 0.5)) {
                            if (entity == entityiterator) {
                                logic_a = true;
                            } else {
                                if (entityiterator instanceof LivingEntity _livEntIt && _livEntIt.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                                    level = _livEntIt.getEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getAmplifier();
                                    tick = _livEntIt.getEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getDuration();
                                    tick -= Math.round(Math.sqrt(str_lv + 1.0) * 10.0);
                                    
                                    _livEntIt.removeEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
                                    if (level >= 0.0 && !_livEntIt.level().isClientSide()) {
                                        _livEntIt.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), (int) tick, (int) level, true, true));
                                    }
                                } else if (tick_1 % 20.0 == 0.0) {
                                    EffectCharactorProcedure.execute(world, entity, entityiterator);
                                }

                                if (entityiterator instanceof LivingEntity _livEntIt && !_livEntIt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                                        && entityiterator.getPersistentData().getDouble("select") == 0.0 && !_livEntIt.level().isClientSide()) {
                                    _livEntIt.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.NEUTRALIZATION.get(), 20, (int) (entity.getPersistentData().getDouble("skill_domain") + 10.0), false, false));
                                }
                            }
                        }
                    }

                    if (!logic_a && entity.getPersistentData().getDouble("skill_domain") > 0.0) {
                        int amp = (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()))
                                ? _livEnt.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier() : 0;
                        if (amp == 0 && entity instanceof LivingEntity _livEnt) {
                            _livEnt.removeEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                        }
                    }
                }

                double oldHealth = entity.getPersistentData().getDouble("oldHealth");
                float currentHealth = entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1.0F;
                
                if (oldHealth != (double) currentHealth) {
                    if (oldHealth > (double) currentHealth) {
                        entity.getPersistentData().putDouble("totalDamage", entity.getPersistentData().getDouble("totalDamage") + (oldHealth - (double) currentHealth));
                    } else {
                        entity.getPersistentData().putDouble("totalDamage", entity.getPersistentData().getDouble("totalDamage") + (oldHealth - (double) currentHealth) * 0.5);
                    }
                }
                entity.getPersistentData().putDouble("oldHealth", (double) currentHealth);

                if ((tick_1 % 20.0 == 0.0 || update1) && entity.getPersistentData().getDouble("select") == 0.0) {
                    float maxHealth = entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F;
                    double healthRatio = Math.max((double) Math.max(maxHealth, 1.0F) - Math.max(entity.getPersistentData().getDouble("totalDamage"), 0.0) * 2.0, 0.0) / (double) Math.max(maxHealth, 1.0F);
                    domainPower1 = str_lv * healthRatio * Math.min(Math.min(tick_1, 1200.0) / 2400.0 + 0.5, 1.0);
                    
                    failed = false;
                    logic_a = false;
                    logic_b = false;

                    for (int index0 = 0; index0 < 2; index0++) {
                        Vec3 _center = new Vec3(entity.getPersistentData().getDouble("x_pos_doma"), entity.getPersistentData().getDouble("y_pos_doma"), entity.getPersistentData().getDouble("z_pos_doma"));
                        
                        List<Entity> _clashEnts = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(range / 2.0), e -> true).stream()
                                .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();

                        for (Entity entityiterator : _clashEnts) {
                            int myAmp = (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) ? _livEnt.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier() : 0;
                            int targetAmp = (entityiterator instanceof LivingEntity _livEntIt && _livEntIt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) ? _livEntIt.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier() : 0;
                            
                            noClosing = (myAmp > 0 && targetAmp == 0);
                            
                            if (entity != entityiterator && (entityiterator instanceof LivingEntity _livEntIt && _livEntIt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) || entityiterator.getPersistentData().getDouble("select") != 0.0)) {
                                x_dis = entity.getPersistentData().getDouble("x_pos_doma") - entityiterator.getX();
                                y_dis = entity.getPersistentData().getDouble("y_pos_doma") - (entityiterator.getY() + entityiterator.getBbHeight() * 0.5);
                                z_dis = entity.getPersistentData().getDouble("z_pos_doma") - entityiterator.getZ();
                                distance = x_dis * x_dis + y_dis * y_dis + z_dis * z_dis;

                                if (distance < (range * 0.5 * range * 0.5)) {
                                    domainPower2 = (entityiterator instanceof LivingEntity _livEntIt && _livEntIt.hasEffect(MobEffects.DAMAGE_BOOST) ? _livEntIt.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0) + 10;
                                    if (targetAmp > 0) domainPower2 *= 1.15;
                                    
                                    if (entityiterator.getPersistentData().getDouble("select") == 27.0 || entityiterator.getPersistentData().getDouble("skill_domain") == 27.0) {
                                        domainPower2 *= 1.5;
                                    } else if (entityiterator.getPersistentData().getDouble("select") == 29.0 || entityiterator.getPersistentData().getDouble("skill_domain") == 29.0) {
                                        domainPower2 *= 2.0;
                                    }

                                    if (entityiterator.getPersistentData().getDouble("select") != 0.0) {
                                        tick_2 = 1200.0;
                                    } else {
                                        tick_2 = (entityiterator instanceof LivingEntity _livEntIt && _livEntIt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) ? _livEntIt.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getDuration() : 0.0;
                                        float tMaxHealth = entityiterator instanceof LivingEntity _livEntIt ? _livEntIt.getMaxHealth() : -1.0F;
                                        double tHealthRatio = Math.max((double) Math.max(tMaxHealth, 1.0F) - Math.max(entityiterator.getPersistentData().getDouble("totalDamage"), 0.0) * 2.0, 0.0) / (double) Math.max(tMaxHealth, 1.0F);
                                        domainPower2 = domainPower2 * tHealthRatio * Math.min(Math.min(tick_2, 1200.0) / 2400.0 + 0.5, 1.0);
                                    }

                                    if ((domainPower1 - domainPower2 >= 10.0 || (logic_a && targetAmp <= 0)) && entityiterator instanceof LivingEntity _livEntIt && _livEntIt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                        failed = false;
                                        logic_b = true;
                                        entityiterator.getPersistentData().putBoolean("Failed", false);
                                        entityiterator.getPersistentData().putBoolean("DomainDefeated", !noClosing);
                                        
                                        if (!_livEntIt.level().isClientSide()) {
                                            _livEntIt.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get(), 5, 0, false, false));
                                        }
                                        _livEntIt.removeEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                                    } else {
                                        if (noClosing && (tick_1 <= tick_2 ? tick_2 < 1000.0 : tick_1 < 1000.0) && entityiterator instanceof LivingEntity _livEntIt && _livEntIt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                            logic_a = true;
                                            boolean canLearn = false;
                                            if (entityiterator instanceof ServerPlayer _player) {
                                                Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:anti_open_barrier_type_domain"));
                                                if (_adv != null) {
                                                    AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
                                                    if (!_ap.isDone()) {
                                                        if (!(entityiterator instanceof GojoSatoruEntity || entityiterator instanceof HigurumaHiromiEntity || entityiterator instanceof OkkotsuYutaEntity || entityiterator instanceof OkkotsuYutaCullingGameEntity || entityiterator instanceof KenjakuEntity)) {
                                                            for (String criteria : _ap.getRemainingCriteria()) {
                                                                _player.getAdvancements().award(_adv, criteria);
                                                            }
                                                        } else {
                                                            entityiterator.getPersistentData().putDouble("cnt_learn_domain", 1.0);
                                                        }
                                                        canLearn = true;
                                                    }
                                                }
                                            } else if (entityiterator.getPersistentData().getDouble("cnt_learn_domain") == 0.0 && !(entityiterator instanceof LivingEntity _livEntIt2 && _livEntIt2.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get()))) {
                                                entityiterator.getPersistentData().putDouble("cnt_learn_domain", 1.0);
                                                canLearn = true;
                                            }
                                            
                                            if (canLearn) {
                                                logic_a = true;
                                            } else {
                                                logic_a = false;
                                            }

                                            if (logic_a) break;
                                        }

                                        if (!logic_b) {
                                            double currentRadius;
                                            if (addonVars.BarrierlessDomain) {
                                                currentRadius = addonVars.RadiusDomain;
                                            } else if (addonVars.DomainType == 1) {
                                                currentRadius = 12.0;
                                            } else {
                                                currentRadius = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius;
                                            }

                                            if (distance < currentRadius * currentRadius) {
                                                entity.getPersistentData().putBoolean("Failed", true);
                                            } else {
                                                use_old = true;
                                                old_failed = entity.getPersistentData().getBoolean("Failed");
                                                entity.getPersistentData().putBoolean("Failed", true);
                                            }
                                            failed = true;
                                        }
                                    }
                                }
                            }
                        }
                        if (!logic_a) break;
                    }
                } else {
                    failed = true;
                }

                entity.getPersistentData().putDouble("skill", old_skill);
                
                if (!entity.getPersistentData().getBoolean("Failed") && !entity.getPersistentData().getBoolean("Cover")) {
                    int zoneDuration = (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(JujutsucraftModMobEffects.ZONE.get())) ? _livEnt.getEffect(JujutsucraftModMobEffects.ZONE.get()).getDuration() : 0;
                    if (zoneDuration < 10 && entity instanceof LivingEntity _livEnt && !_livEnt.level().isClientSide()) {
                        _livEnt.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.ZONE.get(), 10, 0, false, false));
                    }
                } else {
                    if (!failed && !entity.getPersistentData().getBoolean("Cover")) {
                        x_dis = entity.getPersistentData().getDouble("x_pos_doma") - entity.getX();
                        y_dis = entity.getPersistentData().getDouble("y_pos_doma") - (entity.getY() + entity.getBbHeight() * 0.5);
                        z_dis = x_dis * x_dis + y_dis * y_dis + (entity.getPersistentData().getDouble("z_pos_doma") - entity.getZ()) * (entity.getPersistentData().getDouble("z_pos_doma") - entity.getZ());
                        
                        double currentRadius;
                        if (addonVars.BarrierlessDomain) {
                            currentRadius = addonVars.RadiusDomain;
                        } else if (addonVars.DomainType == 1) {
                            currentRadius = 12.0;
                        } else {
                            currentRadius = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius;
                        }

                        int myAmp = (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) ? _livEnt.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier() : 0;

                        if (z_dis < currentRadius * currentRadius || myAmp != 0) {
                            entity.getPersistentData().putDouble("cnt_cover", 1.0);
                            old_failed = false;
                            entity.getPersistentData().putBoolean("Failed", false);
                            entity.getPersistentData().putBoolean("Cover", true);
                            if (entity instanceof LivingEntity _livEnt && !_livEnt.level().isClientSide()) {
                                _livEnt.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get(), (int) (tick_1 + 100.0), myAmp, true, false));
                            }
                            
                            // Restore Raytrace
                            Vec3 _eyePos = entity.getEyePosition(1.0F);
                            Vec3 _viewVec = entity.getViewVector(1.0F).scale(0.0);
                            BlockPos _bpos = entity.level().clip(new ClipContext(_eyePos, _eyePos.add(_viewVec), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity)).getBlockPos();
                            
                            entity.getPersistentData().putDouble("x_pos_doma2", Math.round(_bpos.getX()));
                            entity.getPersistentData().putDouble("y_pos_doma2", Math.round(_bpos.getY()));
                            entity.getPersistentData().putDouble("z_pos_doma2", Math.round(_bpos.getZ()));
                        } else if (entity instanceof LivingEntity _livEnt) {
                            _livEnt.removeEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                        }
                    }

                    if (entity.getPersistentData().getBoolean("Cover")) {
                        x_pos = entity.getPersistentData().getDouble("x_pos_doma");
                        y_pos = entity.getPersistentData().getDouble("y_pos_doma");
                        z_pos = entity.getPersistentData().getDouble("z_pos_doma");
                    } else {
                        x_pos = entity.getX();
                        y_pos = entity.getY();
                        z_pos = entity.getZ();
                    }

                    old_skill = entity.getPersistentData().getDouble("cnt1");
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    DomainExpansionBattleProcedure.execute(world, x_pos, y_pos, z_pos, entity);
                    entity.getPersistentData().putDouble("cnt1", old_skill);
                    
                    if (use_old) {
                        entity.getPersistentData().putBoolean("Failed", old_failed);
                    }

                    if (entity.getPersistentData().getBoolean("Cover")) {
                        entity.getPersistentData().putDouble("cnt_cover", entity.getPersistentData().getDouble("cnt_cover") + 1.0);
                        double currentRadius;
                        if (addonVars.BarrierlessDomain) {
                            currentRadius = addonVars.RadiusDomain;
                        } else if (addonVars.DomainType == 1) {
                            currentRadius = 12.0;
                        } else {
                            currentRadius = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius;
                        }
                        if (entity.getPersistentData().getDouble("cnt_cover") > currentRadius * 2.0 + 1.0) {
                            entity.getPersistentData().putBoolean("Cover", false);
                        }
                    }
                }

                if (!entity.getPersistentData().getBoolean("Cover")) {
                    if (entity instanceof Player) {
                        if (tick_1 % 20.0 == 0.0) {
                            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                                capability.PlayerCursePowerChange -= 20.0;
                                if (capability.PlayerCursePower + capability.PlayerCursePowerChange <= 0.0 && entity instanceof LivingEntity _livEnt) {
                                    _livEnt.removeEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                                }
                                capability.syncPlayerVariables(entity);
                            });
                        }
                    } else if (entity.getPersistentData().getDouble("cnt_target") > 5.0) {
                        entity.getPersistentData().putDouble("cnt_domain_cancel", 0.0);
                    } else {
                        entity.getPersistentData().putDouble("cnt_domain_cancel", entity.getPersistentData().getDouble("cnt_domain_cancel") + 1.0);
                        int cancelLimit = (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) ? 600 : 100;
                        if (entity.getPersistentData().getDouble("cnt_domain_cancel") > (double) cancelLimit && entity instanceof LivingEntity _livEnt) {
                            _livEnt.removeEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                        }
                    }
                }
            } else if (entity instanceof LivingEntity _livEnt) {
                _livEnt.removeEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            }
        }
    }
}
