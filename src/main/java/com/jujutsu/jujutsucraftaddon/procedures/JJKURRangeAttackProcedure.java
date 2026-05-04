package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.entity.ItadoriShinjukuEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

public class JJKURRangeAttackProcedure {

    private static final TagKey<EntityType<?>> RANGED_AMMO_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:ranged_ammo"));
    private static final TagKey<EntityType<?>> BLACK_FLASH_ABLE_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:black_flash_able"));
    private static final ResourceKey<DamageType> DMG_COMBAT =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("jujutsucraft:damage_combat"));
    private static final ResourceKey<DamageType> DMG_CURSE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("jujutsucraft:damage_curse"));

    private static SoundEvent SOUND_CRITICAL;
    private static boolean soundCached = false;

    private static void ensureSoundCached() {
        if (soundCached) return;
        SOUND_CRITICAL = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("jujutsucraft:critical"));
        soundCached = true;
    }

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        ensureSoundCached();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        CompoundTag nbt = entity.getPersistentData();

        JujutsucraftModVariables.PlayerVariables pVars = null;
        if (entity instanceof Player) {
            pVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(null);
        }

        double damageValue = nbt.getDouble("Damage");
        boolean blackflashable = damageValue >= 9.0;
        boolean highPower = damageValue >= 24.0;

        if (entity instanceof LivingEntity living && !living.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(
                    JujutsucraftModMobEffects.ATTACKING.get(), 5, highPower ? 1 : 0, false, false));
        }

        DamageFixProcedure.execute(entity);
        double damageSourcePlayer = nbt.getDouble("Damage");

        if (nbt.getDouble("KnockbackFix") != 0.0
                && nbt.getBoolean("attack")
                && nbt.getDouble("knockback") >= 1.0) {
            double kbMult = 1.0 + Math.min(
                    Math.max(nbt.getDouble("cnt6") * 0.2, nbt.getDouble("cnt5") * 0.005), 1.0
            ) * nbt.getDouble("KnockbackFix");
            nbt.putDouble("knockback", nbt.getDouble("knockback") * kbMult);
        }

        boolean BlackFlash = false;
        if (blackflashable
                && nbt.getBoolean("attack")
                && (entity instanceof Player
                ? pVars != null && pVars.PlayerCursePowerFormer > 150.0
                : entity.getType().is(BLACK_FLASH_ABLE_TAG))
                && nbt.getDouble("skill") != 2105.0
                && nbt.getDouble("cnt6") >= 0.0) {

            double bfChance = computeBlackFlashRolls(entity, nbt, pVars);

            for (int i = 0; i < (int) Math.round(bfChance); i++) {
                double threshold = computeAddonBFThreshold(world, entity, 0.998, rng);
                if (rng.nextDouble() > threshold) {
                    BlackFlash = true;
                    break;
                }
            }
        }

//        Entity entityOwner = null;
//        String ownerUuid = nbt.getString("OWNER_UUID");
//        if (!ownerUuid.isEmpty()) {
//            entityOwner = GetEntityFromUUIDProcedure.execute(world, ownerUuid);
//        }

        var damageRegistry = world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder.Reference<DamageType> combatHolder = damageRegistry.getHolderOrThrow(DMG_COMBAT);
        Holder.Reference<DamageType> curseHolder = damageRegistry.getHolderOrThrow(DMG_CURSE);

        double xPos = x, yPos = y, zPos = z;
        double range = nbt.getDouble("Range");
        double yLimit = 1.5;
