package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.YutaCullingGamesEntity;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AIOkkotsuProcedure.class, priority = -10000)
public abstract class AIOkkotsuMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        if (entity.isAlive()) {
            ItemStack ITEM1 = ItemStack.EMPTY;
            boolean use_copy = false;
            boolean domain = false;
            boolean PureLoveCannon = false;
            boolean canUseCopy = false;
            boolean StrongEnemy = false;
            boolean logicLocateRika = false;
            Entity entity_rika = null;
            double NUM_COPY = 0.0;
            double distance = 0.0;
            double rnd = 0.0;
            double tick = 0.0;
            double level = 0.0;

            AIActiveProcedure.execute(world, x, y, z, entity);

            if (entity instanceof LivingEntity _liv) {
                if (!_liv.hasEffect(MobEffects.DAMAGE_BOOST)) {
                    if (!_liv.level().isClientSide()) {
                        int amp = (entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof YutaCullingGamesEntity) ? 20 : 18;
                        _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, amp, false, false));
                    }
                }
                if (!_liv.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false));
                    }
                }
            }

            LivingEntity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;

            if (target instanceof LivingEntity && entity.getPersistentData().getDouble("cnt_target") > 6.0) {
                entity.getPersistentData().putDouble("cnt_rika", 0.0);
                entity.getPersistentData().putDouble("cnt_x", entity.getPersistentData().getDouble("cnt_x") + 1.0);

                if (entity.getPersistentData().getDouble("cnt_x") > 10.0 && entity.getPersistentData().getDouble("skill") == 0.0) {
                    entity.getPersistentData().putDouble("cnt_x", 0.0);
                    logicLocateRika = LocateRikaProcedure.execute(world, entity);
                    canUseCopy = logicLocateRika || (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && !entity.getPersistentData().getBoolean("Failed"));

                    // Rika Defense Logic (Reintroduced from Original Addon IA)
                    if (target instanceof Mob _mobTarget) {
                        LivingEntity targetsTarget = _mobTarget.getTarget();
                        if ((targetsTarget instanceof RikaEntity || targetsTarget instanceof Rika2Entity) && logicLocateRika) {
                            entity.getPersistentData().putDouble("cnt1", 20.0);
                            TechniqueRika2Procedure.execute(world, x, y, z, entity);
                        }
                    }

                    if (entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof YutaCullingGamesEntity) {
                        handleDomainLogic(entity, target);
                        domain = LogicConfilmDomainProcedure.execute(world, x, y, z, entity);
                    }

                    if (entity instanceof OkkotsuYutaEntity && LocateRikaProcedure.execute(world, entity)) {
                        PureLoveCannon = (entity instanceof LivingEntity _liv2 ? _liv2.getHealth() : -1.0F) <= (entity instanceof LivingEntity _liv3 ? _liv3.getMaxHealth() : -1.0F) * 0.2 || target.getPersistentData().getDouble("skill") % 100.0 == 15.0 && (target.getPersistentData().getDouble("skill") != 1815.0 || target.getPersistentData().getDouble("cnt9") >= 20.0 && target.getPersistentData().getDouble("cnt9") <= 25.0);
                    }

                    StrongEnemy = (target.hasEffect(MobEffects.DAMAGE_BOOST) ? target.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0) >= (entity instanceof LivingEntity _liv2 && _liv2.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv2.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0) * 0.5 || entity.getPersistentData().getDouble("cnt_target") > 600.0;
                    ResetCounterProcedure.execute(entity);
                    rnd = 0.0;

                    if (LogicStartProcedure.execute(entity) || domain) {
                        if (domain) {
                            entity.getPersistentData().putBoolean("flag_domain", false);
                            rnd = 20.0;
                            tick = 20.0;
                        } else if ((logicLocateRika || !StrongEnemy) && !PureLoveCannon) {
                            if (Math.random() > 0.75) {
                                rnd = 10.0;
                                tick = 75.0;
                            } else if (!AIDomainLogicProcedure.execute(world, x, y, z, entity) && Math.random() > 0.95 && (entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof YutaCullingGamesEntity) && StrongEnemy) {
                                rnd = 20.0;
                                tick = 20.0;
                            } else {
                                distance = GetDistanceProcedure.execute(entity);
                                rnd = decideNextAction(entity, distance, canUseCopy);
                                
                                if (rnd == 3.0) tick = 20.0;
                                else if (rnd == 5.0) tick = 250.0;
                                else if (rnd == 6.0) tick = 150.0;
                                else if (rnd == 7.0) tick = 100.0;
                                else if (rnd == 10.0) tick = 10.0;
                                else if (rnd == 106.0) { use_copy = true; tick = 50.0; }
                                else if (rnd == 0.0 && canUseCopy && (!(entity instanceof OkkotsuYutaEntity) || !(target instanceof GetoSuguruCurseUserEntity))) {
                                    use_copy = false;
                                    NUM_COPY = Math.floor(Math.random() * 4.0);
                                    for (int index1 = 0; index1 < 4; index1++) {
                                        ITEM1 = (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, (int) NUM_COPY)) : ItemStack.EMPTY).copy();
                                        if (ITEM1.getOrCreateTag().getDouble("skill") > 0.0) {
                                            use_copy = true;
                                            rnd = ITEM1.getOrCreateTag().getDouble("skill");
                                            tick = ITEM1.getOrCreateTag().getDouble("COOLDOWN_TICKS");
                                            break;
                                        }
                                        if (++NUM_COPY > 3.0) NUM_COPY = 0.0;
                                    }
                                }
                            }
                        } else {
                            if (entity instanceof LivingEntity _liv2 && !_liv2.level().isClientSide()) {
                                _liv2.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 19, false, false));
                            }
                            if (entity.getPersistentData().getDouble("friend_num") != 0.0) {
                                entity_rika = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("RIKA_UUID"));
                                if ((entity_rika instanceof RikaEntity || entity_rika instanceof Rika2Entity) && entity_rika instanceof LivingEntity _rika && !_rika.level().isClientSide()) {
                                    _rika.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 30, 1, false, false));
                                }
                            }
                            if (PureLoveCannon) {
                                rnd = 15.0;
                                tick = 500.0;
                            } else {
                                rnd = 10.0;
                                tick = 75.0;
                            }
                        }
                    }

                    // Sword Swap Logic (v43 + Helper)
                    Object[] swapResult = handleSwordSwapping(world, entity, rnd, tick, level);
                    rnd = (double) swapResult[0];
                    tick = (double) swapResult[1];
                    level = (double) swapResult[2];

                    if (rnd != 0.0) {
                        entity.getPersistentData().putDouble("skill", (double) Math.round(use_copy ? rnd : 500.0 + rnd));
                        if (entity.getPersistentData().getDouble("skill") == 4204.0) {
                            entity.getPersistentData().putBoolean("flag_weapon", true);
                        }
                        if (entity instanceof LivingEntity _liv2 && !_liv2.level().isClientSide()) {
                            _liv2.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                            MobEffect effect = (level > 0.0) ? (MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get() : (MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get();
                            _liv2.addEffect(new MobEffectInstance(effect, (int) tick / 2, 0, false, false));
                        }

                        if (rnd == 15.0) {
                            swapSwordToHead(world, entity);
                        }
                    } else if (PureLoveCannon) {
                        if (entity instanceof LivingEntity _liv2 && !_liv2.level().isClientSide()) {
                            _liv2.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 19, false, false));
                        }
                        entity.getPersistentData().putDouble("cnt_x", 5.0);
                    } else {
                        CalculateAttackProcedure.execute(world, entity);
                        swapSwordToHand(world, entity);
                    }
                }
            } else {
                entity.getPersistentData().putDouble("cnt_x", 0.0);
                entity.getPersistentData().putDouble("cnt_rika", entity.getPersistentData().getDouble("cnt_rika") + 1.0);
                if (entity.getPersistentData().getDouble("cnt_rika") % 200.0 == 190.0) {
                    if (LocateRikaProcedure.execute(world, entity)) {
                        entity.getPersistentData().putDouble("cnt1", 20.0);
                        TechniqueRika2Procedure.execute(world, x, y, z, entity);
                    }
                    swapSwordToHead(world, entity);
                }
            }

            if (entity.getPersistentData().getDouble("skill") == 0.0) {
                if (entity.getPersistentData().getBoolean("flag_weapon")) {
                    entity.getPersistentData().putBoolean("flag_weapon", false);
                    if ((entity instanceof LivingEntity _liv2 ? _liv2.getMainHandItem() : ItemStack.EMPTY).getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get()) {
                        runCommand(entity, "item replace entity @s weapon.mainhand with air");
                    }
                }
                if ((entity instanceof LivingEntity _liv2 ? _liv2.getMainHandItem() : ItemStack.EMPTY).getItem() == JujutsucraftModItems.LOUDSPEAKER.get()) {
                    runCommand(entity, "item replace entity @s weapon.mainhand with air");
                }
            }
        }
    }

    @Unique
    private static double decideNextAction(Entity entity, double distance, boolean canUseCopy) {
        if (Math.random() > 0.8) {
            for (int i = 0; i < 256; i++) {
                double r = (double)Math.round(Math.random() * 10.0);
                if (r == 3.0 && distance >= 6.0) continue;
                if (r == 5.0 && (!canUseCopy || Math.random() < 0.5)) continue;
                if (r == 6.0 && (!(entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof YutaCullingGamesEntity) || !canUseCopy || distance < 6.0)) continue;
                if (r == 7.0 && (!(entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof YutaCullingGamesEntity) || !canUseCopy || distance > 6.0)) continue;
                if (r == 10.0 && Math.random() < 0.5) continue;
                return r;
            }
        } else if ((entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof YutaCullingGamesEntity) && Math.random() > 0.8 && distance < 4.0 && canUseCopy) {
            return 106.0;
        }
        return 0.0;
    }

    @Unique
    private static void handleDomainLogic(Entity entity, LivingEntity target) {
        float health = ((LivingEntity)entity).getHealth();
        float maxHealth = ((LivingEntity)entity).getMaxHealth();
        int entityBoost = ((LivingEntity)entity).hasEffect(MobEffects.DAMAGE_BOOST) ? ((LivingEntity)entity).getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
        int targetBoost = target.hasEffect(MobEffects.DAMAGE_BOOST) ? target.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;

        boolean flag = health < maxHealth * 0.5;
        if (!flag && (target instanceof SukunaPerfectEntity || target instanceof SukunaFushiguroEntity)) {
            if (target.hasEffect((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get()) && entity.getPersistentData().getDouble("cnt_target") >= 100.0) {
                flag = true;
            }
        }
        entity.getPersistentData().putBoolean("flag_domain", flag && (entityBoost - 10 <= targetBoost));
    }

    @Unique
    private static Object[] handleSwordSwapping(LevelAccessor world, Entity entity, double rnd, double tick, double level) {
        LivingEntity _liv = (LivingEntity) entity;
        if (_liv.getMainHandItem().getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get() && !LogicCooldownCombatProcedure.execute(entity)) {
            if (_liv.getHealth() <= _liv.getMaxHealth() * 0.5 && !entity.getPersistentData().getBoolean("flag1")) {
                entity.getPersistentData().putBoolean("flag1", true);
                rnd = 3704.0;
                tick = 100.0;
                level = 1.0;
                if (_liv.getMainHandItem().getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get() && _liv.getItemBySlot(EquipmentSlot.HEAD).getItem() == ItemStack.EMPTY.getItem()) {
                    swapSwordToHead(world, entity);
                }
            }
        } else {
            entity.getPersistentData().putBoolean("flag1", false);
        }
        return new Object[]{rnd, tick, level};
    }

    @Unique
    private static void swapSwordToHead(LevelAccessor world, Entity entity) {
        LivingEntity _liv = (LivingEntity) entity;
        ItemStack mainHand = _liv.getMainHandItem();
        if (mainHand.getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get() && _liv.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            ItemStack swordCopy = mainHand.copy();
            runCommand(entity, "item replace entity @s armor.head with jujutsucraft:sword_okkotsu_yuta");
            ItemStack headItem = _liv.getItemBySlot(EquipmentSlot.HEAD);
            // Verify if the command was executed and sync NBT safely
            if (headItem.getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get()) {
                headItem.setTag(swordCopy.getTag() != null ? swordCopy.getTag().copy() : null);
            }
            runCommand(entity, "item replace entity @s weapon.mainhand with air");
        }
    }

    @Unique
    private static void swapSwordToHand(LevelAccessor world, Entity entity) {
        LivingEntity _liv = (LivingEntity) entity;
        ItemStack headItem = _liv.getItemBySlot(EquipmentSlot.HEAD);
        if (_liv.getMainHandItem().isEmpty() && headItem.getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get()) {
            ItemStack swordCopy = headItem.copy();
            runCommand(entity, "item replace entity @s weapon.mainhand with jujutsucraft:sword_okkotsu_yuta");
            ItemStack mainHand = _liv.getMainHandItem();
            // Verify if the command was executed and sync NBT safely
            if (mainHand.getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get()) {
                mainHand.setTag(swordCopy.getTag() != null ? swordCopy.getTag().copy() : null);
            }
            runCommand(entity, "item replace entity @s armor.head with air");
        }
    }

    @Unique
    private static void runCommand(Entity entity, String command) {
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            entity.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), entity.level() instanceof ServerLevel ? (ServerLevel)entity.level() : null, 4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity), command);
        }
    }
}
