package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.procedures.UnlimitedPurpleProcedure;
import net.mcreator.jujutsucraft.entity.GojoSatoruEntity;
import net.mcreator.jujutsucraft.entity.GojoSatoruSchoolDaysEntity;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(value = AIGojoSchoolDaysProcedure.class, priority = -10000)
public abstract class AISatoruGojoProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity != null) {
            boolean adult = false;
            boolean defense = false;
            boolean domain = false;
            boolean simple = false;
            boolean purple = false;
            boolean red = false;
            boolean target_sukuna = false;
            Entity target_entity = null;
            double rnd = 0.0;
            double strlv = 0.0;
            double tick = 0.0;
            double health = 0.0;
            double z_pos = 0.0;
            double num1 = 0.0;
            double distance = 0.0;

            if (entity.isAlive() && entity instanceof LivingEntity _livEntity && !_livEntity.hasEffect(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get())) {
                BlockPos belowPos = _livEntity.blockPosition().below();
                boolean isJustAboveWater = ((world.getBlockState(BlockPos.containing(_livEntity.getX(), _livEntity.getY() - 1, _livEntity.getZ()))).getBlock() instanceof LiquidBlock);
                if (isJustAboveWater && !_livEntity.isInWater()) {
                    if (_livEntity.getDeltaMovement().y() <= 0) {
                        _livEntity.setDeltaMovement(_livEntity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                        _livEntity.setOnGround(true);
                        _livEntity.setPos(_livEntity.getX(), belowPos.getY() + 1.0, _livEntity.getZ());
                    }
                }

                if (_livEntity instanceof GojoSatoruSchoolDaysEntity _gojoDays) {
                    if ((Boolean) _gojoDays.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_dying)) {
                        _gojoDays.getPersistentData().putDouble("cnt_target", 0.0);
                        if (!_gojoDays.level().isClientSide()) {
                            _gojoDays.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 99, false, false));
                        }

                        if (!_gojoDays.getSyncedAnimation().equals("death")) {
                            PlayAnimationEntity2Procedure.execute(_gojoDays, "death");
                        }

                        _gojoDays.getPersistentData().putDouble("cnt_dying", _gojoDays.getPersistentData().getDouble("cnt_dying") + 1.0);
                        if (_gojoDays.getPersistentData().getDouble("cnt_dying") > 200.0) {
                            _gojoDays.getEntityData().set(GojoSatoruSchoolDaysEntity.DATA_awaking, true);
                            _gojoDays.getEntityData().set(GojoSatoruSchoolDaysEntity.DATA_dying, false);

                            if (!_gojoDays.level().isClientSide() && _gojoDays.getServer() != null) {
                                _gojoDays.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _gojoDays.position(), _gojoDays.getRotationVector(), _gojoDays.level() instanceof ServerLevel ? (ServerLevel) _gojoDays.level() : null, 4, _gojoDays.getName().getString(), _gojoDays.getDisplayName(), _gojoDays.level().getServer(), _gojoDays), "data merge entity @s {Invulnerable:0b}");
                            }

