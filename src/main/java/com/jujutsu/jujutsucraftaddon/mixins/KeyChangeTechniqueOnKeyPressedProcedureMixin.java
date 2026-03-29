package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.FixPower;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = KeyChangeTechniqueOnKeyPressedProcedure.class, priority = -10000)
public abstract class KeyChangeTechniqueOnKeyPressedProcedureMixin {

    /**
     * @author Satushi
     * @reason Consolidado para estender as técnicas do mod base e corrigir a desincronia visual de custos.
     */
    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) {
            ci.cancel();
            return;
        }

        // Execute power corrections from Addon
        FixPower.execute(world, entity);

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {

                // 1. Passive Effects (Nanami Overtime / Cursed Spirit Regeneration)
                applyPassiveEffects(world, entity, vars, addonVars);

                // 2. Imbued Power Logic (Gojo/Sukuna)
                applyImbuedPowers(world, entity, vars, addonVars);

                // 3. Dynamic Names and Dialogues
                updateTechniqueNames(entity, vars, addonVars);

                // 4. Infused Clones Logic
                if (addonVars.InfusedDomain && vars.PlayerSelectCurseTechniqueName.equals(net.minecraft.network.chat.Component.translatable("jujutsu.technique.attack2").getString())) {
                    if (vars.PlayerCurseTechnique2 == 15) vars.PlayerSelectCurseTechniqueName = "Mahito Clone";
                    else if (vars.PlayerCurseTechnique2 == 24) vars.PlayerSelectCurseTechniqueName = "Ice Clone";
                    else if (vars.PlayerCurseTechnique2 == 23) vars.PlayerSelectCurseTechniqueName = "Bug Clones";
                    // else if (vars.PlayerCurseTechnique2 == 14) vars.PlayerSelectCurseTechniqueName = "Wood Clone";
                    // else if (vars.PlayerCurseTechnique2 == 8) vars.PlayerSelectCurseTechniqueName = "Water Clone";
                }

                // 5. Kashimo Domain (Secret Ability)
                if (vars.PlayerCurseTechnique2 == 7.0 && entity.isShiftKeyDown() && vars.PlayerSelectCurseTechnique == 15.0) {
                    vars.PlayerSelectCurseTechniqueName = net.minecraft.network.chat.Component.translatable("jujutsu.technique.kashimo_domain").getString();
                    vars.PlayerSelectCurseTechniqueCost = 200.0;
                    vars.PlayerSelectCurseTechnique = 20;
                    vars.PhysicalAttack = false;
                    vars.PassiveTechnique = false;
                }

                // 6. COST CORRECTION
                if (entity instanceof LivingEntity living) {
                    // Apply JJKUR reduction Gamerules over updated cost
                    if (living.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                        double level = (double) world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_SUKUNA_LEVEL);
                        vars.PlayerSelectCurseTechniqueCost = Math.round(vars.PlayerSelectCurseTechniqueCost * level / 10.0);
                    }

                    if (living.hasEffect(JujutsucraftModMobEffects.SIX_EYES.get())) {
                        double level = (double) world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_SIX_EYES_LEVEL);
                        // Reverting to linear multiplication as per original logic [A]
                        vars.PlayerSelectCurseTechniqueCost = Math.round(vars.PlayerSelectCurseTechniqueCost * level);
                    }
                }

                vars.syncPlayerVariables(entity);
            });
        });
    }

    private static void applyPassiveEffects(LevelAccessor world, Entity entity, JujutsucraftModVariables.PlayerVariables vars, JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        // Overtime (Nanami)
        if (!(world instanceof Level _lvl && _lvl.isDay()) && vars.PlayerCurseTechnique2 == 13) {
            if (entity instanceof LivingEntity _liv && !_liv.hasEffect(JujutsucraftaddonModMobEffects.OVERTIME.get())) {
                _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.OVERTIME.get(), 8400, 2, false, false));
            }
        }

        // Passive Regeneration / RCT
        if (vars.PlayerCurseTechnique2 == -1) {
            if (entity instanceof LivingEntity _liv && !_liv.hasEffect(MobEffects.REGENERATION)) {
                _liv.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 2, false, false));
            }
        } else if (entity.getPersistentData().getDouble("CursedSpirit") == 1 && entity.getPersistentData().getDouble("CurseUser") == 0) {
            if (!world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_CURSED_SPIRIT_RCT)) {
                int amp = "Disaster Curses".equals(addonVars.Subrace) ? 3 : 2;
                if (entity instanceof LivingEntity _liv && !_liv.hasEffect(MobEffects.REGENERATION)) {
                    _liv.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, amp, false, false));
                }
            }
        }
    }

    private static void applyImbuedPowers(LevelAccessor world, Entity entity, JujutsucraftModVariables.PlayerVariables vars, JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        if (vars.PlayerCurseTechnique2 == 2 || vars.PlayerCurseTechnique == 2) {
            if (!(entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.GOJO_IMBUED_POWER.get())) && "Gojo".equals(addonVars.Clans)) {
                if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OVERPOWERED_STUFF)) {
                    if (entity instanceof LivingEntity _ent && !_ent.level().isClientSide())
                        _ent.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.GOJO_IMBUED_POWER.get(), -1, 1, false, false));
                }
            }
        } else if (vars.PlayerCurseTechnique2 == 1 || vars.PlayerCurseTechnique == 1) {
            if (!(entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.SUKUNA_POWERS.get())) && "Sukuna".equals(addonVars.Clans)) {
                if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OVERPOWERED_STUFF)) {
                    if (entity instanceof LivingEntity _ent && !_ent.level().isClientSide())
                        _ent.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.SUKUNA_POWERS.get(), -1, 1, false, false));
                }
            }
        }
    }

    private static void updateTechniqueNames(Entity entity, JujutsucraftModVariables.PlayerVariables vars, JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        String name = vars.PlayerSelectCurseTechniqueName;
        if (name.equals(net.minecraft.network.chat.Component.translatable("jujutsu.technique.purple").getString())) {
            if (addonVars.OutputLevel >= 5 && "Gojo".equals(addonVars.Clans)) {
                vars.PlayerSelectCurseTechniqueName = entity.isShiftKeyDown() ? net.minecraft.network.chat.Component.translatable("dialogueun").getString() : net.minecraft.network.chat.Component.translatable("dialoguepurple").getString();
            }
        }

        if ("Death Painting".equals(addonVars.Subrace) && "Itadori".equals(addonVars.Clans) && entity instanceof ServerPlayer _plr) {
            boolean soulRes = _plr.getAdvancements().getOrStartProgress(Objects.requireNonNull(_plr.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:soul_research")))).isDone();
            boolean enchained = _plr.getAdvancements().getOrStartProgress(Objects.requireNonNull(_plr.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:enchained")))).isDone();
            if (soulRes && enchained) {
                if (name.equals(net.minecraft.network.chat.Component.translatable("jujutsu.technique.malevolent_shrine").getString())) {
                    vars.PlayerSelectCurseTechniqueName = "Jujutsu Kaisen";
                } else if (name.equals(net.minecraft.network.chat.Component.translatable("jujutsu.technique.dismantle").getString())) {
                    vars.PlayerSelectCurseTechniqueName = net.minecraft.network.chat.Component.translatable("dialoguesoul1").getString();
                } else if (name.equals(net.minecraft.network.chat.Component.translatable("jujutsu.technique.cleave").getString())) {
                    vars.PlayerSelectCurseTechniqueName = net.minecraft.network.chat.Component.translatable("dialoguesoul2").getString();
                }
            }
        }
    }
}
