package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.ItadoriShinjukuEntity;
import com.jujutsu.jujutsucraftaddon.entity.YutaCullingGamesEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.procedures.ReturnEnergyColorProcedure2;
import net.mcreator.jujutsucraft.entity.ItadoriYujiShinjukuEntity;
import net.mcreator.jujutsucraft.entity.OkkotsuYutaCullingGameEntity;
import net.mcreator.jujutsucraft.entity.OkkotsuYutaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.WhenEntityFallProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WhenEntityFallProcedure.class, priority = -10000)
public abstract class WhenEntityFallMixin {

    /**
     * @author Satushi / Rigorous Restoration
     * @reason Refactored for v43. RESTORED: Visual Explosion (0.0F), Dual Particle Layers, and 1:1 Visual Parity.
     * FIXED: Missing explosion logic and low particle density identified in audit.
     */
    @Inject(at = @At("HEAD"), method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;D)V", remap = false, cancellable = true)
    private static void execute(Event event, LevelAccessor world, Entity entity, double distance, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        boolean logicFall;
        double distancePower = distance - 8.0;
        double curseEnergyColor;
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        // 1. Logic Fall Determination (Full Addon Parity)
        if (entity instanceof Player _player) {
            JujutsucraftModVariables.PlayerVariables baseVars = _player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
            logicFall = _player.isShiftKeyDown() && baseVars.PlayerCursePowerMAX >= 6000.0;
        } else {
            logicFall = entity instanceof OkkotsuYutaEntity || entity instanceof OkkotsuYutaCullingGameEntity || 
                        entity instanceof YutaCullingGamesEntity || entity instanceof ItadoriShinjukuEntity || 
                        entity instanceof ItadoriYujiShinjukuEntity;
        }

        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
            logicFall = logicFall || !(entity instanceof Player) || entity.isShiftKeyDown();
        }

        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.JACKPOT.get())) {
            logicFall = true;
        }

        // Wukong Set Support
        ItemStack feetArmor = (entity instanceof LivingEntity _liv) ? _liv.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY;
        if (feetArmor.getItem() == JujutsucraftaddonModItems.WUKONG_SET_BOOTS.get().asItem()) {
            logicFall = true;
        }

        // 2. Impact Processing
        if (distancePower > 0.0) {
            distancePower = Math.sqrt(distancePower + 1.0);
            curseEnergyColor = ReturnEnergyColorProcedure2.execute(entity);

            if (distancePower > 4.0 || curseEnergyColor > 0.0) {
                entity.getPersistentData().putDouble("BlockRange", Math.min(distancePower, 4.0) + entity.getBbWidth());
                entity.getPersistentData().putDouble("BlockDamage", (curseEnergyColor > 0.0 ? 1.0 : 0.25) * distancePower);
                BlockDestroyAllDirectionProcedure.execute(world, x, y, z, entity);
            }

            if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 6, false, false));
            }

            // v43 Safeguard: Animation only if not in Technique
            if (!(entity instanceof LivingEntity _livCT && _livCT.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()))) {
                if (entity instanceof LivingEntity _livAnim && _livAnim.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                    _livAnim.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(-10.0);
                }
                PlayAnimationProcedure.execute(world, entity);
            }

            // 3. Impact Effects (Dual Layers + Visual Explosion)
            if (logicFall) {
                applyImpactEffectsRigor(world, entity, distancePower, curseEnergyColor, x, y, z);
                
                // Ring Ground Particle
                if (!entity.level().isClientSide() && entity.getServer() != null) {
                    CommandSourceStack stack = new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), (ServerLevel) world, 4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity);
                    entity.getServer().getCommands().performPrefixedCommand(stack, "particle jjkueffects:ring_ground");
                }
            }
        }

        // 4. Cancellation Logic
        if (logicFall && event != null && event.isCancelable()) {
            event.setCanceled(true);
        }

        double safeThreshold = 0;
        if (entity instanceof LivingEntity _liv && _liv.hasEffect(MobEffects.DAMAGE_BOOST)) {
            safeThreshold = (double) _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 5.0;
        }

        if (distance < safeThreshold && event != null && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    private static void applyImpactEffectsRigor(LevelAccessor world, Entity entity, double distancePower, double energyColor, double x, double y, double z) {
        if (!(world instanceof ServerLevel _level)) return;

        double particleAmount = Math.min(distancePower * distancePower + 20.0, 100.0);
        double particleSpeed = 0.5 + Math.min(distancePower * 0.25, 1.5);
        double bbWidth = entity.getBbWidth() * 0.25;

        SimpleParticleType pType = null;
        if (energyColor == 1.0) pType = (SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_BLUE.get();
        else if (energyColor == 2.0) pType = (SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_ORANGE.get();
        else if (energyColor == 3.0) pType = (SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_RED.get();
        else if (energyColor == 4.0) pType = (SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_GREEN.get();

        if (pType != null) {
            // RESTORED: DUAL LAYER PARTICLES (Wide + Narrow)
            _level.sendParticles(pType, x, y, z, (int) particleAmount, distancePower, 0.0, distancePower, particleSpeed * 0.1);
            _level.sendParticles(pType, x, y, z, (int) particleAmount, bbWidth, 0.0, bbWidth, particleSpeed);
        } else {
            _level.sendParticles(ParticleTypes.CLOUD, x, y, z, (int) (particleAmount * 0.25), distancePower, 0.0, distancePower, particleSpeed * 0.1);
            _level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, (int) (particleAmount * 0.25), bbWidth, 0.0, bbWidth, particleSpeed);
        }

        // RESTORED: Visual Explosion (0.0F Force)
        _level.explode(null, x, y, z, 0.0F, Level.ExplosionInteraction.NONE);

        // Audio with v43 variable pitch
        RandomSource random = _level.getRandom();
        if (energyColor > 0.0) {
            playSoundRigor(_level, x, y, z, "jujutsucraft:electric_shock", (float) (distancePower * 0.5), (float) Mth.nextDouble(random, 0.9, 1.1));
        }
        playSoundRigor(_level, x, y, z, "entity.generic.explode", (float) distancePower, (float) Mth.nextDouble(random, 0.5, 0.6));
        playSoundRigor(_level, x, y, z, "entity.zombie.break_wooden_door", (float) distancePower, (float) Mth.nextDouble(random, 0.5, 0.6));
    }

    private static void playSoundRigor(Level level, double x, double y, double z, String id, float vol, float pitch) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(id));
        if (sound == null) return;
        if (!level.isClientSide()) {
            level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, vol, pitch);
        } else {
            level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, vol, pitch, false);
        }
    }
}