//        boolean logicSwing = false;

        for (int pass = 0; pass < 2; pass++) {
            Vec3 center = new Vec3(xPos, yPos, zPos);

            for (Entity target : world.getEntitiesOfClass(Entity.class,
                    new AABB(center, center).inflate(range / 2.0), e -> true)) {

                boolean betrayal = LogicBetrayalProcedure.execute(world, entity, target);
                double knockback = nbt.getDouble("knockback");

                if (entity != target || betrayal) {

                    if (!(target instanceof LivingEntity) && nbt.getBoolean("ExtinctionBlock")) {
                        if (!target.level().isClientSide()) {
                            target.kill();
                        }
                    } else {
//                        boolean guard = false;
                        boolean logicKnockback = false;

                        if (LogicAttackProcedure.execute(world, entity, target) || betrayal) {
                            EffectConfirmProcedure.execute(world, x, y, z, entity, target);
                            double damageSrc = damageSourcePlayer;

                            if (BlackFlash && !target.getType().is(RANGED_AMMO_TAG)) {
                                double oldHealth = target instanceof LivingEntity le ? le.getHealth() : -1.0;
                                target.hurt(new DamageSource(combatHolder), 0.1F);
                                double newHealth = target instanceof LivingEntity le ? le.getHealth() : -1.0F;
                                if (newHealth < oldHealth) {
                                    damageSrc = damageSourcePlayer * 4.0;
//                                    highPower = true;
                                } else {
                                    BlackFlash = false;
                                }
                            }

                            if (LogicGuardSuccessProcedure.execute(target, entity)) {
//                                guard = true;
                                double guardDiff = Math.max(damageSrc - target.getPersistentData().getDouble("Damage"), 0.0);
                                if ((target instanceof LivingEntity le
                                        && le.hasEffect(JujutsucraftModMobEffects.GUARD.get()))
                                        || target.getPersistentData().getBoolean("guard")
                                        || target.getPersistentData().getBoolean("attack")) {
                                    knockback *= damageSrc == 0.0 ? 1.0 : Math.max(guardDiff / damageSrc, 0.1);
                                }
                            }

                            damageSrc *= ChangeDamage1Procedure.execute(world, entity, target);
                            damageSrc *= ChangeDamage2Procedure.execute();

                            if (damageSrc > 0.0) {
                                double oldHp = target instanceof LivingEntity le ? le.getHealth() : -1.0;
                                if (shouldSukunaSurviveFatalPurple(world, entity, target, damageSrc, oldHp, rng)) {
                                    logicKnockback = true;
                                    continue;
                                }
                                target.hurt(new DamageSource(curseHolder, entity), (float) damageSrc);
                                double newHp = target instanceof LivingEntity le ? le.getHealth() : -1.0F;

                                if (newHp != oldHp) {
                                    logicKnockback = true;
                                    EffectProcedure.execute(world, x, y, z, entity, target);

                                    if (BlackFlash) {
                                        BlackFlash = false;
                                        handleBlackFlashSuccess(world, x, y, z, entity, target,
                                                pVars, rng);
                                    }

//                                    logicSwing = true;
                                }
                            }
                        }

                        if ((target instanceof Projectile proj ? proj.getDeltaMovement().length() : 0.0) > 0.0
                                && nbt.getDouble("projectile_type") != 0.0
                                && target.getPersistentData().getDouble("Damage") < nbt.getDouble("Damage")) {
//                            logicSwing = true;
                            logicKnockback = true;
                        }

                        if (entity.getType().is(RANGED_AMMO_TAG) && entity.isAlive()
                                && target.getType().is(RANGED_AMMO_TAG) && target.isAlive()) {
                            nbt.putBoolean("Stop", true);
                        }

                        if (entity != target && logicKnockback && knockback > 0.0) {
                            applyKnockback(world, entity, target, knockback, yLimit, nbt);
                        }
                    }
                }
            }

            if (!(entity instanceof LivingEntity living)
                    || !living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                    || nbt.getBoolean("Failed")
                    || nbt.getBoolean("attack")
                    || nbt.getBoolean("DomainAttack")) {
                break;
            }

            xPos = nbt.getDouble("x_pos_doma");
            yPos = nbt.getDouble("y_pos_doma");
            zPos = nbt.getDouble("z_pos_doma");

            int deAmp = living.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier();
            range = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius
                    * (deAmp > 0 ? 6 : 2);
            nbt.putBoolean("DomainAttack", true);
        }

        nbt.putDouble("effect", 0.0);
        nbt.putDouble("effectConfirm", 0.0);
        nbt.putDouble("projectile_type", 0.0);
        nbt.putBoolean("attack", false);
        nbt.putBoolean("ignore", false);
        nbt.putBoolean("DomainAttack", false);
    }

    private static boolean shouldSukunaSurviveFatalPurple(LevelAccessor world, Entity source, Entity target, double damage, double oldHp, ThreadLocalRandom rng) {
        if (!(world instanceof ServerLevel level)
                || !level.getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA)
                || !(source instanceof PurpleEntity)
                || !(target instanceof SukunaFushiguroEntity sukuna)
                || sukuna.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode)
                || oldHp <= 0.0
                || damage < oldHp
                || rng.nextDouble() >= 0.8) {
            return false;
        }
        return JJKURSukunaAIBuff.forceHeianTransformation(world, sukuna, true);
    }

    private static double computeBlackFlashRolls(Entity entity,
                                                 CompoundTag nbt,
                                                 JujutsucraftModVariables.PlayerVariables pVars) {
        double rolls = 1.0;

        if (entity instanceof LivingEntity le
                && le.hasEffect(JujutsucraftModMobEffects.ZONE.get())) {
            rolls += 2.0 + le.getEffect(JujutsucraftModMobEffects.ZONE.get()).getAmplifier();
        }

        if (entity instanceof LivingEntity le
                && le.hasEffect(JujutsucraftModMobEffects.DEEP_CONCENTRATION.get())) {
            int amp = le.getEffect(JujutsucraftModMobEffects.DEEP_CONCENTRATION.get()).getAmplifier();
            rolls += 75.0 + 5 * (amp + 1);
        }

        if (entity instanceof Player) {
            if (entity instanceof ServerPlayer sp && sp.level() instanceof ServerLevel) {
                Advancement adv = sp.server.getAdvancements()
                        .getAdvancement(ResourceLocation.parse("jujutsucraft:black_flash"));
                if (adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone()) {
                    rolls++;
                }
            }
        } else if (entity.getType().is(BLACK_FLASH_ABLE_TAG)) {
            rolls++;
        }

        if (entity instanceof LivingEntity le
                && le.hasEffect(JujutsucraftModMobEffects.SPECIAL.get())
                && le.getEffect(JujutsucraftModMobEffects.SPECIAL.get()).getAmplifier() > 0) {
            rolls += 5.0;
        }

        if (entity instanceof Player) {
            if (pVars != null && (pVars.PlayerCurseTechnique == 21.0 || pVars.PlayerCurseTechnique2 == 21.0)) {
                rolls += 3.0;
            }
        } else if (entity instanceof ItadoriYujiEntity
                || entity instanceof ItadoriYujiShibuyaEntity
                || entity instanceof ItadoriYujiShinjukuEntity) {
            rolls += 3.0;
        }

        if (entity instanceof LivingEntity le) {
            float healthRatio = Math.max(le.getHealth(), 1.0F) / Math.max(le.getMaxHealth(), 1.0F);
            rolls += (1.0F - healthRatio) * 2.0F;
        }

        rolls += nbt.getDouble("cnt6") * 2.0;

        return rolls;
    }

    private static double computeAddonBFThreshold(LevelAccessor world, Entity entity,
                                                  double original, ThreadLocalRandom rng) {

        if (entity instanceof ItadoriShinjukuEntity || entity instanceof ItadoriYujiShinjukuEntity) {
            float healthRatio = ((LivingEntity) entity).getHealth()
                    / ((LivingEntity) entity).getMaxHealth();
            double chance;
            if (healthRatio <= 0.25)      chance = 1.0 / 20;   // 5%
            else if (healthRatio <= 0.33) chance = 1.0 / 25;   // 4%
            else                          chance = 1.0 / 100;  // 1%
            if (rng.nextDouble() < chance) return 0.001;
        }

        LivingEntity target = (entity instanceof Mob mob) ? mob.getTarget() : null;
        boolean gojoTarget = false, sukunaTarget = false;

        if (target != null) {
            gojoTarget = target instanceof GojoSatoruEntity
                    || target instanceof GojoSatoruSchoolDaysEntity
                    || target.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables()).PlayerCurseTechnique == 2;
            sukunaTarget = target instanceof SukunaFushiguroEntity
                    || target.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
        }

        if (entity instanceof SukunaPerfectEntity || entity instanceof GojoSatoruEntity
                || entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof NanamiKentoEntity
                || entity instanceof GojoSatoruSchoolDaysEntity || entity instanceof SukunaEntity
                || entity instanceof SukunaFushiguroEntity) {

            float hp = ((LivingEntity) entity).getHealth() / ((LivingEntity) entity).getMaxHealth();

            if (hp <= 0.5) {
                if (entity instanceof SukunaPerfectEntity) {
                    if (rng.nextDouble() < 1.0 / 20) return 0.001;
                } else if (entity instanceof SukunaFushiguroEntity sf
                        && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode)) {
                    if (rng.nextDouble() < 1.0 / 20) return 0.001;
                } else if (entity instanceof SukunaFushiguroEntity) {
                    if (target != null) {
                        if (gojoTarget) return original; // Contra Gojo = normal
                        if (rng.nextDouble() < 1.0 / 50) return 0.001;
                    }
                } else if (entity instanceof GojoSatoruEntity) {
                    if (sukunaTarget) return original;
                    if (rng.nextDouble() < 1.0 / 100) return 0.001;
                } else if (entity instanceof OkkotsuYutaCullingGameEntity) {
                    if (rng.nextDouble() < 1.0 / 300) return 0.001;
                } else {
                    if (rng.nextDouble() < 1.0 / 50) return 0.001;
                }
            } else {
                if (entity instanceof GojoSatoruEntity) {
                    if (target != null && sukunaTarget) return original;
                    if (rng.nextDouble() < 1.0 / 200) return 0.001;
                } else if (entity instanceof SukunaFushiguroEntity) {
                    if (target != null && gojoTarget) return original;
                    if (rng.nextDouble() < 1.0 / 150) return 0.001;
                } else if (entity instanceof NanamiKentoEntity) {
                    if (rng.nextDouble() < 1.0 / 100) return 0.001;
                } else if (entity instanceof OkkotsuYutaCullingGameEntity) {
                    if (rng.nextDouble() < 1.0 / 400) return 0.001;
                } else if (rng.nextDouble() < 1.0 / 150) {
                    return 0.001;
                }
            }
        }

        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeKey != null && !typeKey.toString().startsWith("jujutsucraft")) {
            JujutsucraftaddonModVariables.PlayerVariables addonVars =
                    entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                            .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

            if (rng.nextDouble() < addonVars.BFChance / 1000.0) {
                boolean reworked = world.getLevelData().getGameRules()
                        .getBoolean(JujutsucraftaddonModGameRules.JJKU_BLACK_FLASH_REWORKED);
                boolean hasFatigue = entity instanceof LivingEntity le
                        && le.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE_BLACK_FLASH.get());

                if (reworked) {
                    if (!hasFatigue && entity.getPersistentData().getDouble("cnt_bf") >= 50.0) {
                        return 0.001;
                    }
                } else if (!hasFatigue) {
                    return 0.001;
                }
            }
        }

        return original;
    }

    private static void handleBlackFlashSuccess(LevelAccessor world, double x, double y, double z,
                                                Entity entity, Entity target,
                                                JujutsucraftModVariables.PlayerVariables pVars,
                                                ThreadLocalRandom rng) {

        if (entity instanceof Player
                && world.getLevelData().getGameRules().getBoolean(JujutsucraftModGameRules.JUJUTSU_GAIN_FAME)) {
            if (pVars != null) {
                pVars.PlayerFame += 200.0;
                pVars.PlayerExperience += 200.0;
                pVars.PlayerTechniqueUsedNumber += 2500.0;
                pVars.syncPlayerVariables(entity);
            }
        }

        if (world instanceof ServerLevel serverLevel) {
            Entity bfEntity = JujutsucraftModEntities.ENTITY_BLACK_FLASH.get()
                    .spawn(serverLevel,
                            BlockPos.containing(target.getX(), target.getY(), target.getZ()),
                            MobSpawnType.MOB_SUMMONED);
            if (bfEntity != null) {
                bfEntity.setYRot(rng.nextFloat() * 360.0F);
            }
        }

        if (entity instanceof LivingEntity le && !le.level().isClientSide()) {
            le.addEffect(new MobEffectInstance(
                    JujutsucraftModMobEffects.ZONE.get(), 6000, 0, true, true));
        }

        reduceEffectDuration(entity, JujutsucraftModMobEffects.FATIGUE.get(), 2000);

        reduceEffectDuration(entity, JujutsucraftModMobEffects.BRAIN_DAMAGE.get(), 2000);

        if (entity instanceof ServerPlayer sp) {
            Advancement adv = sp.server.getAdvancements()
                    .getAdvancement(ResourceLocation.parse("jujutsucraft:black_flash"));
            if (adv != null) {
                AdvancementProgress progress = sp.getAdvancements().getOrStartProgress(adv);
                if (!progress.isDone()) {
                    for (String criteria : progress.getRemainingCriteria()) {
                        sp.getAdvancements().award(adv, criteria);
                    }
                }
            }
        }

        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        boolean isJJKEntity = typeKey != null && typeKey.toString().startsWith("jujutsucraft");

        if (isJJKEntity) {
            BlackFlashedProcedure.execute(world, x, y, z, entity);
        } else {
            boolean reworked = world.getLevelData().getGameRules()
                    .getBoolean(JujutsucraftaddonModGameRules.JJKU_BLACK_FLASH_REWORKED);
            boolean hasFatigue = entity instanceof LivingEntity le
                    && le.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE_BLACK_FLASH.get());

            JujutsucraftaddonModVariables.PlayerVariables addonVars =
                    entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                            .orElse(new JujutsucraftaddonModVariables.PlayerVariables());
            boolean timerReady = addonVars.Timer1 == 1;

            if (reworked) {
                if (!hasFatigue
                        && entity.getPersistentData().getDouble("cnt_bf") >= 50.0
                        && timerReady) {
                    executeAddonBFSystems(world, x, y, z, entity, addonVars);
                }
            } else {
                if (!hasFatigue && timerReady) {
                    executeAddonBFSystems(world, x, y, z, entity, addonVars);
                }
                entity.getPersistentData().putDouble("cnt_bf", 0);
            }
        }
    }

    private static void executeAddonBFSystems(LevelAccessor world, double x, double y, double z,
                                              Entity entity,
                                              JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        BlackFlashedProcedure.execute(world, x, y, z, entity);
        BFMasteryProcedure.execute(world, entity);
        entity.getPersistentData().putDouble("cnt_bf", 0);
        ItadoriClan2Procedure.execute(world, entity);
        BlackFlashNerfedProcedure.execute(world, entity);

        if (addonVars.ImpactFramesVariable == 0
                && entity instanceof LivingEntity le
                && !le.hasEffect(JujutsucraftaddonModMobEffects.OUT_LINER.get())
                && !le.level().isClientSide()) {
            le.addEffect(new MobEffectInstance(
                    JujutsucraftaddonModMobEffects.OUT_LINER.get(),
                    Mth.nextInt(RandomSource.create(), 10, 15), 1, false, false));
        }
    }

    private static void reduceEffectDuration(Entity entity, MobEffect effect, int reduction) {
        if (!(entity instanceof LivingEntity le) || !le.hasEffect(effect)) return;

        MobEffectInstance current = le.getEffect(effect);
        if (current == null) return;

        int newDuration = Math.max(current.getDuration() - reduction, 0);
        int amp = Math.max(current.getAmplifier(), 0);

        le.removeEffect(effect);

        if (newDuration > 0 && !le.level().isClientSide()) {
            le.addEffect(new MobEffectInstance(effect, newDuration, amp, false, false));
        }
    }

    private static void applyKnockback(LevelAccessor world, Entity attacker, Entity target,
                                       double knockback, double yLimit, CompoundTag attackerNbt) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        double xKb = target.getX() - attacker.getX();
        double yKb = target.getY() - attacker.getY();
        double zKb = target.getZ() - attacker.getZ();

        if (target.onGround()) {
            yKb = Math.abs(yKb);
        }

        if (xKb == 0.0 && zKb == 0.0) {
            double spread = Math.abs(yKb);
            if (spread > 0.0) {
                xKb = rng.nextDouble(-spread, spread);
                zKb = rng.nextDouble(-spread, spread);
            } else {
                xKb = rng.nextDouble(-0.1, 0.1);
                zKb = rng.nextDouble(-0.1, 0.1);
            }
        }

        double distance = Math.sqrt(xKb * xKb + yKb * yKb + zKb * zKb);
        if (distance == 0.0) return;

        knockback *= 3.0;
        xKb = xKb / distance * knockback;
        yKb = yKb / distance * knockback;
        zKb = zKb / distance * knockback;

        if (yKb > yLimit) {
            double totalMag = Math.sqrt(xKb * xKb + yKb * yKb + zKb * zKb);
            double horizMag = Math.sqrt(xKb * xKb + zKb * zKb);
            double newHorizScale = Math.sqrt(totalMag * totalMag - yLimit * yLimit);
            newHorizScale /= horizMag != 0.0 ? horizMag : 1.0;
            xKb *= newHorizScale;
            yKb = yLimit;
            zKb *= newHorizScale;
        }

        if (JujutsucraftModVariables.MapVariables.get(world).BlastGame) {
            float targetHp = target instanceof LivingEntity le ? le.getHealth() : -1.0F;
            float targetMaxHp = target instanceof LivingEntity le ? le.getMaxHealth() : -1.0F;
            double blastMult = 4.0 * (1.0 - Math.max(targetHp, 1.0F) / Math.max(targetMaxHp, 1.0F));
            xKb *= blastMult;
            yKb *= blastMult;
            zKb *= blastMult;

            int guardAmp = (target instanceof LivingEntity le
                    && le.hasEffect(JujutsucraftModMobEffects.GUARD.get()))
                    ? le.getEffect(JujutsucraftModMobEffects.GUARD.get()).getAmplifier() : 0;

            if (guardAmp < 1 && (xKb * xKb + yKb * yKb + zKb * zKb) > 100.0) {
                double ex = target.getX();
                double ey = target.getY() + target.getBbHeight() * 0.5;
                double ez = target.getZ();

                if (world instanceof ServerLevel sl) {
                    sl.sendParticles(
                            JujutsucraftModParticleTypes.PARTICLE_BLACK_FLASH_1.get(),
                            ex, ey, ez, 50, 2.0, 2.0, 2.0, 2.0);
                    sl.sendParticles(
                            JujutsucraftModParticleTypes.PARTICLE_BROKEN_GLASS_SMALL.get(),
                            ex, ey, ez, 50, 2.0, 2.0, 2.0, 2.0);
                }

                if (SOUND_CRITICAL != null && world instanceof Level level) {
                    if (!level.isClientSide()) {
                        level.playSound(null, ex, ey, ez, SOUND_CRITICAL, SoundSource.NEUTRAL, 4.0F, 1.0F);
                    } else {
                        level.playLocalSound(ex, ey, ez, SOUND_CRITICAL, SoundSource.NEUTRAL, 4.0F, 1.0F, false);
                    }
                }
            }
        }

        xKb += attackerNbt.getDouble("x_knockback");
        yKb += attackerNbt.getDouble("y_knockback");
        zKb += attackerNbt.getDouble("z_knockback");

        if (attacker instanceof LivingEntity) {
            double kbResist = 0.0;
            if (target instanceof LivingEntity le
                    && le.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE)) {
                kbResist = le.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue();
            }
            double resistDiv = Math.sqrt(Math.max(kbResist, 0.0) + 1.0);
            xKb /= resistDiv;
            yKb /= resistDiv;
            zKb /= resistDiv;
        }

        target.setDeltaMovement(new Vec3(xKb, yKb, zKb));

        CompoundTag targetNbt = target.getPersistentData();
        targetNbt.putDouble("old_x_position", targetNbt.getDouble("old_x_position") - xKb);
        targetNbt.putDouble("old_y_position", targetNbt.getDouble("old_y_position") - yKb);
        targetNbt.putDouble("old_z_position", targetNbt.getDouble("old_z_position") - zKb);
    }
}
