package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.procedures.IdleTransfigurationProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.RangeAttackProcedure;
import net.mcreator.jujutsucraft.procedures.ReturnEntitySizeProcedure;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IdleTransfigurationProcedure.class, priority = -10000)
public abstract class IdleTransfigurationProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        double cnt1 = entity.getPersistentData().getDouble("cnt1") + 1.0;
        entity.getPersistentData().putDouble("cnt1", cnt1);

        if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
            _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) entity.getPersistentData().getDouble("COOLDOWN_TICKS"), 0, false, false));
            // Addon: Stronger Slowdown (Level 5, 10 ticks)
            _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5, false, false));
        }

        Entity target = entity instanceof Mob _mob ? _mob.getTarget() : null;
        if (target instanceof LivingEntity) {
            // v43: Height 0.75
            entity.lookAt(Anchor.EYES, new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.75, target.getZ()));
        }

        double rangeSize = ReturnEntitySizeProcedure.execute(entity);
        double yaw = Math.toRadians(entity.getYRot() + 90.0F);
        double pitch = Math.toRadians(entity.getXRot());
        // v43: distance = 2.0 + width
        double distance = 2.0 + entity.getBbWidth();
        double x_pos = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * distance;
        double y_pos = entity.getY() + entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * distance;
        double z_pos = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * distance;

        if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_PURPLE.get(), x_pos, y_pos, z_pos, (int) (1.0 * rangeSize), 0.1 * rangeSize, 0.1 * rangeSize, 0.1 * rangeSize, 0.1);
        }

        if (entity instanceof LivingEntity _liv && _liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
            _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(618.0);
        }

        PlayAnimationProcedure.execute(world, entity);

        if (cnt1 == 1.0 && world instanceof Level _level) {
            SoundEvent sound = (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:slow_motion_end"));
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), sound, SoundSource.NEUTRAL, 1.0F, 1.2F);
            } else {
                _level.playLocalSound(x_pos, y_pos, z_pos, sound, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
            }
        }

        // Single-hit logic (Tick 6)
        if (cnt1 == 6.0) {
            if (entity instanceof LivingEntity _liv) {
                _liv.swing(InteractionHand.MAIN_HAND, true);
                if (!_liv.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.ATTACKING.get(), 1, 1, false, false));
                }
            }

            int addonLevel = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_IDLE_TRANSFIGURATION_LEVEL);
            entity.getPersistentData().putDouble("cnt2", entity.getPersistentData().getDouble("cnt2") + 1.0);
            
            // Corrected Math: 24.0 + (cnt2 * 0.1 * addonLevel)
            entity.getPersistentData().putDouble("Damage", 24.0 + (entity.getPersistentData().getDouble("cnt2") * 0.1 * addonLevel));
            // Corrected Range: distance * 2.5 (v43 base) * addonLevel
            entity.getPersistentData().putDouble("Range", distance * 2.5 * addonLevel);
            
            entity.getPersistentData().putDouble("effect", 2.0);
            entity.getPersistentData().putDouble("knockback", 0.1);
            entity.getPersistentData().putDouble("effect", 12.0);
            entity.getPersistentData().putDouble("effectConfirm", 2.0);
            entity.getPersistentData().putBoolean("swing", true);
            entity.getPersistentData().putBoolean("onlyLiving", true);
            
            RangeAttackProcedure.execute(world, x_pos, y_pos, z_pos, entity);
            
            if (world instanceof ServerLevel _level) {
                _level.sendParticles(ParticleTypes.ENCHANTED_HIT, x_pos, y_pos, z_pos, 15, 0.1, 0.1, 0.1, 4.0);
            }

            // End technique after attack
            entity.getPersistentData().putDouble("skill", 0.0);
        }

        if (cnt1 > 12.0) {
            if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) (entity.getPersistentData().getDouble("COOLDOWN_TICKS") * 2.0), 0, false, false));
            }
            entity.getPersistentData().putDouble("skill", 0.0);
        }
    }
}
