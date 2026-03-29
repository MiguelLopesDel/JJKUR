package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.entity.RabbitEscapeEntity;
import net.mcreator.jujutsucraft.entity.UraumeEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.GetEntityFromUUIDProcedure;
import net.mcreator.jujutsucraft.procedures.LogicAttackTargetStartProcedure;
import net.mcreator.jujutsucraft.procedures.ReturnInsideItemProcedure;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LogicAttackTargetStartProcedure.class, priority = -10000)
public abstract class LogicAttackTargetStartMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Mob mob)) {
            cir.setReturnValue(false);
            return;
        }

        // Addon Logic: Quake effect prevents attacking
        if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.QUAKE.get())) {
            cir.setReturnValue(false);
            return;
        }

        Entity target = mob.getTarget();
        if (!(target instanceof LivingEntity livingTarget)) {
            cir.setReturnValue(true);
            return;
        }

        cir.cancel(); // Assume control of the logic

        CompoundTag entityData = entity.getPersistentData();
        CompoundTag targetData = target.getPersistentData();
        double friendNum = entityData.getDouble("friend_num");

        if (friendNum != 0.0 && friendNum == targetData.getDouble("friend_num")) {
            cir.setReturnValue(false);
            return;
        }

        if (entityData.getString("TARGET_UUID").equals(target.getStringUUID())) {
            cir.setReturnValue(true);
            return;
        }

        Entity event_owner = entity;
        Entity target_owner = target;
        int safeCounter = 0;

        // Trace event_owner
        for (String ownerUUID = entityData.getString("OWNER_UUID"); !ownerUUID.isEmpty() && safeCounter < 8; safeCounter++) {
            Entity found = GetEntityFromUUIDProcedure.execute(world, ownerUUID);
            if (!(found instanceof LivingEntity)) break;
            event_owner = found;
            ownerUUID = found.getPersistentData().getString("OWNER_UUID");
        }

        // Trace target_owner
        safeCounter = 0;
        for (String tOwnerUUID = targetData.getString("OWNER_UUID"); !tOwnerUUID.isEmpty() && safeCounter < 8; safeCounter++) {
            Entity found = GetEntityFromUUIDProcedure.execute(world, tOwnerUUID);
            if (!(found instanceof LivingEntity)) break;
            target_owner = found;
            tOwnerUUID = found.getPersistentData().getString("OWNER_UUID");
        }

        boolean logic_a = true;
        boolean eOwnerIsPlayer = event_owner instanceof Player || event_owner.getPersistentData().getBoolean("Player");
        boolean tOwnerIsPlayer = target_owner instanceof Player || target_owner.getPersistentData().getBoolean("Player");

        if (eOwnerIsPlayer && tOwnerIsPlayer) {
            if (!world.getLevelData().getGameRules().getBoolean(JujutsucraftModGameRules.JUJUTSUPVP)) {
                logic_a = false;
            }
        } else if (!event_owner.getPersistentData().getBoolean("JujutsuSorcerer") && !entityData.getBoolean("JujutsuSorcerer")
                || !target_owner.getPersistentData().getBoolean("JujutsuSorcerer") && !targetData.getBoolean("JujutsuSorcerer")) {
            
            boolean eCursed = event_owner.getPersistentData().getBoolean("CursedSpirit") || event_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:transfigured_human")));
            boolean tCursed = target_owner.getPersistentData().getBoolean("CursedSpirit") || target_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:transfigured_human")));

            if (!eCursed || !tCursed) {
                if ((event_owner.getPersistentData().getBoolean("CurseUser") || entityData.getBoolean("CurseUser"))
                        && !target_owner.getPersistentData().getBoolean("JujutsuSorcerer")
                        && !target_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("curseuser")))
                        && !target_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("cursedspirit")))) {
                    logic_a = false;
                }
            } else {
                logic_a = false;
            }
        } else {
            logic_a = false;
        }

        // Sukuna Effect Force Attack
        if (event_owner instanceof LivingEntity le && le.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                || entity instanceof LivingEntity le2 && le2.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                || target_owner instanceof LivingEntity le3 && le3.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                || livingTarget.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
            logic_a = true;
        }

        // Uraume Finger/Sukuna protection
        if (event_owner instanceof UraumeEntity && (ReturnInsideItemProcedure.execute(target_owner).getItem() == JujutsucraftModItems.SUKUNA_FINGER.get()
                || (target_owner instanceof LivingEntity le && le.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())))) {
            logic_a = false;
        }

        // Death Painting protection
        if (event_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:death_painting")))) {
            boolean targetIsItadori = false;
            if (target_owner instanceof Player pTarget) {
                JujutsucraftModVariables.PlayerVariables pVars = pTarget.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
                targetIsItadori = pVars.PlayerCurseTechnique == 21.0 || pVars.PlayerCurseTechnique2 == 21.0;
            } else {
                targetIsItadori = target_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:death_painting")));
            }
            if (targetIsItadori) logic_a = false;
        }

        // Group Protections
        boolean eSukuna = event_owner instanceof LivingEntity le && le.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
        boolean tSukuna = target_owner instanceof LivingEntity lex && lex.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
        
        TagKey<EntityType<?>> group1 = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_1"));
        TagKey<EntityType<?>> zenin = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:zenin"));

        if ((
                (event_owner.getType().is(group1) || (event_owner instanceof Player && event_owner.getPersistentData().getBoolean("CurseUser") && !eSukuna))
                && (target_owner.getType().is(group1) || (target_owner instanceof Player && target_owner.getPersistentData().getBoolean("CurseUser") && !tSukuna))
            )
            || (event_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_2"))) && target_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_2"))))
            || (event_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_3"))) && target_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_3"))))
            || (event_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_4"))) && target_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_4"))))
            || (event_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_5"))) && target_owner.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:group_5"))))
            || (event_owner.getType().is(zenin) && target_owner.getType().is(zenin))) {
            logic_a = false;
        }

        // Target's Target logic
        Entity targetsTarget = target instanceof Mob m ? m.getTarget() : null;
        if (targetsTarget instanceof LivingEntity && targetData.getDouble("cnt_target") > 6.0) {
            if (friendNum != 0.0 && friendNum == targetsTarget.getPersistentData().getDouble("friend_num")) logic_a = true;
            if (event_owner.isAlliedTo(targetsTarget)) logic_a = true;
            if (targetsTarget == entity) logic_a = true;
        }

        if (event_owner == target_owner) logic_a = false;

        // Ten Shadows logic
        TagKey<EntityType<?>> tenShadows = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:ten_shadows_technique"));
        if (entity.getType().is(tenShadows) && !entityData.getBoolean("Ambush")) {
            if (!target.getType().is(tenShadows)) logic_a = true;
            if (entity instanceof RabbitEscapeEntity && target instanceof RabbitEscapeEntity) logic_a = false;
        }

        cir.setReturnValue(logic_a);
    }
}
