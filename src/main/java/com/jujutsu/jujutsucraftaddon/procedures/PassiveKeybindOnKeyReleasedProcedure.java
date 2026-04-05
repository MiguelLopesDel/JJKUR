package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.JujutsucraftMod;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SetupAnimationsProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.network.PacketDistributor;

public class PassiveKeybindOnKeyReleasedProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        NahProcedure.execute(world, x, y, z, entity);

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(baseVars -> {
            entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.SUKUNA) {
                    handleSukunaLogic(world, entity, baseVars, addonVars);
                } else if (baseVars.PlayerCurseTechnique == TechniqueIDs.GOJO) {
                    handleGojoLogic(world, x, y, z, entity, baseVars, addonVars);
                }
            });
        });
    }

    private static void handleSukunaLogic(LevelAccessor world, Entity entity, JujutsucraftModVariables.PlayerVariables base, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (!(entity instanceof LivingEntity living) || living.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get())) return;
        if (base.BodyItem.getCount() < 15.0 || !addon.Clans.equals("Sukuna")) return;

        if (addon.OutputLevel > 0) {
            if (addon.Moveset > 0) {
                if (!entity.level().isClientSide()) {
                    living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.WORLD_CUT.get(), 40, 1, false, false));
                }
                if (addon.Moveset == 4) {
                    playAnimation(entity, "worldslash" + Mth.nextInt(RandomSource.create(), 1, 4));
                }
            }
        } else {
            boolean conditionMet = false;
            if (isSurvival(entity)) {
                if (living.getHealth() <= 60 && entity.getPersistentData().getDouble("brokenBrain") == 2 && base.PlayerCursePower <= 300) {
                    conditionMet = true;
                }
            } else {
                conditionMet = true;
            }

            if (conditionMet && living.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                if (hasAdvancements(entity, "world_slash_advancement", "enchained", "grade_yin_yang", "ultimate_power", "barrierless_domain_perfected")) {
                    if (base.BodyItem.getCount() >= 19.0 && !living.hasEffect(JujutsucraftaddonModMobEffects.HEIAN_FORM.get())) {
                        playAnimation(entity, "heianform");
                        if (!entity.level().isClientSide()) {
                            living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.HEIAN_FORM.get(), -1, 1, false, false));
                        }
                    }
                }
            }
        }
    }

    private static void handleGojoLogic(LevelAccessor world, double x, double y, double z, Entity entity, JujutsucraftModVariables.PlayerVariables base, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (!(entity instanceof LivingEntity living)) return;
        if (!addon.Clans.equals("Gojo") || living.hasEffect(JujutsucraftaddonModMobEffects.WORLD_GOJO.get())) return;
        if (!living.hasEffect(JujutsucraftModMobEffects.SIX_EYES.get())) return;
        if (!hasAdvancement(entity, "gojo_training_part_4")) return;

        if (addon.OutputLevel > 0) {
            switch ((int) addon.Moveset) {
                case 1 -> {
                    if (!entity.level().isClientSide()) living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.WORLD_GOJO.get(), entity.isShiftKeyDown() ? 40 : 20, 1, false, false));
                    playAnimation(entity, entity.isShiftKeyDown() ? "blueenchanted" : "blue" + Mth.nextInt(RandomSource.create(), 1, 2));
                }
                case 2 -> {
                    if (!entity.level().isClientSide()) living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.WORLD_GOJO.get(), 60, 1, false, false));
                    playAnimation(entity, entity.isShiftKeyDown() ? "redunlimited" : "redskill" + Mth.nextInt(RandomSource.create(), 2, 4));
                }
                case 3 -> {
                    if (addon.OutputLevel >= 4) {
                        if (!entity.level().isClientSide()) {
                            living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.WORLD_GOJO.get(), 240, 1, false, false));
                            if (!living.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get())) {
                                living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get(), 240, 1, false, false));
                            }
                        }
                        playAnimation(entity, "murasaki");
                        executeParticleCommand(entity, "particle jjkueffects:red_and_blue ~ ~1 ~ 0 0 0 1 1 force");
                    }
                }
                case 4 -> {
                    if (addon.OutputLevel >= 4 && living.hasEffect(JujutsucraftModMobEffects.ZONE.get()) && living.getHealth() <= living.getMaxHealth() / 2) {
                        if (!entity.level().isClientSide()) {
                            living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.WORLD_GOJO.get(), 240, 1, false, false));
                        }
                        playAnimation(entity, "murasaki2");
                        UnlimitedPurpleProcedure.execute(world, x, y, z, entity);
                    }
                }
                case 5 -> {
                    if (addon.OutputLevel >= 4) {
                        if (!entity.level().isClientSide()) living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.WORLD_GOJO.get(), 30, 1, false, false));
                        playAnimation(entity, "bluebarrage");
                    }
                }
            }
        } else {
            boolean conditionMet = false;
            if (isSurvival(entity)) {
                if (living.getHealth() <= 60 && entity.getPersistentData().getDouble("brokenBrain") == 2 && base.PlayerCursePower <= 300) {
                    conditionMet = true;
                }
            } else {
                conditionMet = true;
            }

            if (conditionMet && hasAdvancements(entity, "gojo_training_part_4", "grade_yin_yang", "last_hope", "barrierless_domain_perfected")) {
                if (!living.hasEffect(JujutsucraftaddonModMobEffects.GOJO_AWAKENING_1.get())) {
                    playAnimation(entity, "awakeninggojo");
                    if (!entity.level().isClientSide()) {
                        living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.GOJO_AWAKENING_1.get(), -1, 1, false, false));
                    }
                }
            }
        }
    }

    private static void playAnimation(Entity entity, String animName) {
        if (!entity.level().isClientSide() && entity instanceof ServerPlayer sp) {
            SetupAnimationsProcedure.JujutsucraftModAnimationMessage msg = new SetupAnimationsProcedure.JujutsucraftModAnimationMessage(animName, entity.getId(), true);
            JujutsucraftMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> sp), msg);
            JujutsucraftMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
        }
    }

    private static boolean hasAdvancements(Entity entity, String... names) {
        for (String name : names) {
            if (!hasAdvancement(entity, name)) return false;
        }
        return true;
    }

    private static boolean hasAdvancement(Entity entity, String name) {
        if (entity instanceof ServerPlayer player) {
            ResourceLocation advLoc = new ResourceLocation("jujutsucraftaddon:" + name);
            net.minecraft.advancements.Advancement adv = player.server.getAdvancements().getAdvancement(advLoc);
            return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
        }
        return false;
    }

    private static boolean isSurvival(Entity entity) {
        if (entity instanceof ServerPlayer sp) return sp.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
        return false;
    }

    private static void executeParticleCommand(Entity entity, String command) {
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            CommandSourceStack stack = new CommandSourceStack(
                CommandSource.NULL, entity.position(), entity.getRotationVector(),
                (ServerLevel) entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), 
                entity.level().getServer(), entity
            );
            entity.getServer().getCommands().performPrefixedCommand(stack, command);
        }
    }
}
