package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModBlocks;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AITrueSphereProcedure.class, priority = -10000)
public abstract class SphereYorozuMixin {

    /**
     * @author Satushi / Audit Correction
     * @reason Refactored for v43 with custom Sphere Size Limit via Gamerule.
     * FIXED: Restored movement smoothing (interpolation) and rotation sync for visual stability.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity.isAlive()) {
            if (entity instanceof LivingEntity _liv && !world.isClientSide()) {
                _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3, false, false));
            }

            if (LogicOwnerExistProcedure.execute(world, entity)) {
                Entity owner = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"));
                
                if (owner != null && entity.getPersistentData().getDouble("NameRanged_ranged") != 0.0 
                    && entity.getPersistentData().getDouble("NameRanged_ranged") == owner.getPersistentData().getDouble("NameRanged")) {
                    
                    if (entity instanceof LivingEntity _liv && _liv.getHealth() < _liv.getMaxHealth() && !_liv.hasEffect(MobEffects.REGENERATION)) {
                        if (!world.isClientSide()) _liv.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 51, 0, false, false));
                    }

                    if (owner instanceof LivingEntity _livOwner && _livOwner.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get())) {
                        killEntity(entity);
                    }
                    if (owner instanceof Player _player) {
                        JujutsucraftModVariables.PlayerVariables vars = _player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
                        if (vars.PlayerCursePower < 1.0) killEntity(entity);
                    }
                }

                // 1. Custom Size Limit Logic
                double limit = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_YOROZU_SPHERE_LIMIT);
                if (entity instanceof LivingEntity _liv && _liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.SIZE.get())) {
                    double currentSize = _liv.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).getBaseValue();
                    if (currentSize < limit) {
                        _liv.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).setBaseValue(Math.min(currentSize + 0.3, limit));
                    }
                }

                double px = entity.getX();
                double py = entity.getY();
                double pz = entity.getZ();

                // 2. Movement and Action Logic
                if (entity.getPersistentData().getDouble("move") == 0.0) {
                    if (owner != null && entity.getPersistentData().getDouble("NameRanged_ranged") == owner.getPersistentData().getDouble("NameRanged")) {
                        handleFollowLogic(entity, owner);
                    } else {
                        killEntity(entity);
                    }
                } else {
                    AITrueSphere1Procedure.execute(world, entity);
                    applySphereDamage(world, px, py, pz, entity);
                }
            } else {
                killEntity(entity);
            }
        } else {
            handleSphereDestruction(world, x, y, z, entity);
        }
    }

    private static void handleFollowLogic(Entity entity, Entity owner) {
        double cntX = entity.getPersistentData().getDouble("cnt_x");
        if (cntX == 0.0) {
            if (owner.getPersistentData().getDouble("skill") != 0.0 && owner.getPersistentData().getBoolean("attack")) {
                entity.getPersistentData().putDouble("cnt_x", 1.0);
            }
        } else if (cntX == 1.0) {
            if (owner.getPersistentData().getDouble("skill") == 0.0) {
                entity.getPersistentData().putDouble("cnt_x", 2.0);
            }
        } else {
            cntX += 1.0;
            entity.getPersistentData().putDouble("cnt_x", cntX);
            if (cntX > 10.0) {
                ResetCounterProcedure.execute(entity);
                entity.getPersistentData().putDouble("cnt_x", 0.0);
                double cntX2 = entity.getPersistentData().getDouble("cnt_x2") + 1.0;
                entity.getPersistentData().putDouble("cnt_x2", cntX2);
                entity.getPersistentData().putDouble("move", cntX2);
                
                if (!entity.level().isClientSide() && entity.getServer() != null) {
                    entity.getServer().getCommands().performPrefixedCommand(
                        new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), (ServerLevel)entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity),
                        "data merge entity @s {NoAI:1b}"
                    );
                }
            }
        }

        // CORRECTED: Restored Smoothing/Interpolation Logic
        double tx = owner.getX() + Math.cos(Math.toRadians(owner.getYRot() + 270.0)) * 8.0;
        double ty = owner.getY() + owner.getBbHeight() * 0.5 + 2.0;
        double tz = owner.getZ() + Math.sin(Math.toRadians(owner.getYRot() + 270.0)) * 8.0;

        double currentX = entity.getX();
        double currentY = entity.getY();
        double currentZ = entity.getZ();

        if (Math.abs(tx - currentX) > 0.5) tx = currentX + (currentX > tx ? -0.5 : 0.5);
        if (Math.abs(ty - currentY) > 0.5) ty = currentY + (currentY > ty ? -0.5 : 0.5);
        if (Math.abs(tz - currentZ) > 0.5) tz = currentZ + (currentZ > tz ? -0.5 : 0.5);

        entity.teleportTo(tx, ty, tz);
        if (entity instanceof ServerPlayer _sp) {
            _sp.connection.teleport(tx, ty, tz, entity.getYRot(), entity.getXRot());
        }

        entity.setDeltaMovement(owner.getDeltaMovement().x(), owner.onGround() ? 0.0 : owner.getDeltaMovement().y(), owner.getDeltaMovement().z());
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());
        
        // CORRECTED: Restored full Rotation Sync for Render
        entity.setYBodyRot(entity.getYRot());
        entity.setYHeadRot(entity.getYRot());
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
        if (entity instanceof LivingEntity _liv) {
            _liv.yBodyRotO = entity.getYRot();
            _liv.yHeadRotO = entity.getYRot();
        }
    }

    private static void applySphereDamage(LevelAccessor world, double x, double y, double z, Entity entity) {
        entity.getPersistentData().putDouble("Damage", 30.0);
        entity.getPersistentData().putDouble("Range", entity.getBbHeight() * 1.2);
        entity.getPersistentData().putDouble("knockback", 0.5);
        entity.getPersistentData().putDouble("projectile_type", 1.0);
        entity.getPersistentData().putBoolean("ignore", true);
        RangeAttackProcedure.execute(world, x, y, z, entity);

        entity.getPersistentData().putDouble("BlockRange", entity.getBbHeight() * 0.8);
        entity.getPersistentData().putDouble("BlockDamage", 99999.0);
        entity.getPersistentData().putBoolean("noParticle", true);
        entity.getPersistentData().putBoolean("noEffect", true);
        entity.getPersistentData().putBoolean("ExtinctionBlock", true);
        BlockDestroyAllDirectionProcedure.execute(world, x, y, z, entity);
    }

    private static void killEntity(Entity entity) {
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            entity.getServer().getCommands().performPrefixedCommand(
                new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), (ServerLevel)entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity),
                "kill @s"
            );
        }
    }

    private static void handleSphereDestruction(LevelAccessor world, double x, double y, double z, Entity entity) {
        world.levelEvent(2001, BlockPos.containing(x, y, z), Block.getId(((Block) JujutsucraftModBlocks.JUJUTSU_BARRIER.get()).defaultBlockState()));
        if (world instanceof Level _level && !_level.isClientSide()) {
            _level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:glass_crash")), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        if (world instanceof ServerLevel _server) {
            _server.sendParticles(JujutsucraftModParticleTypes.PARTICLE_BROKEN_GLASS.get(), x, y, z, (int)(ReturnEntitySizeProcedure.execute(entity) * 18.0), entity.getBbWidth() * 0.25, entity.getBbHeight() * 0.25, entity.getBbWidth() * 0.25, 0.25);
        }
        if (!entity.level().isClientSide()) entity.discard();
    }
}
