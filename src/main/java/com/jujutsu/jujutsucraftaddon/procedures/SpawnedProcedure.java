package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.mojang.util.UUIDTypeAdapter;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SizeByNBTProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber
public class SpawnedProcedure {
    private static final String BENCHMARK_TAG = "jjku_op_sukuna_benchmark";

    @SubscribeEvent
    public static void onEntitySpawned(EntityJoinLevelEvent event) {
        if (event != null && event.getEntity() != null) {
            execute(event, event.getLevel(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
        }
    }

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        execute(null, world, x, y, z, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return;

        CompoundTag persistentData = entity.getPersistentData();

        if (world instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_NO_VANILLA)) {
                if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:vanilla_mob")))) {
                    cancelEvent(event);
                    return;
                }
            }
        }

        handleTenShadowsLogic(world, entity, persistentData);

        ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityTypeKey == null || !entityTypeKey.toString().startsWith("jujutsucraft")) return;

        handleBossStats(entity);
        handlePurpleLogic(world, x, y, z, entity, persistentData);
        handleMahoragaLogic(world, x, y, z, entity, persistentData);

        if (!persistentData.getString("OWNER_UUID").isEmpty()) return;

        handleRedSize(entity);

        if (!world.isClientSide()) {
            livingEntity.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.RESPAWNED_JUJUTSU.get(), 20, 1, false, false));
        }

        MobSpawnType spawnType = entity instanceof PathfinderMob _pathfinder ? _pathfinder.getSpawnType() : null;
        if (spawnType != null) {
            handleEntitySpawnLogic(event, world, entity, spawnType);
        }
    }

    private static void handleTenShadowsLogic(LevelAccessor world, Entity entity, CompoundTag persistentData) {
        if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:ten_shadows_technique"))) && !(entity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity)) {
            String ownerUuidStr = persistentData.getString("OWNER_UUID");
            if (!ownerUuidStr.isEmpty()) {
                try {
                    Entity owner = null;
                    if (world instanceof ServerLevel serverLevel) {
                        owner = serverLevel.getEntity(UUID.fromString(ownerUuidStr));
                    }
                    if (owner instanceof SukunaFushiguroEntity && entity instanceof LivingEntity living) {
                        var sizeAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("jujutsucraft:size"));
                        if (sizeAttr != null && living.getAttributes().hasAttribute(sizeAttr)) {
                            living.getAttribute(sizeAttr).setBaseValue(living.getAttribute(sizeAttr).getBaseValue() * 2);
                        }
                        if (living.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                            living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(living.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * 2);
                            living.setHealth(living.getMaxHealth());
                        }
                        if (living.getAttributes().hasAttribute(Attributes.ARMOR)) {
                            living.getAttribute(Attributes.ARMOR).setBaseValue(living.getAttribute(Attributes.ARMOR).getBaseValue() + 2);
                        }
                        if (!world.isClientSide()) {
                            int amp = living.hasEffect(MobEffects.DAMAGE_BOOST) ? living.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, amp + 4, false, false));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private static void handleBossStats(Entity entity) {
        if (entity instanceof TodoAoiEntity || entity instanceof HigurumaHiromiEntity || entity instanceof MiguelEntity || entity instanceof MiguelDancerEntity) {
            LivingEntity living = (LivingEntity) entity;
            AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
            AttributeInstance toughness = living.getAttribute(Attributes.ARMOR_TOUGHNESS);

            if (maxHealth != null) maxHealth.setBaseValue(900);
            if (armor != null) armor.setBaseValue(30);
            if (toughness != null) toughness.setBaseValue(entity instanceof TodoAoiEntity ? 20 : 10);
            living.setHealth(living.getMaxHealth());
        }
    }

    private static void handlePurpleLogic(LevelAccessor world, double x, double y, double z, Entity entity, CompoundTag persistentData) {
        if (entity instanceof PurpleEntity && persistentData.getDouble("Full") == 1) {
            Vec3 center = new Vec3(x, y, z);
            List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(150.0), e -> e != entity);
            for (Entity target : entities) {
                if (target instanceof GojoSatoruEntity || target instanceof SukunaFushiguroEntity) {
                    if (target instanceof LivingEntity living) {
                        living.setHealth(living.getMaxHealth());
                        if (target instanceof SukunaFushiguroEntity sukunaF) {
                            if (!world.isClientSide()) {
                                sukunaF.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.BINDING_VOW_COOLDOWN.get(), 1200, 0, false, false));
                                sukunaF.addEffect(new MobEffectInstance(net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 100, 0, false, false));
                                sukunaF.addEffect(new MobEffectInstance(net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                            }
                            if (!sukunaF.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut)) {
                                sukunaF.getEntityData().set(SukunaFushiguroEntity.DATA_world_cut, true);
                            }
                            sukunaF.getPersistentData().putDouble("skill", 105);
                            sukunaF.getPersistentData().putDouble("cnt6", 20);
                        }
                    }
                }
            }
        }
    }

    private static void handleMahoragaLogic(LevelAccessor world, double x, double y, double z, Entity entity, CompoundTag persistentData) {
        KenjakuDomainSummoningProcedure.execute(world, x, y, z, entity);
        if (persistentData.getDouble("Mahoraga") == 1 && entity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity && !world.isClientSide()) {
            ((LivingEntity) entity).addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.MAHO_EFFECTO.get(), 40, 1, false, false));
        }
    }

    private static void handleRedSize(Entity entity) {
        if (entity instanceof RedEntity redEntity) {
            redEntity.getDimensions(redEntity.getPose()).scale((float) SizeByNBTProcedure.execute(entity));
        }
    }

    private static void handleEntitySpawnLogic(Event event, LevelAccessor world, Entity entity, MobSpawnType spawnType) {
        if (isBenchmarkEntity(entity)) {
            handleBuffModification(world, entity);
            return;
        }
        if (spawnType == MobSpawnType.COMMAND) {
            handleBuffModification(world, entity);
            return;
        }

        cancelEventIfRuleMet(event, world, JujutsucraftaddonModGameRules.JJKU_NO_STEVENSON, entity instanceof StevensonScreenEntity);
        cancelEventIfRuleMet(event, world, JujutsucraftaddonModGameRules.JJKU_NO_ARMORY_SPIRIT, entity instanceof CursedSpiritGrade37Entity);
        
        GameRules rules = world.getLevelData().getGameRules();
        cancelEventIfChanceFails(event, rules, entity, JujutsucraftaddonModGameRules.JJKU_SUKUNA_RATE, SukunaEntity.class, SukunaFushiguroEntity.class, SukunaPerfectEntity.class);
        cancelEventIfChanceFails(event, rules, entity, JujutsucraftaddonModGameRules.JJKU_GOJO_RATE, GojoSatoruSchoolDaysEntity.class, GojoSatoruEntity.class);
        cancelEventIfChanceFails(event, rules, entity, JujutsucraftaddonModGameRules.JJKU_TOJI_RATE, FushiguroTojiEntity.class, FushiguroTojiBugEntity.class);
        
        cancelEventIfPersistentData(event, rules, entity, "CursedSpirit", JujutsucraftaddonModGameRules.JJKU_CURSED_SPIRIT_RATE);
        cancelEventIfPersistentData(event, rules, entity, "CurseUser", JujutsucraftaddonModGameRules.JJKU_CURSE_USERS_RATE);
        cancelEventIfPersistentData(event, rules, entity, "JujutsuSorcerer", JujutsucraftaddonModGameRules.JJKU_SORCERERS_RATE);

        handleBuffModification(world, entity);
    }

    private static void handleBuffModification(LevelAccessor world, Entity entity) {
        CompoundTag nbt = entity.getPersistentData();
        if (nbt.getDouble("CursedSpirit") == 1 || nbt.getDouble("CurseUser") == 1 || nbt.getDouble("JujutsuSorcerer") == 1) {
            if (nbt.getDouble("buff") != 1 && entity instanceof LivingEntity living) {
                AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    double difficulty = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_DIFFICULTY);
                    maxHealth.setBaseValue(100.0 / difficulty * maxHealth.getBaseValue());
                    living.setHealth(living.getMaxHealth());
                }
                nbt.putDouble("buff", 1);
            }
        }
    }

    private static void cancelEventIfRuleMet(Event event, LevelAccessor world, GameRules.Key<GameRules.BooleanValue> rule, boolean condition) {
        if (condition && world.getLevelData().getGameRules().getBoolean(rule)) {
            cancelEvent(event);
        }
    }

    private static void cancelEventIfChanceFails(Event event, GameRules rules, Entity entity, GameRules.Key<GameRules.IntegerValue> rule, Class<?>... classes) {
        if (Arrays.stream(classes).anyMatch(c -> c.isInstance(entity))) {
            if (Math.random() >= 0.01 * rules.getInt(rule)) {
                if (shouldCancelSpawn(entity)) cancelEvent(event);
            }
        }
    }

    private static void cancelEventIfPersistentData(Event event, GameRules rules, Entity entity, String key, GameRules.Key<GameRules.IntegerValue> rule) {
        if (entity.getPersistentData().getDouble(key) == 1) {
            if (Math.random() >= 0.01 * rules.getInt(rule)) {
                if (shouldCancelSpawn(entity)) cancelEvent(event);
            }
        }
    }

    private static boolean shouldCancelSpawn(Entity entity) {
        if (isBenchmarkEntity(entity)) {
            return false;
        }
        CompoundTag nbt = entity.getPersistentData();
        return nbt.getString("OWNER_UUID").isEmpty() && nbt.getDouble("friend_num") == 0 && nbt.getDouble("Spirit") == 0;
    }

    private static boolean isBenchmarkEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        CompoundTag nbt = entity.getPersistentData();
        return entity.getTags().contains(BENCHMARK_TAG) || !nbt.getString("JJKU_BENCHMARK_ID").isEmpty();
    }

    private static void cancelEvent(Event event) {
        if (event != null) {
            if (event.isCancelable()) event.setCanceled(true);
            else if (event.hasResult()) event.setResult(Event.Result.DENY);
        }
    }
}
