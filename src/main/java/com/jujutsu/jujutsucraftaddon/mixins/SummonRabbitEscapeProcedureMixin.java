package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.RabbitDarknessProcedure;
import net.mcreator.jujutsucraft.entity.HoshiKiraraEntity;
import net.mcreator.jujutsucraft.entity.RabbitEscapeEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.DespawnTenShadowsTechniqueProcedure;
import net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.SummonRabbitEscapeProcedure;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = SummonRabbitEscapeProcedure.class, priority = -10000)
public abstract class SummonRabbitEscapeProcedureMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Adds custom RabbitDarkness effects and optimizes swarm synchronization.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        double x_pos, y_pos, z_pos, yaw, pitch, dis;
        entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);

        if (!(entity instanceof Player) && entity instanceof LivingEntity _liv && !world.isClientSide()) {
            _liv.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
        }

        if (entity.getPersistentData().getDouble("cnt1") == 1.0) {
            entity.getPersistentData().putDouble("cnt4", 0.025);
            DespawnTenShadowsTechniqueProcedure.execute(world, x, y, z, entity);
        }

        // 1. Animation Handling
        if (entity.getPersistentData().getDouble("cnt1") < 15.0) {
            if (entity instanceof LivingEntity _liv && _liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(20.0);
            }
            PlayAnimationProcedure.execute(world, entity);
        }

        // 2. Swarm Summoning Logic (Ticks 6 to 25)
        if (entity.getPersistentData().getDouble("cnt1") > 5.0 && entity.getPersistentData().getDouble("cnt1") < 26.0) {
            yaw = entity.getYRot();
            pitch = entity.getXRot();
            if (entity.getPersistentData().getDouble("friend_num") == 0.0) {
                entity.getPersistentData().putDouble("friend_num", Math.random());
            }

            for (int i = 0; i < 2; i++) {
                // Randomize spawn offset around player look direction
                float spawnYaw = (float) (yaw + Math.random() * 180.0 - 90.0);
                float spawnPitch = (float) (Math.random() * 22.5 - 11.25);
                dis = 1.0 + Math.random() * 5.0;

                ClipContext clip = new ClipContext(entity.getEyePosition(1.0F), entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(dis)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity);
                BlockPos targetPos = entity.level().clip(clip).getBlockPos();
                x_pos = targetPos.getX() + Math.random() - 0.5;
                y_pos = targetPos.getY() + Math.random() - 0.5;
                z_pos = targetPos.getZ() + Math.random() - 0.5;

                if (world instanceof ServerLevel _level) {
                    _level.sendParticles(ParticleTypes.SQUID_INK, x_pos, y_pos, z_pos, 10, 0.25, 0.25, 0.25, 0.0);
                    
                    Entity rabbit = ((EntityType) JujutsucraftModEntities.RABBIT_ESCAPE.get()).create(_level, null, null, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                    if (rabbit != null) {
                        rabbit.setYRot((float) yaw);
                        rabbit.setXRot((float) pitch);
                        
                        // v43 Smooth Rotation Sync
                        rabbit.setYBodyRot(rabbit.getYRot());
                        rabbit.setYHeadRot(rabbit.getYRot());
                        rabbit.yRotO = rabbit.getYRot();
                        rabbit.xRotO = rabbit.getXRot();
                        if (rabbit instanceof LivingEntity _livRabbit) {
                            _livValueRotation(_livRabbit, rabbit.getYRot());
                        }

                        // Rabbit Data Initialization
                        rabbit.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());
                        rabbit.getPersistentData().putDouble("friend_num2", entity.getPersistentData().getDouble("friend_num"));

                        boolean mastered = false;
                        if (entity instanceof ServerPlayer _sp) {
                            mastered = _sp.getAdvancements().getOrStartProgress(Objects.requireNonNull(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:skill_rabbit_escape")))).isDone();
                        }

                        if (entity.getPersistentData().getDouble("TenShadowsTechnique8") == 1.0 || mastered) {
                            rabbit.getPersistentData().putDouble("friend_num", entity.getPersistentData().getDouble("friend_num"));
                            rabbit.getPersistentData().putBoolean("Ambush", true);
                            rabbit.getPersistentData().putBoolean("Player", entity instanceof Player || entity.getPersistentData().getBoolean("Player"));
                            rabbit.getPersistentData().putBoolean("JujutsuSorcerer", entity.getPersistentData().getBoolean("JujutsuSorcerer"));
                            rabbit.getPersistentData().putBoolean("CurseUser", entity.getPersistentData().getBoolean("CurseUser"));
                        } else {
                            rabbit.getPersistentData().putString("TARGET_UUID", entity.getStringUUID());
                            rabbit.getPersistentData().putDouble("friend_num_worker", entity.getPersistentData().getDouble("friend_num"));
                        }

                        if (entity instanceof Player) {
                            rabbit.getPersistentData().putDouble("BaseCursePower", Math.max(Math.floor(entity.getPersistentData().getDouble("cnt10") / 40.0), 1.0));
                        }

                        _level.addFreshEntity(rabbit);
                    }
                }

                if (world instanceof Level _level && !_level.isClientSide()) {
                    _level.playSound(null, BlockPos.containing(x_pos, y_pos, z_pos), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.item.pickup")), SoundSource.NEUTRAL, 1.0F, 1.0F);
                }

                // Addon Feature: Darkness Logic
                RabbitDarknessProcedure.execute(world, x, y, z);
            }

            // Sync Summoner Rotation
            entity.setYRot(entity.getYRot());
            entity.setXRot(entity.getXRot());
            entity.setYBodyRot(entity.getYRot());
            entity.setYHeadRot(entity.getYRot());
            entity.yRotO = entity.getYRot();
            entity.xRotO = entity.getXRot();
            if (entity instanceof LivingEntity _liv) {
                _livValueRotation(_liv, entity.getYRot());
            }

            // Technique Completion
            if (entity.getPersistentData().getDouble("cnt1") >= 25.0) {
                entity.getPersistentData().putDouble("TenShadowsTechnique8", -1.0);
                entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                    cap.noChangeTechnique = true;
                    cap.syncPlayerVariables(entity);
                });
                KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
            }
        }

        // 3. Post-Summon Interactions (Kirara Support)
        if (entity.getPersistentData().getDouble("cnt1") > 35.0) {
            entity.getPersistentData().putDouble("skill", 0.0);
            Vec3 center = new Vec3(x, y, z);
            for (Entity found : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(16.0), e -> e instanceof HoshiKiraraEntity)) {
                found.lookAt(EntityAnchorArgument.Anchor.EYES, center);
                if (world instanceof ServerLevel _level) {
                    _level.sendParticles(ParticleTypes.HEART, found.getX(), found.getY() + found.getBbHeight(), found.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private static void _livValueRotation(LivingEntity entity, float yaw) {
        entity.yBodyRotO = yaw;
        entity.yHeadRotO = yaw;
    }
}
