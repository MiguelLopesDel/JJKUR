package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.GojoSatoruSchoolDaysEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyReverseCursedTechniqueOnKeyPressedProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = KeyReverseCursedTechniqueOnKeyPressedProcedure.class, priority = -10000)
public abstract class ReverseKeyMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43 with RCT progress system and Cursed Spirit support.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get())) {
            return;
        }

        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
                _player.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.dont_use").getString()), false);
            }
            return;
        }

        if (entity.getPersistentData().getDouble("skill") != 0.0) {
            return;
        }

        double level = -1.0;
        JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        // 1. Determine RCT Level
        if (entity.getPersistentData().getBoolean("CursedSpirit")) {
            level = 1.0;
        } else if (entity instanceof Player) {
            if (baseVars.PlayerCursePowerFormer > 150.0 && baseVars.PlayerCursePower >= 10.0) {
                boolean hasRCT2 = false;
                if (entity instanceof ServerPlayer _sp) {
                    hasRCT2 = _sp.getAdvancements().getOrStartProgress(Objects.requireNonNull(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:reverse_cursed_technique_2")))).isDone();
                }
                
                if (hasRCT2 || (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get()))) {
                    level = 1.0;
                } else {
                    boolean hasRCT1 = false;
                    if (entity instanceof ServerPlayer _sp) {
                        hasRCT1 = _sp.getAdvancements().getOrStartProgress(Objects.requireNonNull(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:reverse_cursed_technique_1")))).isDone();
                    }
                    if (hasRCT1) {
                        level = 0.0;
                    }
                }
            }
        } else {
            // v43 Logic: can_use tag or awakening or NBT flag
            boolean canUseBase = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_reverse_cursed_technique")));
            boolean isGojoAwakened = (entity instanceof GojoSatoruSchoolDaysEntity _gojo && (Boolean) _gojo.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking));
            boolean nbtCanUse = entity.getPersistentData().getBoolean("entity_can_use_rct");

            if (canUseBase || isGojoAwakened || nbtCanUse) {
                level = (entity instanceof LivingEntity _liv && _liv.getMaxHealth() > 800.0F) ? 1.0 : 0.0;
            }
        }

        // 2. Not Mastered Check
        if (level < 0.0) {
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
                _player.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.not_mastered").getString()), false);
            }
            return;
        }

        // 3. Zone Boost
        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.ZONE.get())) {
            level += 1.0 + _liv.getEffect((MobEffect) JujutsucraftModMobEffects.ZONE.get()).getAmplifier();
        }

        // 4. Pre-activation Cleanup
        if (entity instanceof LivingEntity _liv) {
            _liv.removeEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get());
        }
        entity.getPersistentData().putBoolean("PRESS_M", true);

        // 5. Activation Logic (JJKUR Custom Progress)
        if (entity.getPersistentData().getBoolean("CursedSpirit") || entity.getPersistentData().getDouble("CursedSpirit") == 1.0) {
            if (addonVars.rctspirit && entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, (int) (Math.round(level) * -1), true, true));
            }
        } else {
            long rctAmplifier;
            if (addonVars.RCTLimitLevel > 0) {
                rctAmplifier = Math.min(Math.round(addonVars.RCTCount / 5000.0), (long) addonVars.RCTLimitLevel);
                if (rctAmplifier == 0) rctAmplifier = 1;
            } else {
                rctAmplifier = Math.round(addonVars.RCTCount / 5000.0);
            }

            if (!entity.level().isClientSide() && entity.getServer() != null) {
                entity.getServer().getCommands().performPrefixedCommand(
                    new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), 
                    (ServerLevel) entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), 
                    entity.level().getServer(), entity),
                    "effect give @s jujutsucraft:reverse_cursed_technique infinite " + rctAmplifier + " true"
                );
            }
        }

        // 6. Final Effects
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            entity.getServer().getCommands().performPrefixedCommand(
                new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), 
                (ServerLevel) entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), 
                entity.level().getServer(), entity),
                "playsound ui.button.click master @s"
            );
        }
    }
}
