package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.entity.ErroEntity;
import com.jujutsu.jujutsucraftaddon.entity.ErrorEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.ProjectileSlashEntity;
import net.mcreator.jujutsucraft.entity.SukunaEntity;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

public class JJKURDismantleProcedure { //checkar depois

    public static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (ci != null) ci.cancel();
        if (entity == null) return;
        processDismantleLogic(world, x, y, z, entity);
    }

    public static void processDismantleLogic(LevelAccessor world, double x, double y, double z, Entity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putDouble("cnt1", persistentData.getDouble("cnt1") + 1.0);

        boolean canUseWorld = checkWorldSlashPermission(entity);
        LivingEntity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;

        if (target != null) {
            handleTargetInteraction(world, entity, target, canUseWorld);
        }

        handleModeSelection(entity, target, canUseWorld);

        double multiplier = handleChargingAndCost(world, entity, canUseWorld);

        handlePreAnimations(world, entity, canUseWorld);

        if (persistentData.getDouble("cnt1") == 5.0) {
            executeHybridSlashBurst(world, x, y, z, entity, canUseWorld, multiplier);
        }

        handlePostExecution(entity);
    }

    private static boolean checkWorldSlashPermission(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            ResourceLocation adv = ResourceLocation.parse("jujutsucraft:skill_dismantle_cut_the_world");
            Advancement advancement = player.server.getAdvancements().getAdvancement(adv);
            return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
        }
        if (entity instanceof SukunaFushiguroEntity fushiguro) {
            return fushiguro.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut);
        }
        return entity instanceof SukunaPerfectEntity || entity instanceof ErrorEntity || entity instanceof ErroEntity;
    }

    private static void handleTargetInteraction(LevelAccessor world, Entity entity, LivingEntity target, boolean canUseWorld) {
        if (canUseWorld && entity instanceof LivingEntity living && !living.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, false, false));
        }

        RotateEntityProcedure.execute(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), entity);
        entity.getPersistentData().putBoolean("PRESS_Z", false);

        if (GetDistanceNearestEnemyProcedure.execute(world, entity) > 6.0) {
            CompoundTag targetData = target.getPersistentData();
            boolean targetIsBusy = targetData.getDouble("skill") != 0.0 || targetData.getBoolean("attack") || targetData.getDouble("Damage") != 0.0;
            if (!targetIsBusy) {
                entity.getPersistentData().putBoolean("PRESS_Z", true);
            }
        }

        if (canUseWorld && target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) && !entity.getPersistentData().getBoolean("flag_dismantle")) {
            entity.getPersistentData().putBoolean("flag_dismantle", true);
            entity.getPersistentData().putDouble("cnt6", 5.0);
        }

        if (entity.getPersistentData().getDouble("cnt6") >= 5.0) {
            entity.getPersistentData().putBoolean("PRESS_Z", false);
        }
    }

    private static void handleModeSelection(Entity entity, LivingEntity target, boolean canUseWorld) {
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.getDouble("cnt7") == 0.0) {
            double mode = (entity instanceof Player p) ? (p.isShiftKeyDown() ? 1.0 : 2.0) : (Math.random() < 0.5 ? 1.0 : 2.0);
            if (target != null && canUseWorld && target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
                mode = 2.0;
            }
            persistentData.putDouble("cnt7", mode);
        }
    }

    private static double handleChargingAndCost(LevelAccessor world, Entity entity, boolean canUseWorld) {
        CompoundTag persistentData = entity.getPersistentData();
        double multiplier = 1.0;
        if (persistentData.getDouble("cnt7") == 1.0) {
            if (entity instanceof LivingEntity living && !living.level().isClientSide()) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 6, false, false));
                living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) persistentData.getDouble("COOLDOWN_TICKS"), 0, false, false));
            }
            persistentData.putDouble("cnt6", -1.0);
            multiplier = 0.5;
        } else if (persistentData.getDouble("cnt1") <= 1.0) {
            handleZCharging(world, entity);
            multiplier = 1.0;
        }
        return multiplier;
    }

    private static void handleZCharging(LevelAccessor world, Entity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.getBoolean("PRESS_Z")) return;

        persistentData.putDouble("cnt1", 0.0);
        if (entity instanceof LivingEntity living && !living.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 4, false, false));
            living.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) persistentData.getDouble("COOLDOWN_TICKS"), 0, false, false));
        }

        double cnt6 = persistentData.getDouble("cnt6");
        double yaw = Math.toRadians(entity.getYRot() + 90.0F);
        double pitch = Math.toRadians(entity.getXRot());
        double dist = 1.0 + entity.getBbWidth();
        double px = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * dist;
        double py = entity.getY() + entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * dist;
        double pz = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * dist;

        if (cnt6 < 3.0) {
            double cnt5 = persistentData.getDouble("cnt5") + 1.0;
            persistentData.putDouble("cnt5", cnt5);
            if (cnt5 > 20.0) {
                persistentData.putDouble("cnt5", 0.0);
                persistentData.putDouble("cnt6", cnt6 + 1.0);
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    player.displayClientMessage(Component.literal("§l\"" + Component.translatable("chant.jujutsucraft.dismantle" + Math.round(cnt6 + 1.0)).getString() + "\""), false);
                }
                entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                    cap.PlayerCursePowerChange -= 30.0;
                    cap.syncPlayerVariables(entity);
                });
            }
        } else if (cnt6 < 5.0) {
            persistentData.putDouble("cnt6", 5.0);
            if (!world.isClientSide()) {
                if (world instanceof Level level) level.explode(null, px, py, pz, 0.0F, Level.ExplosionInteraction.NONE);
                if (world instanceof ServerLevel server) server.sendParticles(ParticleTypes.ENCHANTED_HIT, px, py, pz, 20, 0.25, 0.25, 0.25, 1.0);
            }
        }

        if (entity instanceof LivingEntity living && !living.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get())) {
            ChargeParticleProcedure.execute(world, entity, persistentData.getDouble("cnt6") >= 5.0 ? 1.0 : 0.0);
        }
    }

    private static void handlePreAnimations(LevelAccessor world, Entity entity, boolean canUseWorld) {
        CompoundTag persistentData = entity.getPersistentData();
        double cnt1 = persistentData.getDouble("cnt1");
        if (cnt1 == 0.0 && canUseWorld) {
            setEntityAnimation(entity, persistentData.getDouble("cnt6") >= 5.0 ? 207 : 120);
            PlayAnimationProcedure.execute(world, entity);
        }

        if (cnt1 == 1.0) {
            if (persistentData.getDouble("cnt7") == 1.0) {
                persistentData.putDouble("cnt1", Math.max(cnt1, 5.0));
                if (persistentData.getDouble("cnt8") == 1.0 && (entity instanceof SukunaFushiguroEntity || entity instanceof SukunaEntity || entity instanceof SukunaPerfectEntity)) {
                    if (entity instanceof LivingEntity living) living.swing(InteractionHand.MAIN_HAND, true);
                    setEntityAnimation(entity, 240);
                    PlayAnimationProcedure.execute(world, entity);
                }
            }

            if (persistentData.getDouble("cnt6") >= 0.0) {
                if (entity instanceof LivingEntity living) living.swing(InteractionHand.MAIN_HAND, true);
                setEntityAnimation(entity, 207);
                if (canUseWorld && persistentData.getDouble("cnt6") >= 4.0) {
                    entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                        cap.PlayerCursePower -= 500.0;
                        cap.syncPlayerVariables(entity);
                    });
                }
                PlayAnimationProcedure.execute(world, entity);
            }
        }
    }

    private static void executeHybridSlashBurst(LevelAccessor world, double x, double y, double z, Entity entity, boolean canUseWorld, double modeMultiplier) {
        CompoundTag persistentData = entity.getPersistentData();
        boolean worldCutter = (persistentData.getDouble("cnt6") >= 5.0 && canUseWorld);
        boolean isVertical = Math.random() < 0.5;
        double cnt6Value = 1.0 + persistentData.getDouble("cnt6") * 0.2;
        int destructionLevel = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_DESTRUCTION_LEVEL);

        if (worldCutter) {
            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                cap.PlayerCursePower -= 500.0;
                cap.syncPlayerVariables(entity);
            });
        }

        if (world instanceof ServerLevel serverLevel) {
            double hpValue = (30 + ((entity instanceof LivingEntity livingEntity && livingEntity.hasEffect(MobEffects.DAMAGE_BOOST)) ? livingEntity.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0) * 7) * cnt6Value;
            double sizeValue = 0.5 * ReturnEntitySizeProcedure.execute(entity);

            ProjectileSlashEntity slash = JujutsucraftModEntities.PROJECTILE_SLASH.get().create(serverLevel);
            if (slash != null) {
                slash.moveTo(x, y + entity.getBbHeight() * 0.5, z, entity.getYRot(), entity.getXRot());
                slash.setYBodyRot(slash.getYRot());
                slash.setYHeadRot(slash.getYRot());
                slash.yRotO = slash.getYRot();
                slash.xRotO = slash.getXRot();
                slash.yBodyRotO = slash.getYRot();
                slash.yHeadRotO = slash.getYRot();

                SetRangedAmmoProcedure.execute(entity, slash);

                if (slash.getServer() != null) {
                    slash.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, slash.position(), slash.getRotationVector(), serverLevel, 4, slash.getName().getString(), slash.getDisplayName(), slash.getServer(), slash),
                            "data merge entity @s {NoAI:1b}"
                    );
                }

                if (slash.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                    slash.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hpValue);
                }
                slash.setHealth((float) hpValue);
                if (slash.getAttributes().hasAttribute(JujutsucraftModAttributes.SIZE.get())) {
                    slash.getAttribute(JujutsucraftModAttributes.SIZE.get()).setBaseValue(sizeValue);
                }

                CompoundTag slashData = slash.getPersistentData();
                slashData.putDouble("size", sizeValue * 5.0 * (1.0 + 0.5 * persistentData.getDouble("cnt6")));
                slashData.putDouble("cnt6", persistentData.getDouble("cnt6") + persistentData.getDouble("cnt8") * 0.025);
                slashData.putDouble("x_power", entity.getLookAngle().x * 9.0);
                slashData.putDouble("y_power", entity.getLookAngle().y * 9.0);
                slashData.putDouble("z_power", entity.getLookAngle().z * 9.0);
                slashData.putDouble("NameRanged_ranged", persistentData.getDouble("NameRanged"));
                slashData.putString("OWNER_UUID", entity.getStringUUID());

                slash.getEntityData().set(ProjectileSlashEntity.DATA_mode, worldCutter ? 1 : 0);

                if (persistentData.getDouble("cnt6") >= 0.0) {
                    PlayAnimationEntity2Procedure.execute(slash, isVertical ? "vertical1" : "idle1");
                } else {
                    PlayAnimationEntity2Procedure.execute(slash, isVertical ? "vertical" + Math.round((float) Mth.nextInt(RandomSource.create(), 1, 5)) : "idle" + Math.round((float) Mth.nextInt(RandomSource.create(), 1, 5)));
                }

                serverLevel.addFreshEntity(slash);
            }
        }

        playSweepSounds(world, x, y, z, cnt6Value);

        double originalYaw = entity.getYRot();
        double originalPitch = entity.getXRot();

        if (entity instanceof LivingEntity attacker) {
            attacker.swing(InteractionHand.MAIN_HAND, true);
            if (!canUseWorld) {
                if (isVertical) {
                    setEntityAnimation(attacker, 207.0);
                } else {
                    Attribute anim1 = JujutsucraftModAttributes.ANIMATION_1.get();
                    Attribute anim2 = JujutsucraftModAttributes.ANIMATION_2.get();
                    if (attacker.getAttributes().hasAttribute(anim1)) attacker.getAttribute(anim1).setBaseValue(-5.0);
                    if (attacker.getAttributes().hasAttribute(anim2)) attacker.getAttribute(anim2).setBaseValue(Mth.nextInt(RandomSource.create(), 0, 1));
                }
                PlayAnimationProcedure.execute(world, attacker);
            }
        }

        double rotYaw = originalYaw - persistentData.getDouble("cnt2") * 15.0 * modeMultiplier * destructionLevel;
        double rotPitch = originalPitch - persistentData.getDouble("cnt3") * 15.0 * modeMultiplier * destructionLevel;
        double startPitch = originalPitch - 25.0;
        double endPitch = originalPitch + 25.0;
        int iterations = (int) Math.round(30.0 * modeMultiplier * destructionLevel);
        double pitchStep = (endPitch - startPitch) / iterations;
        double currentPitch = startPitch;

        for (int i = 0; i < iterations; i++) {
            syncEntityRotation(entity, rotYaw, rotPitch);
            rotYaw += persistentData.getDouble("cnt2");
            rotPitch += persistentData.getDouble("cnt3");

            double rayRange = 30.0 + Math.max(persistentData.getDouble("cnt6"), 0.0) * 2.0;
            processHybridRaycastLine(world, entity, rayRange, cnt6Value, worldCutter, destructionLevel);

            if (cnt6Value > 7 && worldCutter) {
                syncEntityRotation(entity, originalYaw, currentPitch);
                processHybridRaycastLine(world, entity, 200 * modeMultiplier, 1, true, destructionLevel);
                currentPitch += pitchStep;
            }
        }

        if (world instanceof Level level) playSound(level, x, y, z, "jujutsucraft:sword_sweep", 2.0F, 2.0F);
        syncEntityRotation(entity, originalYaw, originalPitch);

        if (persistentData.getDouble("cnt7") == 1.0) {
            handleMasteryProgression(entity);
        }
    }

    private static void processHybridRaycastLine(LevelAccessor world, Entity entity, double maxDist, double cnt6, boolean worldCutter, int destLevel) {
        CompoundTag persistentData = entity.getPersistentData();
        double distStep = 0.0;
        boolean stopOnBlock = false;

        for (int j = 0; j < (int) Math.round(maxDist); j++) {
            Vec3 eyePos = entity.getEyePosition(1.0F);
            Vec3 lookPos = eyePos.add(entity.getViewVector(1.0F).scale(distStep));
            BlockPos bpos = BlockPos.containing(lookPos);

            persistentData.putDouble("Damage", 15.0);
            applyDistanceDecay(entity, distStep);
            persistentData.putDouble("Damage", persistentData.getDouble("Damage") * Math.max(cnt6, 0.75));
            persistentData.putDouble("Range", 3.0 * cnt6 * destLevel);
            persistentData.putDouble("knockback", 0.25 * Math.min(cnt6, 1.0));
            persistentData.putDouble("projectile_type", 1.0);
            persistentData.putDouble("effect", 1.0);

            if (worldCutter) {
                persistentData.putBoolean("ignore", true);
                persistentData.putDouble("effectConfirm", 3.0);
            }

            RangeAttackProcedure.execute(world, lookPos.x, lookPos.y, lookPos.z, entity);

            if (world.getBlockState(bpos).canOcclude()) {
                if (world instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, lookPos.x, lookPos.y, lookPos.z, 1, 0.1, 0.1, 0.1, 0.0);
                stopOnBlock = true;
            }

            if (!world.isEmptyBlock(bpos)) {
                persistentData.putDouble("BlockRange", 2.0 * cnt6 * destLevel);
                persistentData.putDouble("BlockDamage", 6.0 * cnt6 * destLevel);
                if (worldCutter) {
                    persistentData.putBoolean("ExtinctionBlock", true);
                    persistentData.putDouble("BlockDamage", 99999.0);
                }
                persistentData.putBoolean("noEffect", true);
                BlockDestroyAllDirectionProcedure.execute(world, lookPos.x, lookPos.y, lookPos.z, entity);
            }

            distStep += 1.0;
            if (!worldCutter && stopOnBlock) break;
        }
    }

    private static void applyDistanceDecay(Entity entity, double dist) {
        int floorDist = (int) Math.round(Math.floor(dist));
        if (floorDist > 0) {
            CompoundTag persistentData = entity.getPersistentData();
            double dmg = persistentData.getDouble("Damage");
            for (int k = 0; k < floorDist; k++) {
                dmg *= 0.99;
                if (dmg < 9.0) {
                    dmg = 9.0;
                    break;
                }
            }
            persistentData.putDouble("Damage", dmg);
        }
    }

    private static void handleMasteryProgression(Entity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        LivingEntity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;
        if (target != null) persistentData.putBoolean("PRESS_Z", true);

        double cnt8 = persistentData.getDouble("cnt8");
        boolean canProgress = (!persistentData.getBoolean("PRESS_Z") || !(cnt8 < 16.0)) && !(cnt8 < 8.0);

        if (canProgress) {
            incrementTechniqueMastery(entity);
            persistentData.putDouble("skill", 0.0);
        } else {
            persistentData.putDouble("cnt1", 3.0);
            persistentData.putDouble("cnt8", cnt8 + 1.0);
            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                cap.PlayerCursePowerChange -= 5.0;
                cap.syncPlayerVariables(entity);
                if (cap.PlayerCursePower + cap.PlayerCursePowerChange <= 0.0) {
                    persistentData.putDouble("skill", 0.0);
                    incrementTechniqueMastery(entity);
                }
            });
        }
    }

    private static void incrementTechniqueMastery(Entity entity) {
        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(base -> {
            if (base.BodyItem.getCount() >= 19.0) {
                entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addon -> {
                    addon.TechniqueMastery += 1.0;
                    addon.syncPlayerVariables(entity);
                });
            }
        });
    }

    private static void handlePostExecution(Entity entity) {
        if (entity.getPersistentData().getDouble("cnt1") > 10.0) {
            incrementTechniqueMastery(entity);
            entity.getPersistentData().putDouble("skill", 0.0);
        }
    }

    private static void setEntityAnimation(Entity entity, double value) {
        if (entity instanceof LivingEntity living) {
            Attribute animAttr = JujutsucraftModAttributes.ANIMATION_1.get();
            if (living.getAttributes().hasAttribute(animAttr)) {
                living.getAttribute(animAttr).setBaseValue(value);
            }
        }
    }

    private static void syncEntityRotation(Entity ent, double yaw, double pitch) {
        ent.setYRot((float) yaw);
        ent.setXRot((float) pitch);
        ent.setYBodyRot(ent.getYRot());
        ent.setYHeadRot(ent.getYRot());
        ent.yRotO = ent.getYRot();
        ent.xRotO = ent.getXRot();
        if (ent instanceof LivingEntity living) {
            living.yBodyRotO = living.getYRot();
            living.yHeadRotO = living.getYRot();
        }
    }

    private static void playSweepSounds(LevelAccessor world, double x, double y, double z, double cnt6) {
        if (!(world instanceof Level level)) return;
        float volume = (float) (0.5 * cnt6);
        playSound(level, x, y, z, "jujutsucraft:sword_sweep", volume, 0.5F);
        playSound(level, x, y, z, "jujutsucraft:sword_sweep", volume, 0.75F);
        playSound(level, x, y, z, "jujutsucraft:sword_sweep", volume, 1.0F);
    }

    private static void playSound(Level level, double x, double y, double z, String soundId, float vol, float pitch) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse(soundId));
        if (sound == null) return;
        if (!level.isClientSide()) level.playSound(null, x, y, z, sound, SoundSource.NEUTRAL, vol, pitch);
        else level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, vol, pitch, false);
    }
}
