package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.ErrorEntity;
import com.jujutsu.jujutsucraftaddon.entity.ItadoriShinjukuEntity;
import com.jujutsu.jujutsucraftaddon.entity.SukunaMangaEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.procedures.TenShadowsRegisterProcedure;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AISukunaProcedure.class, priority = -10000)
public abstract class SukunaAIBuffMixin {

    /**
     * @author Satushi / FULL RESTORATION
     * @reason Refactored for v43. Restored missing Meteor Passenger logic, effect clearing, and exact probabilities.
     */
    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity.isAlive() && entity instanceof LivingEntity _livEntity) {
            ItemStack item_head = _livEntity.getItemBySlot(EquipmentSlot.HEAD).copy();
            double rnd = 0.0;
            double tick = 0.0;
            double distance = 0.0;
            boolean domain = false;
            boolean infinity = false;
            boolean fushiguro_body = false;
            boolean mahoraga_exist = false;
            boolean vsMahoraga = false;
            boolean flag_megumiTechnique = false;
            boolean gojosatoru = false;

            // 1. Water Walking Logic
            BlockPos belowPos = _livEntity.blockPosition().below();
            if (world.getBlockState(BlockPos.containing(_livEntity.getX(), _livEntity.getY() - 1, _livEntity.getZ())).getBlock() instanceof LiquidBlock && !_livEntity.isInWater()) {
                if (_livEntity.getDeltaMovement().y() <= 0) {
                    _livEntity.setDeltaMovement(_livEntity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                    _livEntity.setOnGround(true);
                    _livEntity.setPos(_livEntity.getX(), belowPos.getY() + 1.0, _livEntity.getZ());
                }
            }

            AIActiveProcedure.execute(world, x, y, z, _livEntity);
            fushiguro_body = _livEntity instanceof SukunaFushiguroEntity || _livEntity instanceof SukunaMangaEntity || _livEntity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity;

            // 2. Armor Strip Logic
            if (_livEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() == JujutsucraftModItems.UNIFORM_NORMAL_CHESTPLATE.get()) {
                if (Math.random() < 1.0 / 3600.0) _livEntity.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            }
            if (_livEntity.getItemBySlot(EquipmentSlot.HEAD).getItem() == JujutsucraftModItems.HAIR_FUSHIGURO_MEGUMI_HELMET.get()) {
                if (Math.random() < 1.0 / 1200.0) _livEntity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }

            // 3. Ten Shadows Initialization
            if (fushiguro_body && !_livEntity.getPersistentData().getBoolean("flag_start")) {
                _livEntity.getPersistentData().putBoolean("flag_start", true);
                TenShadowsRegisterProcedure.execute(_livEntity);
                for (int i = 4; i <= 10; i++) _livEntity.getPersistentData().putDouble("TenShadowsTechnique" + i, 1.0);
                _livEntity.getPersistentData().putDouble("TenShadowsTechnique1", -2.0);
                _livEntity.getPersistentData().putDouble("TenShadowsTechnique5", -2.0);
                _livEntity.getPersistentData().putDouble("TenShadowsTechnique14", 1.0);
            }

            // 4. Buffs Logic
            if (!_livEntity.hasEffect(MobEffects.DAMAGE_BOOST)) {
                if (_livEntity instanceof SukunaPerfectEntity || _livEntity instanceof ErrorEntity) {
                    rnd = 35.0;
                    _livEntity.getPersistentData().putDouble("KnockbackFix", 1.0);
                } else if (fushiguro_body && !(_livEntity instanceof SukunaFushiguroEntity _sf && (Boolean) _sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                    rnd = 30.0;
                } else {
                    rnd = 25.0;
                }
                if (!_livEntity.level().isClientSide()) {
                    _livEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, (int) Math.round(rnd), false, false));
                    if (!(_livEntity instanceof ItadoriShinjukuEntity)) {
                        int sukunaAmp = (int) Math.round(Math.min(Math.max(rnd - 11.0, 0.0), 19.0));
                        _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get(), Integer.MAX_VALUE, sukunaAmp, false, false));
                    }
                }
            }
            if (!_livEntity.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !_livEntity.level().isClientSide()) {
                _livEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false));
            }

            LivingEntity target = (_livEntity instanceof Mob _mob) ? _mob.getTarget() : null;
            if (target != null) {
                if (target instanceof GojoSatoruEntity || target instanceof GojoSatoruSchoolDaysEntity || target.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables()).PlayerCurseTechnique == 2) {
                    gojosatoru = true;
                }
            }

            // 5. Binding Vow
            if (_livEntity instanceof SukunaFushiguroEntity && gojosatoru && !_livEntity.hasEffect(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get())) {
                if (!_livEntity.level().isClientSide()) {
                    _livEntity.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get(), 3000, 0, false, false));
                }
            }

            // 6. Jogo Meteor Logic
            if (_livEntity instanceof SukunaEntity && target instanceof JogoEntity && _livEntity.getPersistentData().getDouble("cnt_target") > 6.0) {
                if (target.getPersistentData().getDouble("skill") == 415.0) {
                    _livEntity.getPersistentData().putBoolean("flag2", true);
                    if (!_livEntity.level().isClientSide()) {
                        _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 10, 0, false, false));
                        _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 10, 0, false, false));
                    }
                } else if (_livEntity.getPersistentData().getBoolean("flag2")) {
                    distance = GetDistanceProcedure.execute(_livEntity);
                    if (distance < 32.0) {
                        if (distance < 6.0 && _livEntity.onGround()) {
                            _livEntity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                            _livEntity.getPersistentData().putBoolean("PRESS_S", true);
                            WhenBackStepProcedure.execute(world, _livEntity);
                            _livEntity.getPersistentData().putBoolean("PRESS_S", false);
                        }
                        
                        _livEntity.removeEffect(MobEffects.MOVEMENT_SPEED);
                        _livEntity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        if (!_livEntity.level().isClientSide()) {
                            _livEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
                            _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 10, 0, false, false));
                        }

                        target.removeEffect(MobEffects.MOVEMENT_SPEED);
                        target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        if (!target.level().isClientSide()) {
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
                            target.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 10, 0, false, false));
                        }
                        if (target.onGround() && (target.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get()) ? target.getEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get()).getDuration() : 0) < 200) {
                            target.removeEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get());
                        }
                    }

                    if (target.isPassenger() && target.getVehicle() instanceof MeteorEntity) {
                        if (!_livEntity.level().isClientSide()) {
                            _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 10, 0, false, false));
                            _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 10, 0, false, false));
                        }
                    }
                }
            }

            // 7. Heian Transformation
            if (target != null && _livEntity.getPersistentData().getDouble("cnt_target") > 6.0) {
                if (fushiguro_body && !(_livEntity instanceof SukunaFushiguroEntity _sf && (Boolean) _sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                    if (!(target instanceof GojoSatoruEntity) && !(target instanceof PurpleEntity)) {
                        if (_livEntity.getHealth() <= _livEntity.getMaxHealth() * 0.3 && !target.hasEffect(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get())) {
                            if (_livEntity instanceof SukunaFushiguroEntity animatable) {
                                animatable.getEntityData().set(SukunaFushiguroEntity.DATA_perfect_mode, true);
                                animatable.setTexture("sukuna_perfect");
                                
                                animatable.setYBodyRot(animatable.getYRot());
                                animatable.setYHeadRot(animatable.getYRot());
                                animatable.yRotO = animatable.getYRot();
                                animatable.xRotO = animatable.getXRot();
                                animatable.yBodyRotO = animatable.getYRot();
                                animatable.yHeadRotO = animatable.getYRot();

                                PlayAnimationEntity2Procedure.execute(animatable, "heianform");
                                if (!animatable.level().isClientSide()) {
                                    animatable.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.ANIMATION_HEIAN.get(), 40, 1, false, false));
                                    animatable.addEffect(new MobEffectInstance(MobEffects.HEAL, 10, 10, false, false));
                                    
                                    CommandSourceStack stack = new CommandSourceStack(CommandSource.NULL, animatable.position(), animatable.getRotationVector(), (ServerLevel) animatable.level(), 4, animatable.getName().getString(), animatable.getDisplayName(), animatable.level().getServer(), animatable);
                                    animatable.getServer().getCommands().performPrefixedCommand(stack, "particle jjkueffects:red_awakening_2 ~ ~-1 ~ 0 0 0 1 1 force");
                                    animatable.getServer().getCommands().performPrefixedCommand(stack, "item replace entity @s weapon.offhand with jujutsucraft:supreme_martial_solution");
                                    animatable.getServer().getCommands().performPrefixedCommand(stack, "item replace entity @s armor.chest with jujutsucraft:sukuna_body_chestplate");
                                }

                                final Vec3 _center = new Vec3(_livEntity.getX(), _livEntity.getY(), _livEntity.getZ());
                                for (Entity p : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(15.0), e -> e instanceof Player)) {
                                    if (p instanceof Player _player && !_player.level().isClientSide()) {
                                        _player.displayClientMessage(Component.literal("§lLike a Calamity, The Strongest Sorcerer from History, Awakens"), false);
                                    }
                                }

                                _livEntity.removeEffect(MobEffects.DAMAGE_BOOST);
                                _livEntity.getPersistentData().putDouble("cnt_reverse_lim", 0.0);
                                _livEntity.getPersistentData().putDouble("skill", 1.0);
                                ReturnShadowProcedure.execute(world, x, y, z, _livEntity);
                                _livEntity.getPersistentData().putDouble("skill", 0.0);
                                if (item_head.getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()) {
                                    _livEntity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                                }
                            }
                        }
                    }
                }

                infinity = target.hasEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get());
                _livEntity.getPersistentData().putDouble("cnt_x", _livEntity.getPersistentData().getDouble("cnt_x") + 1.0);
                _livEntity.getPersistentData().putDouble("cnt_rest", 0.0);

                if (_livEntity.getPersistentData().getDouble("cnt_x") > 10.0 && _livEntity.getPersistentData().getDouble("skill") == 0.0) {
                    mahoraga_exist = _livEntity.getPersistentData().getDouble("TenShadowsTechnique14") == -1.0;
                    vsMahoraga = target instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
                    distance = GetDistanceProcedure.execute(_livEntity);

                    if (vsMahoraga && _livEntity.getPersistentData().getDouble("cnt_target") >= 2400.0 && _livEntity.getPersistentData().getDouble("cnt_target") <= 3600.0) {
                        _livEntity.getPersistentData().putBoolean("flag_domain", true);
                    }

                    domain = LogicConfilmDomainProcedure.execute(world, x, y, z, _livEntity) && !mahoraga_exist;
                    ResetCounterProcedure.execute(_livEntity);

                    // Ten Shadows AI
                    if (fushiguro_body && !(_livEntity instanceof SukunaFushiguroEntity _sf && (Boolean) _sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                        if (_livEntity.getPersistentData().getDouble("cnt_target") > 200.0 && _livEntity.getPersistentData().getDouble("TenShadowsTechnique14") >= 1.0 && item_head.getItem() != JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()) {
                            if (gojosatoru) {
                                _livEntity.setItemSlot(EquipmentSlot.HEAD, new ItemStack((ItemLike) JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()));
                                _livEntity.setItemSlot(EquipmentSlot.CHEST, new ItemStack((ItemLike) JujutsucraftModItems.CLOTHES_SUKUNA_FUSHIGURO_CHESTPLATE.get()));
                            } else if (target instanceof YorozuEntity || _livEntity.getHealth() <= _livEntity.getMaxHealth() / 1.1) {
                                _livEntity.setItemSlot(EquipmentSlot.HEAD, new ItemStack((ItemLike) JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()));
                            }
                        }
                    }

                    // Hunger logic
                    if (((!_livEntity.getPersistentData().getBoolean("flag1") && _livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) || _livEntity.getPersistentData().getBoolean("flag2")) && distance < 48.0 && !_livEntity.level().isClientSide()) {
                        _livEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false));
                    }

                    // Mahoraga Flag logic
                    if (fushiguro_body && !(_livEntity instanceof SukunaFushiguroEntity _sf && (Boolean) _sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                        boolean flagMahoraga = _livEntity.getPersistentData().getDouble("TenShadowsTechnique14") > 0.0 && (_livEntity.getHealth() < _livEntity.getMaxHealth() * 0.6 || (item_head.getOrCreateTag().getDouble("skill205") >= 100.0 && infinity));
                        if (flagMahoraga && target instanceof GojoSatoruEntity) {
                            if ((_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.BRAIN_DAMAGE.get()) ? _livEntity.getEffect((MobEffect) JujutsucraftModMobEffects.BRAIN_DAMAGE.get()).getAmplifier() : 0) < 1) flagMahoraga = false;
                        }
                        _livEntity.getPersistentData().putBoolean("flag_mahoraga", flagMahoraga);
                    }

                    flag_megumiTechnique = fushiguro_body && !(_livEntity instanceof SukunaFushiguroEntity _sf && (Boolean) _sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode)) && ((_livEntity.getPersistentData().getBoolean("flag_mahoraga") || Math.random() < 0.2 && !infinity) && !_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && !domain && _livEntity.getPersistentData().getDouble("TenShadowsTechnique14") >= 1.0 || _livEntity.getPersistentData().getBoolean("flag_agito") && _livEntity.getPersistentData().getDouble("TenShadowsTechnique13") >= 0.0);

                    // Skill Selection
                    if (!LogicStartProcedure.execute(_livEntity) || (!(Math.random() > (infinity ? 0.75 : 0.0)) || !(distance < 48.0)) && !_livEntity.hasEffect(MobEffects.HUNGER) && !flag_megumiTechnique) {
                        if (!domain) {
                            _livEntity.getPersistentData().putDouble("cnt_x", 0.0);
                            rnd = 0.0; tick = 0.0;
                            if (!LogicCooldownCombatProcedure.execute(_livEntity) && !infinity) {
                                _livEntity.removeEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
                                if (LogicStartPassiveProcedure.execute(_livEntity)) {
                                    if (distance < 16.0 && Math.random() < 0.5) { rnd = 111.0; tick = 50.0; }
                                    if (distance < 10.0 && Math.random() < 0.75) { rnd = 112.0; tick = 50.0; }
                                    if (distance < 48.0 && Math.random() < 0.25) { rnd = 113.0; tick = 50.0; }
                                }
                            }
                            if (rnd != 0.0) {
                                _livEntity.getPersistentData().putDouble("skill", rnd);
                                if (!_livEntity.level().isClientSide()) {
                                    _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int) tick, 0, false, false));
                                    _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                                }
                            } else {
                                CalculateAttackProcedure.execute(world, _livEntity);
                            }
                            return;
                        }
                    }

                    if (flag_megumiTechnique) {
                        AIFushiguroMegumiProcedure.execute(world, x, y, z, _livEntity);
                    } else {
                        _livEntity.getPersistentData().putDouble("cnt_x", 0.0);
                        if (domain) {
                            rnd = 20.0; tick = 20.0;
                        } else if (_livEntity.getPersistentData().getBoolean("flag2")) {
                            if ((target.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get()) ? target.getEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get()).getDuration() : 0) <= 20 && (target.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get()) ? target.getEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get()).getDuration() : 0) <= 20) {
                                _livEntity.getPersistentData().putBoolean("flag2", false);
                                rnd = 7.0; tick = 250.0;
                                _livEntity.removeEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get());
                                _livEntity.removeEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get());
                            }
                        } else if (!_livEntity.getPersistentData().getBoolean("flag1") && (!(target instanceof GojoSatoruEntity) || !fushiguro_body) && _livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && !_livEntity.getPersistentData().getBoolean("Failed") && _livEntity.getPersistentData().getDouble("dust_amount") > 100.0 && (((Mob) entity).hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) ? ((Mob) entity).getEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getDuration() : 0) < 1200) {
                            _livEntity.getPersistentData().putBoolean("flag1", true);
                            rnd = 7.0; tick = 250.0;
                        } else {
                            if (infinity && !(_livEntity instanceof SukunaFushiguroEntity _sf && (Boolean) _sf.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut)) && !(_livEntity instanceof SukunaPerfectEntity)) {
                                rnd = 0.0;
                            } else {
                                for (int i = 0; i < 256; i++) {
                                    rnd = Mth.nextInt(RandomSource.create(), 5, 8);
                                    if (rnd == 5.0) { tick = 50.0; break; }
                                    if (rnd == 6.0) {
                                        tick = 100.0;
                                        if (Math.random() >= 0.5 || infinity || vsMahoraga || target instanceof GojoSatoruEntity || target instanceof JogoEntity || distance > 4.0) continue;
                                        break;
                                    } else if (rnd == 7.0) {
                                        tick = 250.0;
                                        if (infinity || target instanceof GojoSatoruEntity && fushiguro_body || target instanceof JogoEntity || _livEntity.getPersistentData().getDouble("cnt_target") <= 1200.0 || vsMahoraga && !_livEntity.getPersistentData().getBoolean("flag1") || Math.random() < 0.95 || distance < 8.0) continue;
                                        break;
                                    } else if (rnd == 8.0) {
                                        rnd = 20.0; tick = 20.0;
                                        if (!mahoraga_exist && !AIDomainLogicProcedure.execute(world, x, y, z, _livEntity)) break;
                                    }
                                }
                            }
                        }

                        if (rnd <= 0.0) {
                            CalculateAttackProcedure.execute(world, _livEntity);
                        } else {
                            if (rnd == 20.0 && !_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.BRAIN_DAMAGE.get())) {
                                _livEntity.getPersistentData().putDouble("skill", 1.0);
                                ReturnShadowProcedure.execute(world, x, y, z, _livEntity);
                            }
                            if (mahoraga_exist && rnd == 7.0) {
                                tick = 100.0;
                                _livEntity.getPersistentData().putDouble("skill", 1007.0);
                            } else {
                                if (fushiguro_body) {
                                    if (gojosatoru || _livEntity.getHealth() <= _livEntity.getMaxHealth() * 0.5 || (_livEntity instanceof SukunaFushiguroEntity _sf && (Boolean) _sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                                        if (_livEntity.hasEffect((MobEffect) JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get()) && _livEntity.getPersistentData().getDouble("TenShadowsTechnique14") == -2.0) {
                                            if (rnd == 5.0) {
                                                _livEntity.getPersistentData().putDouble("cnt6", 30.0);
                                                _livEntity.getPersistentData().putBoolean("flag_dismantle", true);
                                            }
                                        }
                                        _livEntity.getPersistentData().putDouble("skill", 100.0 + rnd);
                                    } else if (target instanceof YorozuEntity || !(_livEntity instanceof SukunaFushiguroEntity _sf2 && (Boolean) _sf2.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                                        if (domain) _livEntity.getPersistentData().putDouble("skill", 620.0);
                                        else if (_livEntity.getHealth() <= _livEntity.getMaxHealth() / 1.5 && item_head.getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()) _livEntity.getPersistentData().putDouble("skill", 618.0);
                                        else {
                                            double chance_skill = Math.random();
                                            if (chance_skill < 0.05) { TenShadowsRegisterProcedure.execute(_livEntity); _livEntity.getPersistentData().putDouble("skill", 600.0 + Mth.nextInt(RandomSource.create(), 7, 14)); }
                                            // CORRECTED MATH: Interval [0.05 to 0.15) results in exactly 10% chance for skill 1007.0
                                            else if (chance_skill < 0.15) _livEntity.getPersistentData().putDouble("skill", 1007.0); 
                                            else _livEntity.getPersistentData().putDouble("skill", Mth.nextInt(RandomSource.create(), -97, -96));
                                        }
                                    } else _livEntity.getPersistentData().putDouble("skill", 100.0 + rnd);
                                } else _livEntity.getPersistentData().putDouble("skill", 100.0 + rnd);
                            }
                            if (!_livEntity.level().isClientSide()) {
                                _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) tick / 2, 0, false, false));
                                _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                            }
                        }
                    }
                    if (_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get()) && !_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) _livEntity.getPersistentData().putBoolean("flag1", false);
                } else {
                    _livEntity.getPersistentData().putBoolean("flag2", false);
                    _livEntity.getPersistentData().putDouble("cnt_x", 0.0);
                    _livEntity.getPersistentData().putDouble("cnt_rest", _livEntity.getPersistentData().getDouble("cnt_rest") + 1.0);
                    if (_livEntity.getPersistentData().getDouble("cnt_rest") > 120.0) {
                        _livEntity.getPersistentData().putDouble("cnt_rest", 0.0);
                        _livEntity.getPersistentData().putDouble("skill", 1.0);
                        ReturnShadowProcedure.execute(world, x, y, z, _livEntity);
                        _livEntity.getPersistentData().putDouble("skill", 0.0);
                    }
                }
            }
        }
    }
}
