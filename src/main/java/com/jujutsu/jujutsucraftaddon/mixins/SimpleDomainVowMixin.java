package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.UiUiEntity;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.LogicSimpleDomainProcedure;
import net.mcreator.jujutsucraft.procedures.SimpleDomainEffectStartedappliedProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = SimpleDomainEffectStartedappliedProcedure.class, priority = -10000)
public abstract class SimpleDomainVowMixin {

    /**
     * @author Satushi / Audit Correction
     * @reason Refactored for v43 with JJKUR quest progression, range upgrades, and projectile defense.
     * FIXED: Added amplifier > 0 check to the main condition for parity with base mod.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        // CORRECTED: Main condition now checks for amplifier > 0 as required by v43 and Addon Original logic
        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) 
            && _liv.getEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getAmplifier() > 0) {
            
            if (!LogicSimpleDomainProcedure.execute()) return;

            int duration = _liv.getEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getDuration();
            double baseRadius = entity.getBbWidth() + 0.025 * duration;
            baseRadius = Math.min(baseRadius, entity.getPersistentData().getDouble("skill") == 3105.0 ? 16.0 : 4.0);

            JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

            // 1. Particle Rendering (v43 Randomization + JJKUR Level Multiplier)
            double currentAngle;
            double radiusMultiplier = (addonVars.SimpleDomainLevel >= 3.0) ? addonVars.SimpleDomainLevel : 1.0;
            double finalRadius = baseRadius * radiusMultiplier;

            for (int i = 0; i < 72; i++) {
                currentAngle = Math.toRadians(Math.random() * 360.0);
                double px = x + Math.sin(currentAngle) * finalRadius;
                double pz = z + Math.cos(currentAngle) * finalRadius;

                if (world instanceof ServerLevel _level) {
                    if (!addonVars.Effects) {
                        _level.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, new Vec3(px, y, pz), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
                            "particle dust 0.749 0.984 1.000 1 ~ ~ ~ 0 0 0 1 1 force"
                        );
                    } else if (Math.random() < 0.05) {
                        String particle = (addonVars.SimpleDomainLevel >= 3.0) ? "jjkueffects:simple_domain_2" : "jjkueffects:simple_domain";
                        _level.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), _level, 4, entity.getName().getString(), entity.getDisplayName(), _level.getServer(), entity),
                            "particle " + particle
                        );
                    }
                }
            }

            // 2. Perfect Simple Domain: Projectile Defense
            if (entity instanceof ServerPlayer _sp) {
                boolean isPerfect = _sp.getAdvancements().getOrStartProgress(Objects.requireNonNull(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:perfect_simple_domain")))).isDone();
                if (isPerfect) {
                    Vec3 center = new Vec3(x, y, z);
                    for (Entity target : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(3.5), e -> true)) {
                        if (target != entity) {
                            boolean isAmmo = target.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) || 
                                             target.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo_no_move")));
                            
                            if (isAmmo && Math.random() <= 0.1) {
                                if (!target.level().isClientSide()) {
                                    target.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_curse")))), 
                                        Mth.nextInt(RandomSource.create(), 10, 50));
                                }
                            }
                        }
                    }
                }
            }

            // 3. Ui Ui Support
            JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables());
            if (baseVars.PlayerCurseTechnique2 == 11.0) {
                Vec3 center = new Vec3(x, y, z);
                for (Entity found : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(50.0), e -> true)) {
                    if (found instanceof UiUiEntity && (found.getPersistentData().getString("OWNER_UUID")).equals(entity.getStringUUID())) {
                        found.teleportTo(x, y, z);
                        if (found instanceof ServerPlayer _sp) {
                            _sp.connection.teleport(x, y, z, found.getYRot(), found.getXRot());
                        }
                        if (found instanceof LivingEntity _livUi && !found.level().isClientSide()) {
                            int currentAmp = _livUi.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) ? _livUi.getEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getAmplifier() : 0;
                            _livUi.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), 400, currentAmp + 3, false, false));
                        }
                    }
                }
            }

            // 4. Quest Progression
            if (addonVars.SimpleQuest >= 0.0) {
                double progress = entity.getPersistentData().getDouble("cnt_simpledomain");
                if (progress < 10000.0) {
                    entity.getPersistentData().putDouble("cnt_simpledomain", progress + 1.0);
                } else if (progress >= 10000.0) {
                    if (entity instanceof ServerPlayer _sp) {
                        Advancement adv = _sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:perfect_simple_domain"));
                        AdvancementProgress ap = _sp.getAdvancements().getOrStartProgress(adv);
                        if (!ap.isDone()) {
                            for (String criteria : ap.getRemainingCriteria()) {
                                _sp.getAdvancements().award(adv, criteria);
                            }
                        }
                    }
                    entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                        cap.SimpleDomainLevel = 3.0;
                        cap.SimpleQuest = 5.0;
                        cap.syncPlayerVariables(entity);
                    });
                }
            }
        }
    }
}
