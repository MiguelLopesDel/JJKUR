package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

public class JJKURStartCursedTechniqueProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(baseVars -> entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(addonVars -> processExecution(world, x, y, z, entity, baseVars, addonVars)));
    }

    private static void processExecution(LevelAccessor world, double x, double y, double z, Entity entity,
                                         JujutsucraftModVariables.PlayerVariables baseVars,
                                         JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        LivingEntity living = (entity instanceof LivingEntity _ent) ? _ent : null;
        ItemStack mainHand = (living != null) ? living.getMainHandItem() : ItemStack.EMPTY;

        boolean isItemUse = false;
        ItemStack itemClone = ItemStack.EMPTY;
        if (mainHand.getOrCreateTag().getBoolean("used_item")) {
            mainHand.getOrCreateTag().putBoolean("used_item", false);
            itemClone = mainHand.copy();
            isItemUse = true;
        }

        String str1 = isItemUse ? itemClone.getDisplayName().getString() : baseVars.PlayerSelectCurseTechniqueName;
        double skillValue = isItemUse ? itemClone.getOrCreateTag().getDouble("skill") : 0;
        double skillCost = isItemUse ? itemClone.getOrCreateTag().getDouble("COOLDOWN_TICKS") : 0;
        boolean isYutaSword = isItemUse && itemClone.getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get();

        if (!isItemUse) {
            baseVars.noChangeTechnique = true;
            baseVars.syncPlayerVariables(entity);
            if (shouldTriggerTechniqueChange(str1, baseVars.PlayerSelectCurseTechnique)) {
                KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
                str1 = baseVars.PlayerSelectCurseTechniqueName;
            }
        }

        if (!canTechniqueStart(entity, living, isItemUse, isYutaSword)) {
            return;
        }

        if (isSpectator(entity)) return;

        boolean isCreative = isCreative(entity);
        double ceRequired = isYutaSword ? 0.0 : (isItemUse ? skillCost : baseVars.PlayerSelectCurseTechniqueCost);

        if (!isCreative && baseVars.PlayerCursePower < ceRequired) {
            if (entity instanceof Player p && !p.level().isClientSide()) {
                p.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.dont_use").getString()), true);
            }
            return;
        }

        if (!isCreative) {
            baseVars.PlayerCursePower -= ceRequired;
            baseVars.syncPlayerVariables(entity);
        }

        double t1, s1;
        if (isItemUse) {
            t1 = Math.round(Math.floor(skillValue / 100.0));
            s1 = Math.round(Math.floor(skillValue % 100.0));
        } else if (living != null && living.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get())) {
            t1 = TechniqueIDs.GOJO; s1 = 15;
        } else if (living != null && living.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get())) {
            t1 = TechniqueIDs.SUKUNA; s1 = 5;
        } else {
            if (baseVars.PlayerSelectCurseTechnique == 21.0) {
                t1 = baseVars.SecondTechnique ? baseVars.PlayerCurseTechnique2 : baseVars.PlayerCurseTechnique;
                s1 = 21.0;
            } else {
                double[] ids = mapIds(entity, str1, baseVars, addonVars, world, x, y, z);
                if (ids == null) return;
                t1 = ids[0];
                s1 = ids[1];
            }
            str1 = baseVars.PlayerSelectCurseTechniqueName;
        }

        if (str1.contains("Divine Meteor")) {
            entity.getPersistentData().putDouble("skill", 415.0);
            CloneMeteor.execute(world, x, y, z, entity);
        } else if (s1 == 21.0) {
            entity.getPersistentData().putDouble("skill", 21.0);
        } else {
            entity.getPersistentData().putDouble("skill", isItemUse ? skillValue : t1 * 100.0 + s1);
        }

        ResetCounterProcedure.execute(entity);
        if (living != null && !living.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
        }

        double tick = calculateCooldown(str1, t1, s1, isItemUse, skillCost, baseVars, addonVars, isCreative, entity);
        
        baseVars.PlayerTechniqueUsedNumber += Math.round(tick);
        baseVars.syncPlayerVariables(entity);

        if (world.getLevelData().getGameRules().getBoolean(JujutsucraftModGameRules.JUJUTSU_GAIN_FAME)) {
            checkMastery(entity, world, baseVars.PlayerTechniqueUsedNumber, t1, baseVars.PlayerCurseTechnique2);
        }

        finalizeExecution(entity, world, living, mainHand, tick, s1, baseVars, addonVars, isYutaSword, isItemUse);
    }

    private static boolean shouldTriggerTechniqueChange(String name, double id) {
        return !(name.contains(Component.translatable("jujutsu.technique.attack5").getString()) ||
                name.contains(Component.translatable("jujutsu.technique.attack4").getString()) ||
                name.contains(Component.translatable("jujutsu.technique.flying_kick").getString()) ||
                name.contains("Benevolent Shrine") || name.contains("Soul Dismantle") || name.contains("Soul Cleave") ||
                name.contains(Component.translatable("entity.jujutsucraft.nue_totality").getString()) ||
                name.contains("Rika Summon") || name.contains(Component.translatable("jujutsu.technique.attack7").getString()) ||
                name.contains(Component.translatable("jujutsu.technique.attack8").getString()) ||
                name.contains(Component.translatable("jujutsu.technique.mahito7").getString()) ||
                name.contains(Component.translatable("jujutsu.technique.attack3").getString()) ||
                name.contains(Component.translatable("jujutsu.technique.mahito_body_repel2").getString()) ||
                name.contains("Mahoraga: World Slash") || id == 21.0);
    }

    private static boolean canTechniqueStart(Entity entity, LivingEntity living, boolean isItemUse, boolean freePower) {
        if (isItemUse) {
            if (living == null) return true;
            boolean hasCT = living.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
            boolean hasCD = living.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get());
            boolean isUnstable = living.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get());
            if (hasCT) return false;
            return (!hasCD && !isUnstable) || freePower;
        }
        return LogicStartProcedure.execute(entity);
    }

    private static double[] mapIds(Entity entity, String name, JujutsucraftModVariables.PlayerVariables baseVars,
                                   JujutsucraftaddonModVariables.PlayerVariables addonVars, LevelAccessor world, double x, double y, double z) {
        double t1 = baseVars.SecondTechnique ? baseVars.PlayerCurseTechnique2 : baseVars.PlayerCurseTechnique;
        double s1 = baseVars.PlayerSelectCurseTechnique;

        if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.GETO && addonVars.InfusedDomain) {
            if (name.contains("Mammoth")) { t1 = 1; s1 = 50; }
            else if (name.contains("Slug")) { t1 = 1; s1 = 59; }
            else if (name.contains("Blindness")) {
                if (entity.isShiftKeyDown()) { t1 = 23; s1 = 5; } else { t1 = 1; s1 = Mth.nextInt(RandomSource.create(), 51, 52); }
            } else if (name.contains("Samurai")) {
                if (entity.isShiftKeyDown()) { t1 = 22; s1 = 4; } else { t1 = 15; s1 = 16; }
            } else if (name.contains("Tamamo-no-Mae Incarnate")) {
                if (entity.isShiftKeyDown()) { t1 = 1; s1 = 55; } else { t1 = 1; s1 = 56; }
            } else if (name.contains("Ganesha")) {
                if (!entity.isShiftKeyDown()) { t1 = 1; s1 = 60; }
            } else if (name.contains("Teruteru Bozu")) {
                if (!entity.isShiftKeyDown()) { t1 = 10; s1 = 5; }
            } else if (name.contains("Kuchisake-Onna")) {
                if (!entity.isShiftKeyDown()) { t1 = 1; s1 = 53; }
            } else if (name.contains("Forest")) {
                if (!entity.isShiftKeyDown()) { t1 = 1; s1 = 54; }
            } else if (name.contains("Jogo")) {
                t1 = TechniqueIDs.JOGO; s1 = entity.isShiftKeyDown() ? 7 : 5;
            } else if (name.contains("Mahito")) {
                t1 = TechniqueIDs.MAHITO; s1 = entity.isShiftKeyDown() ? 9 : 5;
            } else if (name.contains("Dagon")) {
                t1 = TechniqueIDs.DAGON; s1 = entity.isShiftKeyDown() ? 10 : 9;
            } else if (name.contains("Smallpox Deity")) {
                if (!entity.isShiftKeyDown()) { t1 = 12; s1 = 10; }
            } else if (name.contains("Zenin Naoya")) {
                t1 = TechniqueIDs.NAOYA; s1 = entity.isShiftKeyDown() ? 10 : 5;
            } else if (name.equals(Component.translatable("jujutsu.technique.attack1").getString())) {
                baseVars.PlayerSelectCurseTechniqueName = Component.translatable("jujutsu.technique.kaori1").getString();
                baseVars.syncPlayerVariables(entity);
                t1 = 41; s1 = 10;
            } else if (name.equals(Component.translatable("jujutsu.technique.attack2").getString())) {
                baseVars.PlayerSelectCurseTechniqueName = "Anti-Gravity Push";
                baseVars.syncPlayerVariables(entity);
                t1 = 1; s1 = 60;
            }
        } else {
            if (name.contains(Component.translatable("jujutsu.technique.kashimo_domain").getString())) { t1 = TechniqueIDs.KASHIMO; s1 = 20; }
            else if (name.contains(Component.translatable("jujutsu.technique.attack5").getString())) { t1 = -1; s1 = 7; }
            else if (name.contains(Component.translatable("jujutsu.technique.attack4").getString())) { t1 = -1; s1 = 6; }
            else if (name.contains(Component.translatable("jujutsu.technique.flying_kick").getString())) { t1 = 29; s1 = 4; }
            else if (name.contains("Benevolent Shrine")) { t1 = 1; s1 = 20; }
            else if (name.contains("Soul Dismantle")) { t1 = 1; s1 = 5; }
            else if (name.contains("Soul Cleave")) { t1 = 1; s1 = 6; }
            else if (name.contains("Rika Summon")) { t1 = 5; s1 = 10; }
            else if (name.contains(Component.translatable("jujutsu.technique.attack7").getString())) { t1 = 15; s1 = 4; }
            else if (name.contains(Component.translatable("jujutsu.technique.attack3").getString())) { t1 = baseVars.PlayerCurseTechnique; s1 = 2; }
            else if (name.contains(Component.translatable("jujutsu.technique.mahito7").getString())) { t1 = 15; s1 = 7; }
            else if (name.contains(Component.translatable("jujutsu.technique.attack8").getString())) { t1 = 15; s1 = 16; }
            else if (name.contains(Component.translatable("jujutsu.technique.mahito_body_repel2").getString())) { t1 = 15; s1 = 9; }
            else if (addonVars.InfusedDomain && (name.contains("divine_dog_white") || name.contains("divine_dog_black"))) { AttackBeastProcedure.execute(world, x, y, z, entity); return null; }
            else if (addonVars.InfusedDomain && name.contains("nue")) { t1 = TechniqueIDs.KASHIMO; s1 = 5; }
            else if (addonVars.InfusedDomain && name.contains("piercing_ox")) { AttackPiecingOxProcedure.execute(world, x, y, z, entity); return null; }
            else if (addonVars.InfusedDomain && name.contains("max_elephant")) { t1 = TechniqueIDs.DAGON; s1 = 5; }
        }
        return new double[]{t1, s1};
    }

    private static double calculateCooldown(String name, double t1, double s1, boolean itemUse, double itemCost,
                                            JujutsucraftModVariables.PlayerVariables baseVars,
                                            JujutsucraftaddonModVariables.PlayerVariables addonVars,
                                            boolean isCreative, Entity entity) {
        if (isCreative || (t1 == 2.0 && s1 == 5.0) || (t1 == 9.0 && s1 == 10.0) || (t1 == 18.0 && s1 >= 10.0 && s1 < 15.0)) return 1.0;
        if (s1 == 20.0) return 20.0;

        double tick = Math.max((itemUse ? itemCost : baseVars.PlayerSelectCurseTechniqueCostOrgin) / 2.0, 20.0);
        
        if (name.equals(Component.translatable("jujutsu.technique.attack1").getString())) tick = 5.0;
        else if (name.equals(Component.translatable("jujutsu.technique.attack2").getString())) tick = 15.0;
        else if (name.equals(Component.translatable("jujutsu.technique.attack3").getString())) tick = 20.0;
        else if (name.equals(Component.translatable("jujutsu.technique.attack4").getString())) tick = 100.0;
        else if (name.equals(Component.translatable("jujutsu.technique.attack5").getString())) tick = 200.0;
        else if (name.equals(Component.translatable("jujutsu.technique.attack6").getString()) || name.equals(Component.translatable("jujutsu.technique.flying_kick").getString())) tick = 100.0;
        else if (name.equals(Component.translatable("jujutsu.technique.attack7").getString())) tick = 100.0;

        if (t1 == 5.0 && s1 == 5.0) tick = 20.0;
        if (t1 == 6.0 && s1 >= 5.0 && s1 < 20.0 && !name.equals(Component.translatable("jujutsu.technique.choso3").getString())) tick = 5.0;
        if (t1 == 7.0 && (s1 == 5.0 || s1 == 10.0)) tick = 20.0;
        if (t1 == 19.0 && s1 < 5.0 && LogicStartPassiveProcedure.execute(entity)) tick = 5.0;
        if (t1 == 39.0 && s1 >= 8.0) tick = 20.0;
        if (t1 == 40.0 && (s1 == 6.0 || s1 == 7.0)) tick = 20.0;

        if (addonVars.Profession.equals("Sage") || addonVars.Subrace.equals("Disaster Curses")) tick *= 0.5;
        else if (addonVars.Profession.equals("Sorcerer")) tick *= 0.8;

        return tick;
    }

    private static void finalizeExecution(Entity entity, LevelAccessor world, LivingEntity living, ItemStack mainHand,
                                          double tick, double s1, JujutsucraftModVariables.PlayerVariables baseVars,
                                          JujutsucraftaddonModVariables.PlayerVariables addonVars,
                                          boolean freePower, boolean isItemUse) {
        if (mainHand.getItem() == JujutsucraftModItems.LOUDSPEAKER.get()) {
            mainHand.getOrCreateTag().putBoolean("Used", true);
        }

        if (freePower) {
            entity.getPersistentData().putDouble("COOLDOWN_TICKS", Math.max(entity.getPersistentData().getDouble("COOLDOWN_TICKS"), (double) Math.round(tick)));
        } else if (living != null && !living.level().isClientSide() && !world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_NO_COOLDOWN)) {
            if (baseVars.PhysicalAttack && !isItemUse) {
                if (s1 >= 0.0 && s1 <= 2.0) {
                    double duration = tick;
                    Item chest = living.getItemBySlot(EquipmentSlot.CHEST).getItem();
                    if (chest == JujutsucraftModItems.SUKUNA_BODY_CHESTPLATE.get()) duration *= 0.5;
                    if (living.getAttributes().hasAttribute(Attributes.ATTACK_SPEED)) {
                        duration += 20.0 * Math.max(1.7 - living.getAttribute(Attributes.ATTACK_SPEED).getValue(), 0.0);
                    }
                    living.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int) Math.round(duration), 1, false, false));
                } else {
                    living.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int) Math.round(tick / 2.0), 0, false, false));
                }
            } else {
                living.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) Math.round(tick), 0, false, false));
            }
        }

        if (!entity.getPersistentData().getBoolean("PRESS_Z") && addonVars.BurnOutRCT) {
            BurnoutKeyOnKeyPressedProcedure.execute(entity);
        }
    }

    private static boolean isSpectator(Entity entity) {
        if (entity instanceof ServerPlayer sp) return sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
        return false;
    }

    private static boolean isCreative(Entity entity) {
        if (entity instanceof ServerPlayer sp) return sp.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
        return false;
    }

    private static void checkMastery(Entity entity, LevelAccessor world, double usedNumber, double t1, double t2) {
        if (entity instanceof ServerPlayer player) {
            double difficulty = 1 + world.getLevelData().getGameRules().getInt(JujutsucraftModGameRules.JUJUTSUUPGRADEDIFFICULTY) / 10.0;
            if (usedNumber > (t1 != 27.0 && t2 != 27.0 ? 4000 : 2000) * difficulty) awardAdvancement(player, "jujutsucraft:mastery_simple_domain");
            if (usedNumber > (t1 != 27.0 && t2 != 27.0 ? 12000 : 100) * difficulty) awardAdvancement(player, "jujutsucraft:mastery_domain_expansion");
        }
    }

    private static void awardAdvancement(ServerPlayer player, String id) {
        Advancement adv = player.server.getAdvancements().getAdvancement(new ResourceLocation(id));
        if (adv != null) {
            AdvancementProgress ap = player.getAdvancements().getOrStartProgress(adv);
            if (!ap.isDone()) {
                for (String criteria : ap.getRemainingCriteria()) player.getAdvancements().award(adv, criteria);
            }
        }
    }
}
