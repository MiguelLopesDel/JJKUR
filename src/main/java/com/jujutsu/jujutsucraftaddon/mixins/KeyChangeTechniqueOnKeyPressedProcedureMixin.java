package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.FixPower;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

    @Inject(method = "execute", at = @At("TAIL"), remap = false, cancellable = true)
    private static void onExecuteTail(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        FixPower.execute(world, entity);

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(baseVars -> {
            entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                handleCombinedTailLogic(world, entity, baseVars, addonVars);
            });
        });
    }

    private static void handleCombinedTailLogic(LevelAccessor world, Entity entity,
                                                JujutsucraftModVariables.PlayerVariables baseVars,
                                                JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        LivingEntity living = (entity instanceof LivingEntity _ent) ? _ent : null;

        handleImbuedPowers(world, living, baseVars, addonVars);

        handleNamingCustomization(entity, baseVars, addonVars);

        handlePassiveBuffs(world, entity, living, baseVars, addonVars);

        handleKashimoMoves(entity, baseVars);
    }

    private static void handleImbuedPowers(LevelAccessor world, LivingEntity living,
                                           JujutsucraftModVariables.PlayerVariables baseVars,
                                           JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        if (living == null || living.level().isClientSide()) return;
        if (!world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OVERPOWERED_STUFF)) return;

        if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.GOJO && addonVars.Clans.equals("Gojo")) {
            if (!living.hasEffect(JujutsucraftaddonModMobEffects.GOJO_IMBUED_POWER.get())) {
                living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.GOJO_IMBUED_POWER.get(), -1, 1, false, false));
            }
        } else if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.SUKUNA && addonVars.Clans.equals("Sukuna")) {
            if (!living.hasEffect(JujutsucraftaddonModMobEffects.SUKUNA_POWERS.get())) {
                living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.SUKUNA_POWERS.get(), -1, 1, false, false));
            }
        }
    }

    private static void handleNamingCustomization(Entity entity,
                                                  JujutsucraftModVariables.PlayerVariables baseVars,
                                                  JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        String currentName = baseVars.PlayerSelectCurseTechniqueName;

        if (currentName.equals(Component.translatable("jujutsu.technique.purple").getString())) {
            if (addonVars.OutputLevel >= 5 && addonVars.Clans.equals("Gojo")) {
                baseVars.PlayerSelectCurseTechniqueName = entity.isShiftKeyDown() ?
                        Component.translatable("dialogueun").getString() : Component.translatable("dialoguepurple").getString();
                baseVars.syncPlayerVariables(entity);
            }
        }

        if (addonVars.Subrace.equals("Death Painting") && addonVars.Clans.equals("Itadori")) {
            if (entity instanceof ServerPlayer player) {
                boolean hasSoul = player.getAdvancements().getOrStartProgress(Objects.requireNonNull(player.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:soul_research")))).isDone();
                boolean hasEnchained = player.getAdvancements().getOrStartProgress(Objects.requireNonNull(player.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:enchained")))).isDone();

                if (hasSoul && hasEnchained) {
                    if (currentName.equals(Component.translatable("jujutsu.technique.malevolent_shrine").getString())) {
                        baseVars.PlayerSelectCurseTechniqueName = "Jujutsu Kaisen";
                    } else if (currentName.equals(Component.translatable("jujutsu.technique.dismantle").getString())) {
                        baseVars.PlayerSelectCurseTechniqueName = Component.translatable("dialoguesoul1").getString();
                    } else if (currentName.equals(Component.translatable("jujutsu.technique.cleave").getString())) {
                        baseVars.PlayerSelectCurseTechniqueName = Component.translatable("dialoguesoul2").getString();
                    }
                    baseVars.syncPlayerVariables(entity);
                }
            }
        }

        if (addonVars.InfusedDomain && currentName.equals(Component.translatable("jujutsu.technique.attack2").getString())) {
            String cloneName = "";
            if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.MAHITO) cloneName = "Mahito Clone";
            else if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.URAUME) cloneName = "Ice Clone";
            else if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.KUROURUSHI) cloneName = "Bug Clones";

            if (!cloneName.isEmpty()) {
                baseVars.PlayerSelectCurseTechniqueName = cloneName;
                baseVars.syncPlayerVariables(entity);
            }
        }
    }

    private static void handlePassiveBuffs(LevelAccessor world, Entity entity, LivingEntity living,
                                           JujutsucraftModVariables.PlayerVariables baseVars,
                                           JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        if (living == null || living.level().isClientSide()) return;

        if (world instanceof Level level && !level.isDay() && baseVars.PlayerCurseTechnique2 == TechniqueIDs.NANAMI) {
            if (!living.hasEffect(JujutsucraftaddonModMobEffects.OVERTIME.get())) {
                living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.OVERTIME.get(), 8400, 2, false, false));
            }
        }

        if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.MAKI) {
            if (!living.hasEffect(MobEffects.REGENERATION)) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 2, false, false));
            }
        } else if (entity.getPersistentData().getDouble("CursedSpirit") == 1 && entity.getPersistentData().getDouble("CurseUser") == 0) {
            if (!world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_CURSED_SPIRIT_RCT)) {
                int amp = addonVars.Subrace.equals("Disaster Curses") ? 3 : 2;
                if (!living.hasEffect(MobEffects.REGENERATION)) {
                    living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, amp, false, false));
                }
            }
        }
    }

    private static void handleKashimoMoves(Entity entity, JujutsucraftModVariables.PlayerVariables baseVars) {
        if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.KASHIMO && entity.isShiftKeyDown()) {
            if (baseVars.PlayerSelectCurseTechnique == 15.0) {
                baseVars.PlayerSelectCurseTechniqueName = Component.translatable("jujutsu.technique.kashimo_domain").getString();
                baseVars.PlayerSelectCurseTechniqueCost = 200.0;
                baseVars.PhysicalAttack = false;
                baseVars.PassiveTechnique = false;
                baseVars.PlayerSelectCurseTechnique = 20;
                baseVars.syncPlayerVariables(entity);
            }
        }
    }

    @Inject(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"), remap = false)
    private static void onExecuteReturnCost(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) return;

        living.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            if (living.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                int level = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_SUKUNA_LEVEL);
                capability.PlayerSelectCurseTechniqueCost = Math.round(capability.PlayerSelectCurseTechniqueCost * level / 10.0);
                capability.syncPlayerVariables(entity);
            }
            if (living.hasEffect(JujutsucraftModMobEffects.SIX_EYES.get())) {
                int level = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_SIX_EYES_LEVEL);
                capability.PlayerSelectCurseTechniqueCost = Math.round(capability.PlayerSelectCurseTechniqueCost * level);
                capability.syncPlayerVariables(entity);
            }
        });
    }
}
