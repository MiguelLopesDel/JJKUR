package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.entity.ErrorEntity;
import com.jujutsu.jujutsucraftaddon.entity.ItadoriShinjukuEntity;
import com.jujutsu.jujutsucraftaddon.entity.SukunaMangaEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class JJKURSukunaAIBuff {

    public static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null || !entity.isAlive() || !(entity instanceof LivingEntity livingEntity)) return;

        CompoundTag persistentData = livingEntity.getPersistentData();
        ItemStack headItem = livingEntity.getItemBySlot(EquipmentSlot.HEAD);

        handleWaterWalking(world, livingEntity);
        AIActiveProcedure.execute(world, x, y, z, livingEntity);

        boolean isFushiguroBody = livingEntity instanceof SukunaFushiguroEntity ||
                livingEntity instanceof SukunaMangaEntity ||
                livingEntity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity;

        handleArmorStripping(livingEntity);
        handleTenShadowsInit(livingEntity, isFushiguroBody, persistentData);
        handleBuffs(livingEntity, isFushiguroBody, persistentData);

        LivingEntity target = (livingEntity instanceof Mob _mob) ? _mob.getTarget() : null;
        boolean isGojoTarget = checkGojoTarget(target);
        boolean opSukuna = world instanceof ServerLevel level && level.getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA);

        handleBindingVow(livingEntity, isFushiguroBody, isGojoTarget, opSukuna);
        handleMeteorLogic(world, livingEntity, target, persistentData);
        handleHeianTransformation(world, x, y, z, livingEntity, target, isFushiguroBody, persistentData, headItem, opSukuna);
        if (opSukuna && OpSukunaBrain.tryExecute(world, x, y, z, livingEntity, target, persistentData)) {
            return;
        }
        handleAILogic(world, x, y, z, livingEntity, target, isFushiguroBody, persistentData, headItem, isGojoTarget);
    }

    private static void handleWaterWalking(LevelAccessor world, LivingEntity living) {
        BlockPos below = BlockPos.containing(living.getX(), living.getY() - 1, living.getZ());
        if (world.getBlockState(below).getBlock() instanceof LiquidBlock && !living.isInWater() && living.getDeltaMovement().y() <= 0) {
            living.setDeltaMovement(living.getDeltaMovement().multiply(1.0, 0.0, 1.0));
            living.setOnGround(true);
            living.setPos(living.getX(), below.getY() + 1.0, living.getZ());
        }
    }

    private static void handleArmorStripping(LivingEntity living) {
        if (living.getItemBySlot(EquipmentSlot.CHEST).getItem() == JujutsucraftModItems.UNIFORM_NORMAL_CHESTPLATE.get() && Math.random() < 1.0 / 3600.0) {
            living.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        }
        if (living.getItemBySlot(EquipmentSlot.HEAD).getItem() == JujutsucraftModItems.HAIR_FUSHIGURO_MEGUMI_HELMET.get() && Math.random() < 1.0 / 1200.0) {
            living.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
    }

    private static void handleTenShadowsInit(LivingEntity living, boolean isFushiguroBody, CompoundTag nbt) {
        if (isFushiguroBody && !nbt.getBoolean("flag_start")) {
            nbt.putBoolean("flag_start", true);
            TenShadowsRegisterProcedure.execute(living);
            for (int i = 4; i <= 10; i++) nbt.putDouble("TenShadowsTechnique" + i, 1.0);
            nbt.putDouble("TenShadowsTechnique1", -2.0);
            nbt.putDouble("TenShadowsTechnique5", -2.0);
            nbt.putDouble("TenShadowsTechnique14", 1.0);
        }
    }

    private static void handleBuffs(LivingEntity living, boolean isFushiguroBody, CompoundTag nbt) {
        if (!living.hasEffect(MobEffects.DAMAGE_BOOST)) {
            double amp;
            if (living instanceof SukunaPerfectEntity || living instanceof ErrorEntity) {
                amp = 35.0;
                nbt.putDouble("KnockbackFix", 1.0);
            } else if (isFushiguroBody && !(living instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                amp = 30.0;
            } else {
                amp = 25.0;
            }

            if (!living.level().isClientSide()) {
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, (int) Math.round(amp), false, false));
                if (!(living instanceof ItadoriShinjukuEntity)) {
                    int sukunaAmp = (int) Math.round(Mth.clamp(amp - 11.0, 0.0, 19.0));
                    living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.SUKUNA_EFFECT.get(), Integer.MAX_VALUE, sukunaAmp, false, false));
                }
            }
        }
        if (!living.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !living.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false));
        }
    }

    private static boolean checkGojoTarget(LivingEntity target) {
        if (target == null) return false;
        if (target instanceof GojoSatoruEntity || target instanceof GojoSatoruSchoolDaysEntity) return true;

        return target.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).map(vars -> {
            double technique = vars.SecondTechnique ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
            return technique == TechniqueIDs.GOJO;
        }).orElse(false);
    }

    private static void handleBindingVow(LivingEntity living, boolean isFushiguroBody, boolean isGojoTarget, boolean opSukuna) {
        if (opSukuna) return;
        if (isFushiguroBody && isGojoTarget && !living.hasEffect(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get())) {
            if (!living.level().isClientSide()) {
                living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get(), 3000, 0, false, false));
            }
        }
    }

    private static void handleMeteorLogic(LevelAccessor world, LivingEntity living, LivingEntity target, CompoundTag nbt) {
        if (target == null || !(living instanceof SukunaEntity) || !(target instanceof JogoEntity) || nbt.getDouble("cnt_target") <= 6.0)
            return;

        if (target.getPersistentData().getDouble("skill") == 415.0) {
            nbt.putBoolean("flag2", true);
            if (!living.level().isClientSide()) {
                living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 10, 0, false, false));
                living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 10, 0, false, false));
            }
        } else if (nbt.getBoolean("flag2")) {
            double distance = GetDistanceProcedure.execute(living);
            if (distance < 32.0) {
                if (distance < 6.0 && living.onGround()) {
                    living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    nbt.putBoolean("PRESS_S", true);
                    WhenBackStepProcedure.execute(world, living);
                    nbt.putBoolean("PRESS_S", false);
                }

                living.removeEffect(MobEffects.MOVEMENT_SPEED);
                living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                target.removeEffect(MobEffects.MOVEMENT_SPEED);
                target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                if (!living.level().isClientSide()) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
                    living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 10, 0, false, false));
                }
                if (!target.level().isClientSide()) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
                    target.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 10, 0, false, false));
                }

                int cooldownTicks = target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) ? target.getEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()).getDuration() : 0;
                if (target.onGround() && cooldownTicks < 200) {
                    target.removeEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get());
                }
            }

            if (target.isPassenger() && target.getVehicle() instanceof MeteorEntity && !living.level().isClientSide()) {
                living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 10, 0, false, false));
                living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 10, 0, false, false));
            }
        }
    }

    private static void handleHeianTransformation(LevelAccessor world, double x, double y, double z, LivingEntity living, LivingEntity target, boolean isFushiguroBody, CompoundTag nbt, ItemStack head, boolean opSukuna) {
        if (target == null || nbt.getDouble("cnt_target") <= 6.0) return;
        if (!isFushiguroBody || (living instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode)))
            return;
        if (!opSukuna && (target instanceof GojoSatoruEntity || target instanceof PurpleEntity)) return;

        boolean blockedByBindingVow = living.hasEffect(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get())
                || target.hasEffect(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get());
        if (living.getHealth() <= living.getMaxHealth() * 0.3 && (opSukuna || !blockedByBindingVow)) {
            transformToHeian(world, x, y, z, living, false);
        }
    }

    public static boolean forceHeianTransformation(LevelAccessor world, LivingEntity living, boolean healToFull) {
        return transformToHeian(world, living.getX(), living.getY(), living.getZ(), living, healToFull);
    }

    private static boolean transformToHeian(LevelAccessor world, double x, double y, double z, LivingEntity living, boolean healToFull) {
        if (!(living instanceof SukunaFushiguroEntity animatable)
                || animatable.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode)) {
            return false;
        }

        animatable.getEntityData().set(SukunaFushiguroEntity.DATA_perfect_mode, true);
        animatable.setTexture("sukuna_perfect");
        syncRotation(animatable);

        PlayAnimationEntity2Procedure.execute(animatable, "heianform");
        if (!animatable.level().isClientSide()) {
            animatable.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.ANIMATION_HEIAN.get(), 40, 1, false, false));
            animatable.addEffect(new MobEffectInstance(MobEffects.HEAL, 10, 10, false, false));

            CommandSourceStack stack = new CommandSourceStack(CommandSource.NULL, animatable.position(), animatable.getRotationVector(), (ServerLevel) world, 4, animatable.getName().getString(), animatable.getDisplayName(), animatable.level().getServer(), animatable);
            animatable.getServer().getCommands().performPrefixedCommand(stack, "particle jjkueffects:red_awakening_2 ~ ~-1 ~ 0 0 0 1 1 force");
            animatable.getServer().getCommands().performPrefixedCommand(stack, "item replace entity @s weapon.offhand with jujutsucraft:supreme_martial_solution");
            animatable.getServer().getCommands().performPrefixedCommand(stack, "item replace entity @s armor.chest with jujutsucraft:sukuna_body_chestplate");
        }

        if (healToFull) {
            animatable.setHealth(animatable.getMaxHealth());
        }

        Vec3 center = living.position();
        for (Player p : world.getEntitiesOfClass(Player.class, new AABB(center, center).inflate(15.0))) {
            if (!p.level().isClientSide()) {
                p.displayClientMessage(Component.literal("§lLike a Calamity, The Strongest Sorcerer from History, Awakens"), false);
            }
        }

        living.removeEffect(MobEffects.DAMAGE_BOOST);
        CompoundTag nbt = living.getPersistentData();
        nbt.putDouble("cnt_reverse_lim", 0.0);
        nbt.putDouble("skill", 1.0);
        ReturnShadowProcedure.execute(world, x, y, z, living);
        nbt.putDouble("skill", 0.0);
        if (living.getItemBySlot(EquipmentSlot.HEAD).getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()) {
            living.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        return true;
    }

    private static void handleAILogic(LevelAccessor world, double x, double y, double z, LivingEntity living, LivingEntity target, boolean isFushiguroBody, CompoundTag nbt, ItemStack head, boolean isGojoTarget) {
        if (target != null && nbt.getDouble("cnt_target") > 6.0) {
            boolean infinity = target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get());
            nbt.putDouble("cnt_x", nbt.getDouble("cnt_x") + 1.0);
            nbt.putDouble("cnt_rest", 0.0);

            if (nbt.getDouble("cnt_x") > 10.0 && nbt.getDouble("skill") == 0.0) {
                boolean mahoragaExist = nbt.getDouble("TenShadowsTechnique14") == -1.0;
                boolean vsMahoraga = target instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
                double distance = GetDistanceProcedure.execute(living);

                if (vsMahoraga && nbt.getDouble("cnt_target") >= 2400.0 && nbt.getDouble("cnt_target") <= 3600.0) {
                    nbt.putBoolean("flag_domain", true);
                }

                boolean domain = LogicConfilmDomainProcedure.execute(world, x, y, z, living) && !mahoragaExist;
                ResetCounterProcedure.execute(living);

                if (isFushiguroBody && !(living instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                    if (nbt.getDouble("cnt_target") > 200.0 && nbt.getDouble("TenShadowsTechnique14") >= 1.0 && head.getItem() != JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()) {
                        if (isGojoTarget) {
                            living.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()));
                            living.setItemSlot(EquipmentSlot.CHEST, new ItemStack(JujutsucraftModItems.CLOTHES_SUKUNA_FUSHIGURO_CHESTPLATE.get()));
                        } else if (target instanceof YorozuEntity || living.getHealth() <= living.getMaxHealth() / 1.1) {
                            living.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()));
                        }
                    }
                }

                if (((!nbt.getBoolean("flag1") && living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) || nbt.getBoolean("flag2")) && distance < 48.0 && !living.level().isClientSide()) {
                    living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false));
                }

                if (isFushiguroBody && !(living instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode))) {
                    boolean flagMahoraga = nbt.getDouble("TenShadowsTechnique14") > 0.0 && (living.getHealth() < living.getMaxHealth() * 0.6 || (head.getOrCreateTag().getDouble("skill205") >= 100.0 && infinity));
                    if (flagMahoraga && target instanceof GojoSatoruEntity) {
                        int brainAmp = living.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get()) ? living.getEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get()).getAmplifier() : 0;
                        if (brainAmp < 1) flagMahoraga = false;
                    }
                    nbt.putBoolean("flag_mahoraga", flagMahoraga);
                }

                boolean flagMegumi = isFushiguroBody && !(living instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode)) &&
                        ((nbt.getBoolean("flag_mahoraga") || Math.random() < 0.2 && !infinity) && !living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && !domain && nbt.getDouble("TenShadowsTechnique14") >= 1.0 ||
                                nbt.getBoolean("flag_agito") && nbt.getDouble("TenShadowsTechnique13") >= 0.0);

                if (!LogicStartProcedure.execute(living) || (!(Math.random() > (infinity ? 0.75 : 0.0)) || !(distance < 48.0)) && !living.hasEffect(MobEffects.HUNGER) && !flagMegumi) {
                    if (!domain) {
                        nbt.putDouble("cnt_x", 0.0);
                        double rnd = 0.0, tick = 0.0;
                        if (!LogicCooldownCombatProcedure.execute(living) && !infinity) {
                            living.removeEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
                            if (LogicStartPassiveProcedure.execute(living)) {
                                if (distance < 16.0 && Math.random() < 0.5) {
                                    rnd = 111.0;
                                    tick = 50.0;
                                }
                                if (distance < 10.0 && Math.random() < 0.75) {
                                    rnd = 112.0;
                                    tick = 50.0;
                                }
                                if (distance < 48.0 && Math.random() < 0.25) {
                                    rnd = 113.0;
                                    tick = 50.0;
                                }
                            }
                        }
                        if (rnd != 0.0) {
                            nbt.putDouble("skill", rnd);
                            if (!living.level().isClientSide()) {
                                living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int) tick, 0, false, false));
                                living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                            }
                        } else {
                            CalculateAttackProcedure.execute(world, living);
                        }
                        return;
                    }
                }

                if (flagMegumi) {
                    AIFushiguroMegumiProcedure.execute(world, x, y, z, living);
                } else {
                    nbt.putDouble("cnt_x", 0.0);
                    double rnd = 0.0, tick = 0.0;
                    if (domain) {
                        rnd = 20.0;
                        tick = 20.0;
                    } else if (nbt.getBoolean("flag2")) {
                        int cd1 = target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) ? target.getEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()).getDuration() : 0;
                        int cd2 = target.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()) ? target.getEffect(JujutsucraftModMobEffects.UNSTABLE.get()).getDuration() : 0;
                        if (cd1 <= 20 && cd2 <= 20) {
                            nbt.putBoolean("flag2", false);
                            rnd = 7.0;
                            tick = 250.0;
                            living.removeEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get());
                            living.removeEffect(JujutsucraftModMobEffects.UNSTABLE.get());
                        }
                    } else if (nbt.getBoolean("flag1") || !((!(target instanceof GojoSatoruEntity) || !isFushiguroBody) && living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) || nbt.getBoolean("Failed") || !(nbt.getDouble("dust_amount") > 100.0) || (living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) ? living.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getDuration() : 0) >= 1200 && !vsMahoraga) {
                        boolean isWorldCutCapable = living instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut);
                        if (infinity && !isWorldCutCapable && !(living instanceof SukunaPerfectEntity)) {
                            rnd = 0.0;
                        } else {
                            for (int i = 0; i < 256; i++) {
                                rnd = Mth.nextInt(RandomSource.create(), 5, 8);
                                if (rnd == 5.0) {
                                    tick = 50.0;
                                    break;
                                }
                                if (rnd == 6.0) {
                                    tick = 100.0;
                                    if (Math.random() >= 0.5 || infinity || vsMahoraga || target instanceof GojoSatoruEntity || target instanceof JogoEntity || distance > 4.0)
                                        continue;
                                    break;
                                } else if (rnd == 7.0) {
                                    tick = 250.0;
                                    if (infinity || target instanceof GojoSatoruEntity && isFushiguroBody || target instanceof JogoEntity || nbt.getDouble("cnt_target") <= 1200.0 || vsMahoraga && !nbt.getBoolean("flag1") || Math.random() < 0.95 || distance < 8.0)
                                        continue;
                                    break;
                                } else if (rnd == 8.0) {
                                    rnd = 20.0;
                                    tick = 20.0;
                                    if (!mahoragaExist && !AIDomainLogicProcedure.execute(world, x, y, z, living)) break;
                                }
                            }
                        }
                    } else {
                        nbt.putBoolean("flag1", true);
                        rnd = 7.0;
                        tick = 250.0;
                    }

                    if (rnd <= 0.0) {
                        CalculateAttackProcedure.execute(world, living);
                    } else {
                        if (rnd == 20.0 && !living.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get())) {
                            nbt.putDouble("skill", 1.0);
                            ReturnShadowProcedure.execute(world, x, y, z, living);
                        }
                        if (mahoragaExist && rnd == 7.0) {
                            tick = 100.0;
                            nbt.putDouble("skill", 1007.0);
                        } else {
                            if (isFushiguroBody) {
                                boolean isPerfect = living instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode);
                                if (isGojoTarget || living.getHealth() <= living.getMaxHealth() * 0.5 || isPerfect) {
                                    if (living.hasEffect(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get()) && nbt.getDouble("TenShadowsTechnique14") == -2.0 && rnd == 5.0) {
                                        nbt.putDouble("cnt6", 30.0);
                                        nbt.putBoolean("flag_dismantle", true);
                                    }
                                    nbt.putDouble("skill", 100.0 + rnd);
                                } else if (target instanceof YorozuEntity || !isPerfect) {
                                    if (domain) nbt.putDouble("skill", 620.0);
                                    else if (living.getHealth() <= living.getMaxHealth() / 1.5 && head.getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get())
                                        nbt.putDouble("skill", 618.0);
                                    else {
                                        double chance = Math.random();
                                        if (chance < 0.05) {
                                            TenShadowsRegisterProcedure.execute(living);
                                            nbt.putDouble("skill", 600.0 + Mth.nextInt(RandomSource.create(), 7, 14));
                                        } else if (chance < 0.15) nbt.putDouble("skill", 1007.0);
                                        else nbt.putDouble("skill", Mth.nextInt(RandomSource.create(), -97, -96));
                                    }
                                } else nbt.putDouble("skill", 100.0 + rnd);
                            } else nbt.putDouble("skill", 100.0 + rnd);
                        }
                        if (!living.level().isClientSide()) {
                            living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) tick / 2, 0, false, false));
                            living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                        }
                    }
                }
                if (living.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()) && !living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()))
                    nbt.putBoolean("flag1", false);
            }
        } else {
            nbt.putBoolean("flag2", false);
            nbt.putDouble("cnt_x", 0.0);
            nbt.putDouble("cnt_rest", nbt.getDouble("cnt_rest") + 1.0);
            if (nbt.getDouble("cnt_rest") > 120.0) {
                nbt.putDouble("cnt_rest", 0.0);
                nbt.putDouble("skill", 1.0);
                ReturnShadowProcedure.execute(world, x, y, z, living);
                nbt.putDouble("skill", 0.0);
            }
        }
    }

    private static void syncRotation(LivingEntity animatable) {
        animatable.setYBodyRot(animatable.getYRot());
        animatable.setYHeadRot(animatable.getYRot());
        animatable.yRotO = animatable.getYRot();
        animatable.xRotO = animatable.getXRot();
        animatable.yBodyRotO = animatable.getYRot();
        animatable.yHeadRotO = animatable.getYRot();
    }
}