                            _gojoDays.removeEffect(MobEffects.DAMAGE_BOOST);
                            _gojoDays.removeEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get());
                            _gojoDays.removeEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get());
                            _gojoDays.setHealth(_gojoDays.getMaxHealth());

                            AnimationResetProcedure.execute(_gojoDays);
                            _gojoDays.getPersistentData().putDouble("cnt_x", 0.0);
                            ResetCounterProcedure.execute(_gojoDays);
                        }
                        return;
                    }
                }

                if (!_livEntity.hasEffect(JujutsucraftaddonModMobEffects.SOKA_MONA.get())) {
                    AIActiveProcedure.execute(world, x, y, z, _livEntity);
                }

                _livEntity.getPersistentData().putBoolean("infinity", true);
                WhenPlayerActiveTickInfinityProcedure.execute(_livEntity);

                if (_livEntity.getPersistentData().getBoolean("GojoNoUseInfinity")) {
                    if (_livEntity.getPersistentData().getDouble("cnt_target") > 50.0) {
                        _livEntity.getPersistentData().putBoolean("GojoNoUseInfinity", false);
                    } else if (_livEntity.getPersistentData().getDouble("cnt_target") > 3.0) {
                        if (!_livEntity.level().isClientSide()) {
                            _livEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 9, false, false));
                        }
                    } else {
                        LivingEntity target_check = (_livEntity instanceof Mob _mobEnt) ? _mobEnt.getTarget() : null;
                        if (target_check == null && !world.getEntitiesOfClass(SukunaFushiguroEntity.class, AABB.ofSize(new Vec3(x, y, z), 256.0, 256.0, 256.0), (e) -> true).isEmpty()) {
                            target_entity = (Entity) world.getEntitiesOfClass(SukunaFushiguroEntity.class, AABB.ofSize(new Vec3(x, y, z), 256.0, 256.0, 256.0), (e) -> true).stream().sorted(Comparator.comparingDouble((_entcnd) -> _entcnd.distanceToSqr(x, y, z))).findFirst().orElse(null);
                            if (_livEntity instanceof Mob _mobEnt && target_entity instanceof LivingEntity _target) {
                                _mobEnt.setTarget(_target);
                            }
                        }
                    }
                }

                LivingEntity current_target = (_livEntity instanceof Mob _mobEnt) ? _mobEnt.getTarget() : null;
                adult = _livEntity instanceof GojoSatoruEntity;
                target_sukuna = current_target instanceof SukunaFushiguroEntity;

                if (!_livEntity.hasEffect(MobEffects.DAMAGE_BOOST)) {
                    if (_livEntity instanceof GojoSatoruSchoolDaysEntity _gojoDays && (Boolean) _gojoDays.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking)) {
                        if (!_livEntity.level().isClientSide()) {
                            _livEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 26, false, false));
                            _livEntity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, Integer.MAX_VALUE, 26, false, false));
                        }
                    } else {
                        if (!_livEntity.level().isClientSide()) {
                            _livEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, adult ? 29 : 20, false, false));
                        }
                    }
                }
                if (!_livEntity.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                    if (!_livEntity.level().isClientSide()) {
                        _livEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false));
                    }
                }
                if (!_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.SIX_EYES.get())) {
                    if (!_livEntity.level().isClientSide()) {
                        _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.SIX_EYES.get(), Integer.MAX_VALUE, 4, false, false));
                    }
                }

                if (_livEntity instanceof GojoSatoruEntity _gojo && (Boolean) _gojo.getEntityData().get(GojoSatoruEntity.DATA_ghost)) {
                    if (!_gojo.level().isClientSide()) {
                        _gojo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false));
                    }
                    if (_gojo.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get()) && !_gojo.level().isClientSide() && _gojo.getServer() != null) {
                        _gojo.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _gojo.position(), _gojo.getRotationVector(), _gojo.level() instanceof ServerLevel ? (ServerLevel) _gojo.level() : null, 4, _gojo.getName().getString(), _gojo.getDisplayName(), _gojo.level().getServer(), _gojo), "kill @s");
                    }
                }

                if (current_target != null) {
                    if (_livEntity.getPersistentData().getDouble("cnt_target") > 150.0 && target_sukuna) {
                        LivingEntity target_of_target = (current_target instanceof Mob _mobEnt) ? _mobEnt.getTarget() : null;
                        if (target_of_target == null && current_target instanceof Mob _mobEnt) {
                            _mobEnt.setTarget(_livEntity);
                        }
                    }

                    ItemStack mainHandItem = current_target.getMainHandItem();
                    defense = mainHandItem.getItem() == JujutsucraftModItems.INVERTED_SPEAR_OF_HEAVEN.get() || mainHandItem.getItem() == JujutsucraftModItems.BLACK_ROPE.get();
                    domain = LogicConfilmDomainProcedure.execute(world, x, y, z, _livEntity) && adult;

                    _livEntity.getPersistentData().putDouble("cnt_x", _livEntity.getPersistentData().getDouble("cnt_x") + 1.0);
                    if (_livEntity.getPersistentData().getDouble("cnt_x") > 10.0 && _livEntity.getPersistentData().getDouble("skill") == 0.0) {
                        _livEntity.getPersistentData().putDouble("cnt_x", 0.0);
                        if (!_livEntity.getPersistentData().getBoolean("GojoNoUseInfinity") && target_sukuna) {
                            ItemStack chestItem = _livEntity.getItemBySlot(EquipmentSlot.CHEST);
                            if (chestItem.getItem() == JujutsucraftModItems.CLOTHES_DECISIVE_BATTLE_CHESTPLATE.get()) {
                                if (_livEntity instanceof Player _player) {
                                    _player.getInventory().armor.set(3, ItemStack.EMPTY);
                                    _player.getInventory().armor.set(2, new ItemStack((ItemLike) JujutsucraftModItems.CLOTHES_FUSHIGURO_TOJI_CHESTPLATE.get()));
                                    _player.getInventory().armor.set(1, new ItemStack((ItemLike) JujutsucraftModItems.CLOTHES_FUSHIGURO_TOJI_LEGGINGS.get()));
                                    _player.getInventory().setChanged();
                                } else {
                                    _livEntity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                                    _livEntity.setItemSlot(EquipmentSlot.CHEST, new ItemStack((ItemLike) JujutsucraftModItems.CLOTHES_FUSHIGURO_TOJI_CHESTPLATE.get()));
                                    _livEntity.setItemSlot(EquipmentSlot.LEGS, new ItemStack((ItemLike) JujutsucraftModItems.CLOTHES_FUSHIGURO_TOJI_LEGGINGS.get()));
                                }
                            }
                        }

                        ResetCounterProcedure.execute(_livEntity);
                        distance = GetDistanceProcedure.execute(_livEntity);
                        health = _livEntity.getHealth() / _livEntity.getMaxHealth();

                        boolean canConsiderPurple = true;
                        if (_livEntity instanceof GojoSatoruSchoolDaysEntity _gojoDays) {
                            canConsiderPurple = (Boolean) _gojoDays.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking);
                        }

                        if (canConsiderPurple) {
                            purple = _livEntity.getPersistentData().getBoolean("GojoNoUseInfinity") && distance > 32.0;
                            if (!_livEntity.getPersistentData().getBoolean("flag1") && health < 0.3 && (!adult || distance < 32.0)) {
                                if (!_livEntity.level().isClientSide()) {
                                    _livEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false));
                                }
                                purple = true;
                            }
                        }

                        if (!domain && distance < 24.0) {
                            if (current_target.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && !_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                int duration = current_target.getEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getDuration();
                                if (duration <= 600) {
                                    red = true;
                                }
                            }
                        }

                        boolean shouldAttack = true;
                        if (LogicStartProcedure.execute(_livEntity)) {
                            if (Math.random() > (defense ? 0.3 : 0.5)) {
                                shouldAttack = false;
                            } else if ((purple || red) && _livEntity instanceof GojoSatoruSchoolDaysEntity _gojoDays && (Boolean) _gojoDays.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking)) {
                                shouldAttack = false;
                            }
                        }

                        if (shouldAttack && !domain) {
                            if (!_livEntity.hasEffect(JujutsucraftaddonModMobEffects.SOKA_MONA.get())) {
                                CalculateAttackProcedure.execute(world, _livEntity);
                            }
                        } else {
                            if (domain) {
                                rnd = 20.0;
                                tick = 20.0;
                            } else {
                                label_skill_selection: {
                                    if (!_livEntity.getPersistentData().getBoolean("flag2")) {
                                        if (_livEntity instanceof GojoSatoruSchoolDaysEntity _gojoDays) {
                                            if ((Boolean) _gojoDays.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking)) {
                                                _livEntity.getPersistentData().putBoolean("flag2", true);
                                                rnd = 7.0;
                                                tick = 250.0;
                                                break label_skill_selection;
                                            }
                                        } else if (health < 0.5 && distance < 16.0) {
                                            _livEntity.getPersistentData().putBoolean("flag2", true);
                                            rnd = 7.0;
                                            tick = 250.0;
                                            break label_skill_selection;
                                        }
                                    }

                                    if (!_livEntity.getPersistentData().getBoolean("flag1")) {
                                        boolean canUsePurple = false;
                                        if (_livEntity instanceof GojoSatoruSchoolDaysEntity _gojoDays) {
                                            if ((Boolean) _gojoDays.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking) && health < 0.5) {
                                                canUsePurple = true;
                                            }
                                        } else if (health < 0.3 && distance < 40.0) {
                                            canUsePurple = true;
                                        }

                                        if (canUsePurple) {
                                            _livEntity.getPersistentData().putBoolean("flag1", true);
                                            if (_livEntity instanceof GojoSatoruEntity _gojo && !_gojo.hasEffect(MobEffects.SLOW_FALLING)) {
                                                if (!_gojo.getSyncedAnimation().equals("murasaki2")) {
                                                    PlayAnimationEntity2Procedure.execute(_gojo, "murasaki2");
                                                }
                                                if (!_gojo.level().isClientSide()) {
                                                    _gojo.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.WORLD_GOJO.get(), 240, 1, false, false));
                                                }
                                                UnlimitedPurpleProcedure.execute(world, x, y, z, _gojo);
                                                return;
                                            }
                                            rnd = 15.0;
                                            tick = 500.0;
                                            break label_skill_selection;
                                        }
                                    }

                                    if (purple) {
                                        boolean schoolDaysBeforeAwakening = _livEntity instanceof GojoSatoruSchoolDaysEntity _gojoDays
                                                && !(Boolean) _gojoDays.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking);
                                        if (!schoolDaysBeforeAwakening) {
                                            _livEntity.getPersistentData().putBoolean("GojoNoUseInfinity", false);
                                            rnd = 15.0;
                                            tick = 500.0;
                                            break label_skill_selection;
                                        }
                                    }

                                    num1 = 0.0;
                                    for (int i = 0; i < 128; i++) {
                                        num1++;
                                        if (num1 > 96.0) {
                                            rnd = 0.0;
                                            break label_skill_selection;
                                        }

                                        rnd = (double) Math.round(4.0 + Math.random() * 16.0);
                                        if (_livEntity instanceof GojoSatoruSchoolDaysEntity _gojoDays && !(Boolean) _gojoDays.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking)) {
                                            if (rnd == 7.0 || rnd == 15.0) continue;
                                        }

                                        if (rnd == 6.0) {
                                            tick = 100.0;
                                            int amp = _livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()) ? _livEntity.getEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier() : 0;
                                            if (amp <= 0) break label_skill_selection;
                                        } else if (rnd == 7.0) {
                                            tick = 250.0;
                                            if (Math.random() <= 0.5 && distance <= 16.0) {
                                                int amp = _livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()) ? _livEntity.getEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier() : 0;
                                                if (amp <= 0 && !_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) break label_skill_selection;
                                            }
                                        } else if (rnd == 8.0) {
                                            tick = 100.0;
                                            int amp = _livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()) ? _livEntity.getEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier() : 0;
                                            if (amp <= 0) break label_skill_selection;
                                        } else if (rnd == 15.0) {
                                            tick = 500.0;
                                            if (!target_sukuna && distance >= 8.0) {
                                                int amp = _livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()) ? _livEntity.getEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier() : 0;
                                                if (amp <= 0 && Math.random() <= 0.1 && !_livEntity.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) break label_skill_selection;
                                            }
                                        } else if (rnd == 20.0) {
                                            tick = 20.0;
                                            if (adult && !AIDomainLogicProcedure.execute(world, x, y, z, _livEntity)) {
                                                if (current_target.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) && !LogicConfilmDomainProcedure.execute(world, x, y, z, _livEntity)) break label_skill_selection;
                                                break label_skill_selection;
                                            }
                                        }
                                    }
                                    rnd = 7.0;
                                    tick = 250.0;
                                }
                            }

                            if (!_livEntity.hasEffect(JujutsucraftaddonModMobEffects.SOKA_MONA.get())) {
                                if (rnd > 0.0) {
                                    _livEntity.getPersistentData().putDouble("skill", (double) Math.round(200.0 + rnd));
                                    if (!_livEntity.level().isClientSide()) {
                                        _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                                        _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) tick, 0, false, false));
                                    }
                                } else {
                                    CalculateAttackProcedure.execute(world, _livEntity);
                                }
                            }
                        }

                        if (rnd == 20.0) {
                            if (_livEntity instanceof Player _player) {
                                _player.getInventory().armor.set(3, ItemStack.EMPTY);
                                _player.getInventory().setChanged();
                            } else {
                                _livEntity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                            }
                        }
                    }
                } else {
                    _livEntity.getPersistentData().putDouble("cnt_x", 0.0);
                    if (health >= 0.5) _livEntity.getPersistentData().putBoolean("flag2", false);
                    if (health >= 0.3) _livEntity.getPersistentData().putBoolean("flag1", false);
                }
            }
        }
    }
}
