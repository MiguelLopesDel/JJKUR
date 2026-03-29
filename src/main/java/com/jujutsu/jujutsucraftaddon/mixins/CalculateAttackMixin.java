package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CalculateAttackProcedure.class, priority = -10000)
public abstract class CalculateAttackMixin {

    /**
     * @author Satushi / Refactored for v43 with AI Preservation
     * @reason Halve the combat cooldown and preserve original AI hesitation and behavior
     */
    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.QUAKE.get())) {
            ci.cancel();
            return;
        }

        ci.cancel();

        boolean success = false;
        boolean danger = false;
        boolean logic_attack = false;
        boolean can_run_attack = false;
        boolean can_bullet_attack = false;
        boolean can_jump_attack = false;
        boolean can_overhead_attack = false;
        boolean can_swim_attack = false;
        boolean cooltime_combat = false;
        boolean can_speed_attack = false;
        boolean cooltime_default = false;
        boolean logic_rifle = false;
        double rnd = 0.0;
        double distance1 = 0.0;
        double ticks = 0.0;
        double level = 0.0;
        double weapon_size = 0.0;
        double range = 0.0;
        double x_pos = 0.0;
        double attack_reach = 0.0;
        double pitch = 0.0;
        double y_pos = 0.0;
        double z_pos = 0.0;
        double yaw = 0.0;
        double dis = 0.0;

        cooltime_combat = entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get());
        cooltime_default = (
              entity instanceof LivingEntity _livEnt && _livEnt.hasEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())
                 ? _livEnt.getEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get()).getAmplifier()
                 : 0
           )
           > 0;

        if ((!cooltime_combat || !cooltime_default)
           && !(entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()))
           && entity.getPersistentData().getDouble("skill") == 0.0) {
           
           weapon_size = GetWeaponSizeProcedure.execute(entity);
           attack_reach = GetReachProcedure.execute(entity);
           range = ReturnEntitySizeProcedure.execute(entity);
           range = range * range * Math.sqrt(weapon_size * attack_reach);
           yaw = Math.toRadians(entity.getYRot() + 90.0F);
           pitch = Math.toRadians(entity.getXRot());
           dis = 2.5 * range;
           x_pos = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * dis;
           y_pos = entity.getY() + entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * dis;
           z_pos = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * dis;
           Vec3 _center = new Vec3(x_pos, y_pos, z_pos);

           for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(6.0 * range / 2.0), e -> true)) {
              if (entity != entityiterator) {
                 if (entityiterator instanceof Projectile) {
                    if (DetectEnemyProjectileProcedure.execute(entity, entityiterator)) {
                       danger = true;
                       break;
                    }
                 } else {
                    logic_attack = LogicAttackProcedure.execute(world, entity, entityiterator);
                    if (logic_attack
                       && (
                          !(entity instanceof LivingEntity _livEnt12 && _livEnt12.hasEffect((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get()))
                             || entity instanceof LivingEntity _livEnt13 && _livEnt13.hasEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())
                             || AntiInfinityProcedure.execute(entityiterator)
                       )) {
                       if (entityiterator.getPersistentData().getDouble("Damage") > 0.0
                          && entityiterator.getPersistentData().getDouble("skill") > 0.0
                          && entityiterator.isAlive()) {
                          danger = true;
                          break;
                       }

                       if (logic_attack && entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
                          danger = true;
                          break;
                       }
                    }
                 }
              }
           }

           distance1 = GetDistanceProcedure.execute(entity);
           can_bullet_attack = !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:cant_barrage_attack")))
              && !cooltime_default
              && (
                    entity instanceof LivingEntity _livEntx && _livEntx.hasEffect((MobEffect)JujutsucraftModMobEffects.SPECIAL.get())
                       ? _livEntx.getEffect((MobEffect)JujutsucraftModMobEffects.SPECIAL.get()).getAmplifier()
                       : 0
                 )
                 < 1;
           can_jump_attack = (
                 entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:jumping_attackable")))
                    || (
                          entity instanceof LivingEntity _livEntxx && _livEntxx.hasEffect((MobEffect)JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get())
                             ? _livEntxx.getEffect((MobEffect)JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get()).getAmplifier()
                             : 0
                       )
                       > 3
                    || entity instanceof LivingEntity _livEnt23 && _livEnt23.hasEffect((MobEffect)JujutsucraftModMobEffects.INSECT_ARMOR_TECHNIQUE.get())
              )
              && !cooltime_combat
              && distance1 > 8.0
              && distance1 < 32.0
              && !danger;
           can_run_attack = (
                 entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:running_attackable")))
                    || (
                          entity instanceof LivingEntity _livEntxxxx
                                && _livEntxxxx.hasEffect((MobEffect)JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get())
                             ? _livEntxxxx.getEffect((MobEffect)JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get()).getAmplifier()
                             : 0
                       )
                       > 3
                    || entity instanceof LivingEntity _livEnt26 && _livEnt26.hasEffect((MobEffect)JujutsucraftModMobEffects.MYTHICAL_BEAST_AMBER_EFFECT.get())
                    || entity instanceof LivingEntity _livEnt27
                       && _livEnt27.hasEffect((MobEffect)JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get())
              )
              && !cooltime_combat
              && (
                    entity instanceof LivingEntity _livEntxxx && _livEntxxx.hasEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())
                       ? _livEntxxx.getEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier()
                       : 0
                 )
                 < 1
              && !(entity instanceof LivingEntity _livEnt29 && _livEnt29.hasEffect(MobEffects.HUNGER));
           can_overhead_attack = (
                 entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:overhead_attackable")))
                    || (
                          entity instanceof LivingEntity _livEntxxxxx
                                && _livEntxxxxx.hasEffect((MobEffect)JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get())
                             ? _livEntxxxxx.getEffect((MobEffect)JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get()).getAmplifier()
                             : 0
                       )
                       > 3
                    || entity instanceof LivingEntity _livEnt32 && _livEnt32.hasEffect((MobEffect)JujutsucraftModMobEffects.INSECT_ARMOR_TECHNIQUE.get())
                    || entity instanceof LivingEntity _livEnt33
                       && _livEnt33.hasEffect((MobEffect)JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get())
              )
              && !cooltime_combat
              && distance1 < 8.0;
           can_speed_attack = (
                 entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:speed_attackable"))) && Math.random() < 0.1
                    || entity instanceof LivingEntity _livEnt35 && _livEnt35.hasEffect((MobEffect)JujutsucraftModMobEffects.INSECT_ARMOR_TECHNIQUE.get())
                    || entity instanceof LivingEntity _livEnt36
                       && _livEnt36.hasEffect((MobEffect)JujutsucraftModMobEffects.INSTANT_SPIRIT_BODYOF_DISTORTED_KILLING_EFFECT.get())
              )
              && !cooltime_combat
              && distance1 > 8.0
              && distance1 < 24.0;
           logic_rifle = LogicRifleProcedure.execute(world, entity);

           for (int index0 = 0; index0 < 255; index0++) {
              if (cooltime_combat) {
                 rnd = Mth.nextInt(RandomSource.create(), 1, 3);
              } else if (cooltime_default) {
                 rnd = Mth.nextInt(RandomSource.create(), 4, 7);
              } else {
                 rnd = Mth.nextInt(RandomSource.create(), 1, 7);
              }

              if (rnd >= 1.0 && rnd <= 3.0) {
                 if (cooltime_default || rnd == 1.0 && Math.random() < (can_bullet_attack ? 0.5 : 0.75)) {
                    continue;
                 }

                 if (rnd == 2.0) {
                 }

                 if (rnd == 3.0 && !can_bullet_attack) {
                    continue;
                 }
              }

              if (!(rnd >= 4.0)
                 || !(rnd <= 7.0)
                 || !cooltime_combat
                    && (rnd != 4.0 || can_jump_attack && !(Math.random() < 0.5))
                    && (rnd != 5.0 || can_run_attack)
                    && (rnd != 6.0 || can_overhead_attack && !(Math.random() < 0.75))
                    && (rnd != 7.0 || can_speed_attack && !(Math.random() < 0.75))) {
                 
                 if (distance1 > 20.0 * range) {
                    if (rnd == 4.0 || rnd == 5.0) {
                       success = true;
                       break;
                    }
                 } else if (distance1 > 10.0 * range) {
                    if (rnd == 4.0 || rnd == 5.0 || rnd == 7.0) {
                       success = true;
                       break;
                    }
                 } else if (distance1 > 5.0 * range) {
                    if (rnd == 4.0 || rnd == 5.0 || rnd == 6.0 || rnd == 7.0) {
                       success = true;
                       break;
                    }
                 } else if (distance1 > 2.5 * range) {
                    if (rnd == 1.0 || rnd == 2.0 || rnd == 3.0 || rnd == 5.0 || rnd == 6.0) {
                       success = true;
                       break;
                    }
                 } else if (rnd == 1.0 || rnd == 2.0 || rnd == 3.0 || rnd == 5.0 || rnd == 6.0) {
                    success = true;
                    break;
                 }

                 if (danger) {
                    if ((rnd == 3.0 || !(Math.random() < 0.75)) && rnd != 2.0 && rnd != 4.0) {
                       success = true;
                       break;
                    }
                 } else if (logic_rifle && distance1 > 6.0 && distance1 < 24.0 && (rnd == 1.0 || rnd == 2.0 || rnd == 3.0)) {
                    success = true;
                    break;
                 }
              }
           }
        } else {
           success = false;
        }

        if (success) {
           entity.getPersistentData().putDouble("cnt_x", Math.max(entity.getPersistentData().getDouble("cnt_x"), 0.0));
           if (rnd == 1.0) {
              ticks = 5.0;
              level = 1.0;
           } else if (rnd == 2.0) {
              ticks = 15.0;
              level = 1.0;
           } else if (rnd == 3.0) {
              ticks = 20.0;
              level = 1.0;
           } else if (rnd == 4.0) {
              ticks = 100.0;
              level = 0.0;
           } else if (rnd == 5.0) {
              ticks = 200.0;
              level = 0.0;
           } else if (rnd == 6.0) {
              ticks = 50.0;
              level = 0.0;
           } else if (rnd == 7.0) {
              ticks = 50.0;
              level = 0.0;
           }

           ResetCounterProcedure.execute(entity);
           entity.getPersistentData().putDouble("skill", 4199.0 + rnd);
           if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
              _entity.addEffect(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
           }

           if (level > 0.0) {
              if ((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getItem()
                 == JujutsucraftModItems.SUKUNA_BODY_CHESTPLATE.get()) {
                 ticks *= 0.5;
              }

              if (entity instanceof LivingEntity _livingEntity45 && _livingEntity45.getAttributes().hasAttribute(Attributes.ATTACK_SPEED)) {
                 ticks += 20.0
                    * Math.max(
                       1.7
                          - (
                             entity instanceof LivingEntity _livingEntity46 && _livingEntity46.getAttributes().hasAttribute(Attributes.ATTACK_SPEED)
                                ? _livingEntity46.getAttribute(Attributes.ATTACK_SPEED).getValue()
                                : 0.0
                          ),
                       0.0
                    );
              }

              if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                 _entity.addEffect(
                    new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int)Math.round(ticks / 2.0), 1, false, false)
                 );
              }
           } else if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
              _entity.addEffect(
                 new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int)Math.round(ticks / 2.0), 0, false, false)
              );
           }

           if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_domain_amplification")))) {
              LivingEntity target = entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null;
              if (target != null && target.hasEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
                 if (entity instanceof LivingEntity _entity) {
                    _entity.removeEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
                 }
                 KeyDomainAmplificationOnKeyPressedProcedure.execute(entity);
              }
           }
        } else {
           entity.getPersistentData().putDouble("cnt_x", Math.max(entity.getPersistentData().getDouble("cnt_x"), 0.0));
        }
    }
}
