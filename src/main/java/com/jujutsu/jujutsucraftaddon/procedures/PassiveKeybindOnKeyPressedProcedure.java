package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.JujutsucraftMod;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SetupAnimationsProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.network.PacketDistributor;

public class PassiveKeybindOnKeyPressedProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            boolean opSukuna = world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA);
            double techniqueId;

            if (opSukuna) {
                techniqueId = vars.SecondTechnique ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
            } else {
                techniqueId = vars.PlayerCurseTechnique2;
            }
            
            if (techniqueId == TechniqueIDs.SUKUNA) {
                PassiveSukunaProcedure.execute(world, x, y, z, entity);
            } else if (techniqueId == TechniqueIDs.GOJO) {
                handleGojoPassive(entity);
            } else if (techniqueId == TechniqueIDs.MEGUMI) {
                handleMegumiPassive(world, x, y, z, entity);
            } else if (techniqueId == TechniqueIDs.TSUKUMO) {
                handleTsukumoPassive(world, x, y, z, entity);
            } else if (techniqueId == TechniqueIDs.KASHIMO) {
                PassiveKashimoProcedure.execute(world, entity);
            }

            double uroCheck = opSukuna ? (vars.SecondTechnique ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique) : vars.PlayerCurseTechnique;
            if (uroCheck == TechniqueIDs.URO) {
                handleUroLogic(entity);
            }
        });
    }

    private static void handleGojoPassive(Entity entity) {
        if (entity instanceof LivingEntity living && living.hasEffect(JujutsucraftModMobEffects.SIX_EYES.get())) {
            playAnimation(entity, "pressure");
            if (!living.hasEffect(JujutsucraftaddonModMobEffects.HWB.get()) && !entity.level().isClientSide()) {
                living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.HWB.get(), 40, 1, false, false));
            }
        }
    }

    private static void handleMegumiPassive(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (hasAdvancement(entity, "extension_technique")) {
            Buff2Procedure.execute(world, x, y, z, entity);
            sendActionMessage(entity, "Buffed Shikigamis");
        }
    }

    private static void handleTsukumoPassive(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (hasAdvancement(entity, "extension_technique")) {
            Buff2Procedure.execute(world, x, y, z, entity);
            sendActionMessage(entity, "Buffed!!");
        }
    }

    private static void handleUroLogic(Entity entity) {
        if (entity instanceof LivingEntity living) {
            if (!living.hasEffect(MobEffects.INVISIBILITY)) {
                sendActionMessage(entity, "Used Sky Manipulation To Hide Yourself");
                if (!entity.level().isClientSide()) {
                    living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 254, false, false));
                    living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.URO_SNEAKY.get(), -1, 254, false, false));
                }
            } else {
                sendActionMessage(entity, "Removed Sky Manipulation To Hide Yourself");
                living.removeEffect(MobEffects.INVISIBILITY);
                living.removeEffect(JujutsucraftaddonModMobEffects.URO_SNEAKY.get());
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

    private static boolean hasAdvancement(Entity entity, String name) {
        if (entity instanceof ServerPlayer player) {
            ResourceLocation advLoc = new ResourceLocation("jujutsucraftaddon:" + name);
            net.minecraft.advancements.Advancement adv = player.server.getAdvancements().getAdvancement(advLoc);
            return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
        }
        return false;
    }

    private static void sendActionMessage(Entity entity, String text) {
        if (entity instanceof Player player && !entity.level().isClientSide()) {
            player.displayClientMessage(Component.literal(text), false);
        }
    }
}
