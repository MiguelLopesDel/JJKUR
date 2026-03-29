package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.YutaCullingGamesEntity;
import com.jujutsu.jujutsucraftaddon.procedures.LocatePartialProcedure;
import net.mcreator.jujutsucraft.entity.OkkotsuYutaCullingGameEntity;
import net.mcreator.jujutsucraft.entity.Rika2Entity;
import net.mcreator.jujutsucraft.entity.RikaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = TechniqueRika1Procedure.class, priority = -10000)
public abstract class TechniqueRika1ProcedureMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Adds support for YutaCullingGamesEntity and LocatePartial.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        double x_pos, y_pos, z_pos, yaw, pitch, dis;
        boolean summon, noControl;
        
        entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);

        // 1. Determine Control and Unlocked State (v43 Logic)
        noControl = entity instanceof Player ? 
            !(entity instanceof ServerPlayer _sp && _sp.server != null && _sp.getAdvancements().getOrStartProgress(Objects.requireNonNull(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:skill_rika_control")))).isDone()) :
            !(entity instanceof Player) && (entity instanceof LivingEntity _liv && _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0) < 10;

        if (entity.getPersistentData().getDouble("cnt1") == 1.0) {
            // v43 Unlocked state (cnt3)
            if (entity instanceof Player ? 
                (entity instanceof ServerPlayer _sp && _sp.server != null && _sp.getAdvancements().getOrStartProgress(Objects.requireNonNull(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:skill_curseis_lifted")))).isDone()) :
                (entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof YutaCullingGamesEntity)) { // Addon support
                entity.getPersistentData().putDouble("cnt3", 1.0);
            }

            // Addon specific: LocatePartial support
            if (LocatePartialProcedure.execute(world, entity)) {
                entity.getPersistentData().putDouble("cnt4", 1.0);
            } else {
                entity.getPersistentData().putDouble("cnt4", 0.0);
                entity.getPersistentData().putDouble("cnt2", 1.0);
                
                LivingEntity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;
                if (target != null) {
                    int targetBoost = target.hasEffect(MobEffects.DAMAGE_BOOST) ? target.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                    int ownerBoost = (entity instanceof LivingEntity _liv) && _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                    if (targetBoost <= (double) ownerBoost * 0.5) {
                        entity.getPersistentData().putDouble("cnt2", 0.0);
                    }
                }

                if (noControl || entity.isShiftKeyDown()) {
                    entity.getPersistentData().putDouble("cnt2", 0.0);
                }
            }

            // Position calculation
            LivingEntity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;
            if (target != null) {
                ClipContext clip = new ClipContext(target.getEyePosition(1.0F), target.getEyePosition(1.0F).add(target.getViewVector(1.0F).scale(-5.0)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, target);
                BlockPos pos = target.level().clip(clip).getBlockPos();
                x_pos = pos.getX();
                y_pos = pos.getY();
                z_pos = pos.getZ();
                yaw = target.getYRot();
                pitch = target.getXRot();
            } else {
                ClipContext clip = new ClipContext(entity.getEyePosition(1.0F), entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(32.0)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity);
                BlockPos pos = entity.level().clip(clip).getBlockPos();
                x_pos = pos.getX();
                y_pos = pos.getY();
                z_pos = pos.getZ();
                yaw = entity.getYRot() + 180.0F;
                pitch = entity.getXRot() * -1.0F;
            }

            entity.getPersistentData().putDouble("x_pos", x_pos);
            entity.getPersistentData().putDouble("y_pos", y_pos);
            entity.getPersistentData().putDouble("z_pos", z_pos);
            entity.getPersistentData().putDouble("yaw", entity.getYRot());
            entity.getPersistentData().putDouble("pitch", entity.getXRot());
        }

        summon = entity.getPersistentData().getDouble("cnt2") == 1.0;

        // 2. Active Rika Sync (v43 standard)
        if (entity.getPersistentData().getDouble("cnt4") == 1.0) {
            if (entity.getPersistentData().getDouble("friend_num") != 0.0) {
                Entity rika = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("RIKA_UUID"));
                if ((rika instanceof RikaEntity || rika instanceof Rika2Entity) && entity.getPersistentData().getDouble("friend_num") == rika.getPersistentData().getDouble("friend_num")) {
                    if (entity instanceof Player ? entity.isShiftKeyDown() : Math.random() < 0.5) {
                        if (world instanceof ServerLevel _level) {
                            _level.sendParticles(ParticleTypes.SQUID_INK, rika.getX(), rika.getY(), rika.getZ(), 50, 1.0, 1.0, 1.0, 0.0);
                        }
                        
                        rika.setYRot((float) entity.getPersistentData().getDouble("yaw"));
                        rika.setXRot((float) entity.getPersistentData().getDouble("pitch"));
                        rika.setYBodyRot(rika.getYRot());
                        rika.setYHeadRot(rika.getYRot());
                        rika.yRotO = rika.getYRot();
                        rika.xRotO = rika.getXRot();
                        if (rika instanceof LivingEntity _livRika) {
                            _livRika.yBodyRotO = rika.getYRot();
                            _livRika.yHeadRotO = rika.getYRot();
                        }

                        rika.teleportTo(entity.getPersistentData().getDouble("x_pos"), entity.getPersistentData().getDouble("y_pos"), entity.getPersistentData().getDouble("z_pos"));
                        if (rika instanceof ServerPlayer _sp) {
                            _sp.connection.teleport(rika.getX(), rika.getY(), rika.getZ(), rika.getYRot(), rika.getXRot());
                        }

                        if (world instanceof ServerLevel _level) {
                            _level.sendParticles(ParticleTypes.SQUID_INK, rika.getX(), rika.getY(), rika.getZ(), 50, 1.0, 1.0, 1.0, 0.25);
                        }
                    }
                    rika.getPersistentData().putBoolean("flag_attack", true);
                }
            }
            entity.getPersistentData().putDouble("skill", 0.0);
        } else {
            // 3. New Summon Phase
            if (summon) {
                if (entity instanceof Player _p && !world.isClientSide()) {
                    _p.displayClientMessage(Component.literal(Component.translatable("jujutsu.technique.rika1").getString()), true);
                }

                if (entity.getPersistentData().getDouble("cnt1") == 1.0) {
                    // Initial summon position and animation (v43)
                    ClipContext clip = new ClipContext(entity.getEyePosition(1.0F), entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(-6.0)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity);
                    BlockPos pos = entity.level().clip(clip).getBlockPos();
                    entity.getPersistentData().putDouble("x_pos", pos.getX());
                    entity.getPersistentData().putDouble("y_pos", pos.getY() + Math.random() * 0.1);
                    entity.getPersistentData().putDouble("z_pos", pos.getZ());
                    entity.getPersistentData().putDouble("yaw", entity.getYRot());
                    entity.getPersistentData().putDouble("pitch", 0.0);

                    if (entity.getPersistentData().getDouble("cnt3") == 0.0 && entity instanceof LivingEntity _liv) {
                        if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                            _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(4.0);
                        }
                        if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                            _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(4.0);
                        }
                        PlayAnimationProcedure.execute(world, entity);
                    }
                }
            } else {
                if (entity instanceof Player _p && !world.isClientSide()) {
                    _p.displayClientMessage(Component.literal(noControl ? Component.translatable("jujutsu.technique.rika4").getString() : Component.translatable("jujutsu.technique.rika3").getString()), true);
                }
                entity.getPersistentData().putDouble("cnt1", Math.max(entity.getPersistentData().getDouble("cnt1"), 5.0));
            }

            x_pos = entity.getPersistentData().getDouble("x_pos");
            y_pos = entity.getPersistentData().getDouble("y_pos");
            z_pos = entity.getPersistentData().getDouble("z_pos");
            yaw = entity.getPersistentData().getDouble("yaw");
            pitch = entity.getPersistentData().getDouble("pitch");

            if (entity.getPersistentData().getDouble("cnt1") < 5.0) {
                if (world instanceof ServerLevel _level) {
                    _level.sendParticles(ParticleTypes.SQUID_INK, x_pos, y_pos, z_pos, 25, 1.0, 1.0, 1.0, 0.25);
                }
            } else if (entity.getPersistentData().getDouble("cnt1") == 5.0) {
                if (entity.getPersistentData().getDouble("friend_num") == 0.0) {
                    entity.getPersistentData().putDouble("friend_num", Math.random());
                }

                // Determine Rika Variant (Rika 1 vs Rika 2)
                EntityType rikaType = (entity.getPersistentData().getDouble("cnt3") > 0.0) ? (EntityType) JujutsucraftModEntities.RIKA_2.get() : (EntityType) JujutsucraftModEntities.RIKA.get();
                
                if (world instanceof ServerLevel _level) {
                    Entity rika = rikaType.create(_level, null, null, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                    if (rika != null) {
                        rika.setYRot((float) yaw);
                        rika.setXRot((float) pitch);
                        rika.setYBodyRot(rika.getYRot());
                        rika.setYHeadRot(rika.getYRot());
                        rika.yRotO = rika.getYRot();
                        rika.xRotO = rika.getXRot();
                        if (rika instanceof LivingEntity _livRika) {
                            _livRika.yBodyRotO = rika.getYRot();
                            _livRika.yHeadRotO = rika.getYRot();
                            
                            int ownerBoost = (entity instanceof LivingEntity _liv) && _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                            _livRika.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, Math.max(ownerBoost, (rika instanceof RikaEntity) ? 20 : 16), false, false));
                        }

                        entity.getPersistentData().putString("RIKA_UUID", rika.getStringUUID());
                        rika.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());
                        rika.getPersistentData().putDouble("friend_num", entity.getPersistentData().getDouble("friend_num"));
                        rika.getPersistentData().putDouble("friend_num_worker", entity.getPersistentData().getDouble("friend_num"));
                        rika.getPersistentData().putBoolean("JujutsuSorcerer", entity.getPersistentData().getBoolean("JujutsuSorcerer"));
                        rika.getPersistentData().putBoolean("CurseUser", entity.getPersistentData().getBoolean("CurseUser"));
                        rika.getPersistentData().putBoolean("Player", entity instanceof Player);

                        if (!summon) {
                            rika.getPersistentData().putDouble("skill", 11.0);
                            rika.getPersistentData().putDouble("despawn_flag", noControl ? 3.0 : 2.0);
                        }
                        _level.addFreshEntity(rika);
                    }
                    _level.sendParticles(ParticleTypes.SQUID_INK, x_pos, y_pos, z_pos, 25, 1.0, 1.0, 1.0, 0.75);
                }
            } else if (entity.getPersistentData().getDouble("cnt1") < 20.0) {
                if (summon) {
                    // v43 Circular Particle Generation
                    if (world instanceof ServerLevel _level) {
                        for (int i = 0; i < 36; i++) {
                            ParticleGeneratorCircleProcedure.execute(world, 1.0, 90.0, 0.0, 1.0, Mth.nextDouble(_level.getRandom(), 0.0, 2.0), x_pos, x_pos, y_pos, y_pos + Mth.nextDouble(_level.getRandom(), 0.0, 0.25), 0.0, z_pos, z_pos, "minecraft:squid_ink");
                        }
                    }
                } else {
                    entity.getPersistentData().putDouble("skill", 0.0);
                }
            } else {
                entity.getPersistentData().putDouble("skill", 0.0);
            }
        }
    }
}
