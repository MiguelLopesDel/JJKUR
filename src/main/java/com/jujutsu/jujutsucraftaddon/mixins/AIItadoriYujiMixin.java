package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.CloneEntity;
import com.jujutsu.jujutsucraftaddon.entity.FakeClonesEntity;
import com.jujutsu.jujutsucraftaddon.entity.FakePurpleClonesEntity;
import com.jujutsu.jujutsucraftaddon.entity.ItadoriShinjukuEntity;
import net.mcreator.jujutsucraft.entity.ItadoriYujiEntity;
import net.mcreator.jujutsucraft.entity.ItadoriYujiShibuyaEntity;
import net.mcreator.jujutsucraft.entity.ItadoriYujiShinjukuEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AIItadoriYujiProcedure.class, priority = -10000)
public class AIItadoriYujiMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity != null && entity.isAlive()) {
            double rnd = 0.0;
            double lv_st = 0.0;
            double lv_df = 0.0;
            double distance = 0.0;
            double tick = 0.0;
            double level = 0.0;

            AIActiveProcedure.execute(world, x, y, z, entity);

            // Suporte para entidades do Addon
            if (entity instanceof ItadoriYujiShinjukuEntity || entity instanceof ItadoriShinjukuEntity) {
                lv_st = 18.0;
                lv_df = 3.0;
            } else if (entity instanceof ItadoriYujiShibuyaEntity || entity instanceof CloneEntity || entity instanceof FakeClonesEntity || entity instanceof FakePurpleClonesEntity) {
                lv_st = 10.0;
                lv_df = 3.0;
            } else {
                lv_st = 7.0;
                lv_df = 2.0;
            }

            if (entity instanceof LivingEntity _liv) {
                if (!_liv.hasEffect(MobEffects.DAMAGE_BOOST)) {
                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, (int) lv_st, false, false));
                    }
                }
                if (!_liv.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, (int) lv_df, false, false));
                    }
                }
            }

            // Invisibility & Helmet logic
            if (entity instanceof ItadoriYujiEntity) {
                handleInvisibilityLogic(entity);
            }

            LivingEntity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;

            if (target instanceof LivingEntity) {
                entity.getPersistentData().putDouble("cnt_x", entity.getPersistentData().getDouble("cnt_x") + 1.0);
                if (entity.getPersistentData().getDouble("cnt_x") > 10.0 && entity.getPersistentData().getDouble("skill") == 0.0) {
                    entity.getPersistentData().putDouble("cnt_x", 0.0);
                    distance = GetDistanceProcedure.execute(entity);

                    // New AI logic from base mod
                    if (LogicStartProcedure.execute(entity) && (entity instanceof ItadoriYujiShinjukuEntity || entity instanceof ItadoriShinjukuEntity)) {
                        if (distance < 6.0 && Math.random() < 0.1) {
                            level = 0.0;
                            rnd = 2108.0;
                            tick = 100.0;
                        }
                    }

                    if (!(entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get()))) {
                        if (entity instanceof ItadoriYujiShibuyaEntity || entity instanceof ItadoriYujiShinjukuEntity || entity instanceof ItadoriShinjukuEntity) {
                            float health = (entity instanceof LivingEntity _liv2) ? _liv2.getHealth() : -1.0F;
                            float maxHealth = (entity instanceof LivingEntity _liv2) ? _liv2.getMaxHealth() : -1.0F;
                            if (health < maxHealth * 0.75 || entity.getPersistentData().getDouble("cnt_target") > 1200.0) {
                                if (entity instanceof LivingEntity _liv2 && !_liv2.hasEffect((MobEffect) JujutsucraftModMobEffects.DEEP_CONCENTRATION.get())) {
                                    level = 1.0;
                                    rnd = 2118.0;
                                    tick = 50.0;
                                }
                            }
                        }

                        if (distance < 8.0 && Math.random() < 0.2) {
                            level = 1.0;
                            rnd = 2105.0;
                            tick = 25.0;
                        }

                        if (Math.random() < 0.2 && distance < 8.0) {
                            level = 1.0;
                            rnd = 2106.0;
                            tick = 50.0;
                        }
                    }

                    if (rnd > 0.0) {
                        ResetCounterProcedure.execute(entity);
                        entity.getPersistentData().putDouble("skill", (double) Math.round(rnd));
                        if (entity instanceof LivingEntity _liv) {
                            if (!_liv.level().isClientSide()) {
                                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));

                                // Propósito: Redução de Cooldown do Addon (tick / 2)
                                if (level > 0.0) {
                                    _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int) tick / 2, 0, false, false));
                                } else {
                                    _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) tick / 2, 0, false, false));
                                }
                            }
                        }
                    } else {
                        CalculateAttackProcedure.execute(world, entity);
                    }
                }
            } else {
                entity.getPersistentData().putDouble("cnt_x", 0.0);
            }
        }
    }

    private static void handleInvisibilityLogic(Entity entity) {
        if (entity instanceof LivingEntity _liv) {
            if (_liv.hasEffect(MobEffects.INVISIBILITY)) {
                ItemStack helmet = _liv.getItemBySlot(EquipmentSlot.HEAD);
                if (helmet.getItem() != JujutsucraftModItems.ITADORI_YUJI_PAPER_HELMET.get()) {
                    if (!_liv.level().isClientSide() && _liv.getServer() != null) {
                        CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, _liv.position(), _liv.getRotationVector(), _liv.level() instanceof ServerLevel ? (ServerLevel)_liv.level() : null, 4, _liv.getName().getString(), _liv.getDisplayName(), _liv.level().getServer(), _liv);
                        _liv.getServer().getCommands().performPrefixedCommand(source, "effect give @s invisibility 1000000 0 true");
                    }
                    _liv.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JujutsucraftModItems.ITADORI_YUJI_PAPER_HELMET.get()));
                    _liv.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                    _liv.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                    _liv.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                    _liv.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    _liv.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    if (_liv instanceof Player _p) _p.getInventory().setChanged();
                }
            } else {
                ItemStack helmet = _liv.getItemBySlot(EquipmentSlot.HEAD);
                if (helmet.getItem() == JujutsucraftModItems.ITADORI_YUJI_PAPER_HELMET.get()) {
                    _liv.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    if (_liv instanceof Player _p) _p.getInventory().setChanged();
                }
            }
        }
    }
}
