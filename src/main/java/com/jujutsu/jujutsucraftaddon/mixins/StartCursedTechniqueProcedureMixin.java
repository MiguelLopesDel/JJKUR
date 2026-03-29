package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.BurnoutKeyOnKeyPressedProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.CloneMeteor;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = StartCursedTechniqueProcedure.class, priority = -10000)
public abstract class StartCursedTechniqueProcedureMixin {

    /**
     * @author Satushi / FULL RESTORATION WITH DEBUG LOGS
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        LivingEntity _living = (entity instanceof LivingEntity _ent) ? _ent : null;
        JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        ItemStack mainHandItem = (_living != null) ? _living.getMainHandItem().copy() : ItemStack.EMPTY;
        ItemStack mainHandItemOrigin = (_living != null) ? _living.getMainHandItem() : ItemStack.EMPTY;
        boolean item_use = false;
        boolean noUseCursePower = false;
        String STR1 = "";
        double skill = 0.0;
        double cost = 0.0;
        double T1 = 0.0;
        double S1 = 0.0;
        double T2 = 0.0;
        double Tick = 0.0;

        // LOG INICIAL
        System.out.println("DEBUG JJKUR: StartCursedTechnique disparado.");
        System.out.println("DEBUG JJKUR: STR1 Base: " + baseVars.PlayerSelectCurseTechniqueName);
        System.out.println("DEBUG JJKUR: Select Number: " + baseVars.PlayerSelectCurseTechnique);

        // 1. Item Selection Logic
        if (mainHandItem.getOrCreateTag().contains("used_item") && mainHandItem.getOrCreateTag().getBoolean("used_item")) {
            mainHandItemOrigin.getOrCreateTag().putBoolean("used_item", false);
            item_use = true;
        }

        if (item_use) {
            STR1 = mainHandItem.getDisplayName().getString();
            skill = mainHandItem.getOrCreateTag().getDouble("skill");
            cost = mainHandItem.getOrCreateTag().getDouble("COOLDOWN_TICKS");
            if (mainHandItem.getItem() == JujutsucraftModItems.SWORD_OKKOTSU_YUTA.get()) {
                noUseCursePower = true;
            }
        } else {
            baseVars.noChangeTechnique = true;
            baseVars.syncPlayerVariables(entity);
            STR1 = baseVars.PlayerSelectCurseTechniqueName;

            // Technique Change Filter (FULL RESTORED)
            if (!(STR1.contains(Component.translatable("jujutsu.technique.attack5").getString()) ||
                  STR1.contains(Component.translatable("jujutsu.technique.attack4").getString()) ||
                  STR1.contains(Component.translatable("jujutsu.technique.flying_kick").getString()) ||
                  STR1.contains("Benevolent Shrine") || STR1.contains("Soul Dismantle") || STR1.contains("Soul Cleave") ||
                  STR1.contains(Component.translatable("entity.jujutsucraft.nue_totality").getString()) ||
                  STR1.contains("Rika Summon") || STR1.contains(Component.translatable("jujutsu.technique.attack7").getString()) ||
                  STR1.contains(Component.translatable("jujutsu.technique.attack8").getString()) ||
                  STR1.contains(Component.translatable("jujutsu.technique.mahito7").getString()) ||
                  STR1.contains(Component.translatable("jujutsu.technique.attack3").getString()) ||
                  STR1.contains(Component.translatable("jujutsu.technique.mahito_body_repel2").getString()) ||
                  STR1.contains("Mahoraga: World Slash"))) {

                KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
                // Re-fetch em caso de mudança
                STR1 = baseVars.PlayerSelectCurseTechniqueName;
            }
        }

        // 2. Start Logic Validation
        boolean canStart = false;
        if (item_use) {
            if (_living != null) {
                boolean hasCT = _living.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
                boolean hasCooldown = _living.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get());
                boolean isUnstable = _living.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get());
                canStart = !hasCT && (!hasCooldown && !isUnstable || noUseCursePower);
            }
        } else {
            canStart = LogicStartProcedure.execute(entity);
            System.out.println("DEBUG JJKUR: LogicStartProcedure result: " + canStart);
        }

        if (!canStart) {
            System.out.println("DEBUG JJKUR: Bloqueado pelo canStart (LogicStartProcedure).");
            return;
        }

        // 3. Spectator Check
        if (isSpectator(entity)) return;

        // 4. Energy Cost Calculation
        boolean isCreative = (entity instanceof Player _p && _p.getAbilities().instabuild);
        double requiredCE = item_use ? cost : baseVars.PlayerSelectCurseTechniqueCost;
        if (noUseCursePower) requiredCE = 0.0;

        if (baseVars.PlayerCursePower < requiredCE && !isCreative) {
            System.out.println("DEBUG JJKUR: Sem energia C amaldiçoada.");
            return;
        }

        if (!isCreative) {
            baseVars.PlayerCursePower -= requiredCE;
            baseVars.syncPlayerVariables(entity);
        }

        // 5. Technique Mapping (FULL RESTORATION)
        if (item_use) {
            T1 = Math.round(Math.floor(skill / 100.0));
            S1 = Math.round(Math.floor(skill % 100.0));
        } else if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get())) {
            T1 = 2; S1 = 15;
        } else if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get())) {
            T1 = 1; S1 = 5;
        } else {
            if (baseVars.PlayerCurseTechnique2 == 18.0 && addonVars.InfusedDomain) {
                // Infused Domain Mappings with Shift-Logic (FULL RESTORED)
                if (STR1.contains("Cursed Spirit Grade Semi 1 (Mammoth)")) { T1 = 1; S1 = 50; }
                else if (STR1.contains("Cursed Spirit Grade Semi 1 (Slug)")) { T1 = 1; S1 = 59; }
                else if (STR1.contains("Cursed Spirit Grade 1 (Blindness)")) {
                    if (entity.isShiftKeyDown()) { T1 = 23; S1 = 5; } else { T1 = 1; S1 = Mth.nextInt(RandomSource.create(), 51, 52); }
                } else if (STR1.contains("Samurai")) {
                    if (entity.isShiftKeyDown()) { T1 = 22; S1 = 4; } else { T1 = 15; S1 = 16; }
                } else if (STR1.contains("Tamamo-no-Mae Incarnate")) {
                    if (entity.isShiftKeyDown()) { T1 = 1; S1 = 55; } else { T1 = 1; S1 = 56; }
                } else if (STR1.contains("Ganesha")) {
                    if (!entity.isShiftKeyDown()) { T1 = 1; S1 = 60; }
                } else if (STR1.contains("Cursed Spirit Grade Semi 1 (Teruteru Bozu)")) {
                    if (!entity.isShiftKeyDown()) { T1 = 10; S1 = 5; }
                } else if (STR1.contains("Cursed Spirit Grade 1 (Kuchisake-Onna)")) {
                    if (!entity.isShiftKeyDown()) { T1 = 1; S1 = 53; }
                } else if (STR1.contains("Cursed Spirit Grade 1 (Forest)")) {
                    if (!entity.isShiftKeyDown()) { T1 = 1; S1 = 54; }
                } else if (STR1.contains("Jogo")) {
                    if (!entity.isShiftKeyDown()) { T1 = 4; S1 = 5; } else { T1 = 4; S1 = 7; }
                } else if (STR1.contains("Mahito")) {
                    if (!entity.isShiftKeyDown()) { T1 = 15; S1 = 5; } else { T1 = 15; S1 = 9; }
                } else if (STR1.contains("Dagon")) {
                    if (!entity.isShiftKeyDown()) { T1 = 8; S1 = 9; } else { T1 = 8; S1 = 10; }
                } else if (STR1.contains("Smallpox Deity")) {
                    if (!entity.isShiftKeyDown()) { T1 = 12; S1 = 10; }
                } else if (STR1.contains("Zenin Naoya")) {
                    if (!entity.isShiftKeyDown()) { T1 = 19; S1 = 5; } else { T1 = 19; S1 = 10; }
                } else if (STR1.equals(Component.translatable("jujutsu.technique.attack1").getString())) {
                    baseVars.PlayerSelectCurseTechniqueName = Component.translatable("jujutsu.technique.kaori1").getString();
                    baseVars.syncPlayerVariables(entity);
                    T1 = 41; S1 = 10;
                } else if (STR1.equals(Component.translatable("jujutsu.technique.attack2").getString())) {
                    baseVars.PlayerSelectCurseTechniqueName = "Anti-Gravity Push";
                    baseVars.syncPlayerVariables(entity);
                    T1 = 1; S1 = 60;
                }
                STR1 = baseVars.PlayerSelectCurseTechniqueName;
                T2 = baseVars.PlayerCurseTechnique2;
            } else {
                // Standard Addon Mappings (RESTORED)
                if (STR1.contains(Component.translatable("jujutsu.technique.kashimo_domain").getString())) { T1 = 7; S1 = 20; }
                else if (STR1.contains(Component.translatable("jujutsu.technique.attack5").getString())) { T1 = -1; S1 = 7; }
                else if (STR1.contains(Component.translatable("jujutsu.technique.attack4").getString())) { T1 = -1; S1 = 6; }
                else if (STR1.contains(Component.translatable("jujutsu.technique.flying_kick").getString())) { T1 = 29; S1 = 4; }
                else if (STR1.contains("Benevolent Shrine")) { T1 = 1; S1 = 20; }
                else if (STR1.contains("Soul Dismantle")) { T1 = 1; S1 = 5; }
                else if (STR1.contains("Soul Cleave")) { T1 = 1; S1 = 6; }
                else if (STR1.contains("Spider Web")) { T1 = 1; S1 = 10; }
                else if (STR1.contains("Rika Summon")) { T1 = 5; S1 = 10; }
                else if (STR1.contains(Component.translatable("jujutsu.technique.attack7").getString())) { T1 = 15; S1 = 4; }
                else if (STR1.contains(Component.translatable("jujutsu.technique.attack3").getString())) { T1 = baseVars.PlayerCurseTechnique; S1 = 2; }
                else if (STR1.contains(Component.translatable("jujutsu.technique.mahito7").getString())) { T1 = 15; S1 = 7; }
                else if (STR1.contains(Component.translatable("jujutsu.technique.attack8").getString())) { T1 = 15; S1 = 16; }
                else if (STR1.contains(Component.translatable("jujutsu.technique.mahito_body_repel2").getString())) { T1 = 15; S1 = 9; }
                else if (addonVars.InfusedDomain && (STR1.contains(Component.translatable("entity.jujutsucraft.divine_dog_white").getString()) || STR1.contains(Component.translatable("entity.jujutsucraft.divine_dog_black").getString()))) { AttackBeastProcedure.execute(world, x, y, z, entity); return; }
                else if (addonVars.InfusedDomain && STR1.contains(Component.translatable("entity.jujutsucraft.nue").getString())) { T1 = 7; S1 = 5; }
                else if (addonVars.InfusedDomain && STR1.contains(Component.translatable("entity.jujutsucraft.piercing_ox").getString())) { AttackPiecingOxProcedure.execute(world, x, y, z, entity); return; }
                else if (addonVars.InfusedDomain && STR1.contains(Component.translatable("entity.jujutsucraft.max_elephant").getString())) { T1 = 8; S1 = 5; }
                else {
                    STR1 = baseVars.PlayerSelectCurseTechniqueName;
                    T1 = baseVars.SecondTechnique ? baseVars.PlayerCurseTechnique2 : baseVars.PlayerCurseTechnique;
                    S1 = baseVars.PlayerSelectCurseTechnique;
                }
            }
        }

        System.out.println("DEBUG JJKUR: T1 final: " + T1 + " | S1 final: " + S1);
        System.out.println("DEBUG JJKUR: Skill Final NBT: " + (T1 * 100.0 + S1));

        // Divine Meteor Fix (RESTORED ID 415)
        if (STR1.contains("Divine Meteor")) {
            entity.getPersistentData().putDouble("skill", 415.0);
            CloneMeteor.execute(world, x, y, z, entity);
        } else {
            entity.getPersistentData().putDouble("skill", item_use ? skill : T1 * 100.0 + S1);
        }

        ResetCounterProcedure.execute(entity);
        if (_living != null && !_living.level().isClientSide()) {
            _living.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
        }

        // 6. Cooldown Calculation (FULL RESTORED)
        if ((T1 == 2.0 && S1 == 5.0) || (T1 == 9.0 && S1 == 10.0) || (T1 == 18.0 && S1 >= 10.0 && S1 < 15.0) || isCreative) {
            Tick = 1.0;
        } else {
            if (S1 == 20.0) {
                Tick = 20.0;
            } else {
                Tick = Math.max((item_use ? cost : baseVars.PlayerSelectCurseTechniqueCostOrgin) / 2.0, 20.0);
                if (STR1.equals(Component.translatable("jujutsu.technique.attack1").getString())) Tick = 5.0;
                else if (STR1.equals(Component.translatable("jujutsu.technique.attack2").getString())) Tick = 15.0;
                else if (STR1.equals(Component.translatable("jujutsu.technique.attack3").getString())) Tick = 20.0;
                else if (STR1.equals(Component.translatable("jujutsu.technique.attack4").getString())) Tick = 100.0;
                else if (STR1.equals(Component.translatable("jujutsu.technique.attack5").getString())) Tick = 200.0;
                else if (STR1.equals(Component.translatable("jujutsu.technique.attack6").getString()) || STR1.equals(Component.translatable("jujutsu.technique.flying_kick").getString())) Tick = 100.0;
                else if (STR1.equals(Component.translatable("jujutsu.technique.attack7").getString())) Tick = 100.0;

                if (T1 == 5.0 && S1 == 5.0) Tick = 20.0;
                if (T1 == 6.0 && S1 >= 5.0 && S1 < 20.0 && !STR1.equals(Component.translatable("jujutsu.technique.choso3").getString())) Tick = 5.0;
                if (T1 == 7.0 && (S1 == 5.0 || S1 == 10.0)) Tick = 20.0;
                if (T1 == 19.0 && S1 < 5.0 && LogicStartPassiveProcedure.execute(entity)) Tick = 5.0;
                if (T1 == 39.0 && S1 >= 8.0) Tick = 20.0;
                if (T1 == 40.0 && (S1 == 6.0 || S1 == 7.0)) Tick = 20.0;
            }
        }

        // 7. Profession & Subrace Buffs (RESTORED)
        if (addonVars.Profession.equals("Sage") || addonVars.Subrace.equals("Disaster Curses")) {
            Tick *= 0.5;
        } else if (addonVars.Profession.equals("Sorcerer")) {
            Tick *= 0.8;
        }

        baseVars.PlayerTechniqueUsedNumber += Math.round(Tick);
        baseVars.syncPlayerVariables(entity);

        // 8. Mastery and Fame
        if (world.getLevelData().getGameRules().getBoolean(JujutsucraftModGameRules.JUJUTSU_GAIN_FAME)) {
            checkMastery(entity, world, baseVars.PlayerTechniqueUsedNumber, T1, T2);
        }

        // 9. Final Cooldown & Loudspeaker Fix
        if (mainHandItemOrigin.getItem() == JujutsucraftModItems.LOUDSPEAKER.get()) {
            mainHandItemOrigin.getOrCreateTag().putBoolean("Used", true);
        }

        if (!noUseCursePower) {
            // Combat Cooldown System (RESTORED)
            if (baseVars.PhysicalAttack && !item_use) {
                if (S1 >= 0.0 && S1 <= 2.0) {
                    if ((_living != null ? _living.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getItem() == JujutsucraftModItems.SUKUNA_BODY_CHESTPLATE.get()) { Tick *= 0.5; }
                    if (_living != null && _living.getAttributes().hasAttribute(Attributes.ATTACK_SPEED)) {
                        Tick += 20.0 * Math.max(1.7 - _living.getAttribute(Attributes.ATTACK_SPEED).getValue(), 0.0);
                    }
                    if (_living != null && !_living.level().isClientSide() && !world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_NO_COOLDOWN)) {
                        _living.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int) Math.round(Tick), 1, false, false));
                    }
                } else if (_living != null && !_living.level().isClientSide() && !world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_NO_COOLDOWN)) {
                    _living.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int) Math.round(Tick / 2.0), 0, false, false));
                }
            } else if (_living != null && !_living.level().isClientSide() && !world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_NO_COOLDOWN)) {
                _living.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) Math.round(Tick), 0, false, false));
            }
        } else {
            entity.getPersistentData().putDouble("COOLDOWN_TICKS", Math.max(entity.getPersistentData().getDouble("COOLDOWN_TICKS"), (double) Math.round(Tick)));
        }

        if (!entity.getPersistentData().getBoolean("PRESS_Z") && addonVars.BurnOutRCT) {
            BurnoutKeyOnKeyPressedProcedure.execute(entity);
        }
    }

    private static boolean isSpectator(Entity entity) {
        if (entity instanceof ServerPlayer _sp) return _sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
        if (entity.level().isClientSide() && entity instanceof Player _p) {
            return Minecraft.getInstance().getConnection().getPlayerInfo(_p.getGameProfile().getId()) != null && 
                   Minecraft.getInstance().getConnection().getPlayerInfo(_p.getGameProfile().getId()).getGameMode() == GameType.SPECTATOR;
        }
        return false;
    }

    private static void checkMastery(Entity entity, LevelAccessor world, double usedNumber, double T1, double T2) {
        if (entity instanceof ServerPlayer _player) {
            double difficulty = 1 + world.getLevelData().getGameRules().getInt(JujutsucraftModGameRules.JUJUTSUUPGRADEDIFFICULTY) / 10.0;
            if (usedNumber > (T1 != 27.0 && T2 != 27.0 ? 4000 : 2000) * difficulty) awardAdvancement(_player, "jujutsucraft:mastery_simple_domain");
            if (usedNumber > (T1 != 27.0 && T2 != 27.0 ? 12000 : 100) * difficulty) awardAdvancement(_player, "jujutsucraft:mastery_domain_expansion");
        }
    }

    private static void awardAdvancement(ServerPlayer _player, String id) {
        Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation(id));
        if (_adv != null) {
            AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
            if (!_ap.isDone()) {
                for (String criteria : _ap.getRemainingCriteria()) _player.getAdvancements().award(_adv, criteria);
            }
        }
    }
}
