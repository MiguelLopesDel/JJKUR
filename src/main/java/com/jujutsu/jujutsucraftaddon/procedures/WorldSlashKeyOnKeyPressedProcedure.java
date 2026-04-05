package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModParticleTypes;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.JujutsucraftMod;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SetupAnimationsProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

public class WorldSlashKeyOnKeyPressedProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        if (addonVars.Clans.equals("Sukuna")) {
            handleSukunaToggle(world, x, y, z, entity, baseVars, addonVars);
        } else {
            handleStrongestOfHistoryLogic(world, x, y, z, entity, baseVars);
        }
    }

    private static void handleSukunaToggle(LevelAccessor world, double x, double y, double z, Entity entity, JujutsucraftModVariables.PlayerVariables base, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (!(entity instanceof ServerPlayer serverPlayer && world instanceof ServerLevel)) return;
        
        if (!hasAdvancement(serverPlayer, "world_slash_advancement")) return;
        if (!hasEffect(entity, JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) return;
        if (hasEffect(entity, JujutsucraftaddonModMobEffects.MANIFESTATION.get())) return;
        
        if (base.PlayerCurseTechnique2 == TechniqueIDs.SUKUNA || base.PlayerCurseTechnique2 == TechniqueIDs.MEGUMI) {
            if (addon.OutputLevel >= 5) {
                addon.WorldSlash = !addon.WorldSlash;
                
                if (addon.WorldSlash) {
                    applyEffect(entity, JujutsucraftaddonModMobEffects.WORLD_SLASH_EFFECT.get(), 3600, 1);
                    spawnParticles(world, x, y, z);
                    sendActionMessage(entity, Component.translatable("dialoguews").getString() + ": On");
                } else {
                    removeEffect(entity, JujutsucraftaddonModMobEffects.WORLD_SLASH_EFFECT.get());
                    sendActionMessage(entity, Component.translatable("dialoguews").getString() + ": Off");
                }
                
                addon.syncPlayerVariables(entity);
            }
        }
    }

    private static void handleStrongestOfHistoryLogic(LevelAccessor world, double x, double y, double z, Entity entity, JujutsucraftModVariables.PlayerVariables base) {
        if (!(entity instanceof ServerPlayer serverPlayer && world instanceof ServerLevel)) return;
        if (!hasAdvancement(serverPlayer, "sorcerer_strongest_of_history")) return;

        playSound(world, x, y, z, "jujutsucraft:wind_chime", 1.0f, 1.0f);
        entity.getPersistentData().putBoolean("PRESS_ULT", true);

        if (base.PlayerCurseTechnique == TechniqueIDs.JOGO) {
            playAnimation(entity, "fire1");
        } else if (base.PlayerCurseTechnique == TechniqueIDs.MEGUMI) {
            playAnimation(entity, "furubeyurayura");
            playSound(world, x, y, z, "jujutsucraftaddon:furubeult", 1.0f, 2.0f);
        }
    }

    private static void playAnimation(Entity entity, String animName) {
        if (!entity.level().isClientSide() && entity instanceof ServerPlayer sp) {
            SetupAnimationsProcedure.JujutsucraftModAnimationMessage msg = new SetupAnimationsProcedure.JujutsucraftModAnimationMessage(animName, entity.getId(), true);
            JujutsucraftMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> sp), msg);
            JujutsucraftMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
        }
    }

    private static void playSound(LevelAccessor world, double x, double y, double z, String path, float pitch, float volume) {
        if (world instanceof Level lvl) {
            var sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(path));
            if (sound == null) return;
            if (!lvl.isClientSide()) lvl.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, volume, pitch);
            else lvl.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, pitch, false);
        }
    }

    private static void spawnParticles(LevelAccessor world, double x, double y, double z) {
        if (world instanceof ServerLevel sLevel) {
            sLevel.sendParticles(JujutsucraftaddonModParticleTypes.THUNDER_WHITE.get(), x, y, z, 1, 3, 3, 3, 1);
        }
    }

    private static void applyEffect(Entity entity, net.minecraft.world.effect.MobEffect effect, int duration, int amplifier) {
        if (entity instanceof LivingEntity living && !entity.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
        }
    }

    private static void removeEffect(Entity entity, net.minecraft.world.effect.MobEffect effect) {
        if (entity instanceof LivingEntity living) {
            living.removeEffect(effect);
        }
    }

    private static boolean hasEffect(Entity entity, net.minecraft.world.effect.MobEffect effect) {
        return entity instanceof LivingEntity living && living.hasEffect(effect);
    }

    private static boolean hasAdvancement(ServerPlayer player, String name) {
        ResourceLocation advLoc = new ResourceLocation("jujutsucraftaddon:" + name);
        net.minecraft.advancements.Advancement adv = player.server.getAdvancements().getAdvancement(advLoc);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static void sendActionMessage(Entity entity, String text) {
        if (entity instanceof Player player && !entity.level().isClientSide()) {
            player.displayClientMessage(Component.literal(text), false);
        }
    }
}
