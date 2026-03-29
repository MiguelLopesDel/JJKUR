package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.entity.GojoSatoruSchoolDaysEntity;
import net.mcreator.jujutsucraft.entity.UroTakakoEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AIActive2Procedure.class, priority = -10000)
public abstract class AIActive2ProcedureMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        // Se a entidade tiver o efeito QUAKE do addon, cancelamos a IA inteligente (Propósito original)
        if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.QUAKE.get())) {
            ci.cancel();
            return;
        }

        if (entity != null) {
            ci.cancel(); // Cancelamos o original para injetar nossa versão atualizada

            Entity target_entity = null;
            ItemStack mainItem = ItemStack.EMPTY;
            ItemStack offItem = ItemStack.EMPTY;
            boolean logic_guard = false;
            boolean logic_heal = false;
            boolean logic_heal_cancel = true;
            boolean test = false;
            boolean target = false;
            boolean logic_avoid = false;
            boolean using = false;
            boolean output = false;
            boolean infinity = false;
            boolean using2 = false;
            double x_knockback = 0.0;
            double y_knockback = 0.0;
            double z_knockback = 0.0;
            double dis = 0.0;
            double old_skill = 0.0;
            double limit = 0.0;
            double distance = 0.0;
            double y_power = 0.0;
            double z_power = 0.0;
            double fix = 0.0;
            double x_power = 0.0;
            double speed = 0.0;
            double level_neutralization = 0.0;

            LivingEntity var54;
            if (entity instanceof Mob _mobEnt) {
                var54 = _mobEnt.getTarget();
            } else {
                var54 = null;
            }

            label754: {
                entity.getPersistentData().putDouble("cnt_guard", Math.max(entity.getPersistentData().getDouble("cnt_guard") - 1.0, 0.0));
                entity.getPersistentData().putDouble("cnt_backstep", Math.max(entity.getPersistentData().getDouble("cnt_backstep") - 1.0, 0.0));
                if (entity instanceof LivingEntity _livEnt) {
                    if (_livEnt.hasEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                        level_neutralization = (double)_livEnt.getEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier();
                        break label754;
                    }
                }
                level_neutralization = 0.0;
            }

            if (entity instanceof LivingEntity _livEnt6) {
                if (_livEnt6.hasEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get()) && level_neutralization == 37.0) {
                    return;
                }
            }

            if (entity.getPersistentData().getDouble("cnt_target") > 6.0) {
                test = entity.getPersistentData().getDouble("cnt_target") % 5.0 == 4.0;
                target = var54 instanceof LivingEntity;
            }

            distance = 99.0;
            if (entity.getPersistentData().getDouble("skill") == 0.0) {
                if (target) {
                    label743: {
                        label742: {
                            distance = GetDistanceProcedure.execute(entity);
                            if (entity instanceof LivingEntity _livEnt11) {
                                if (_livEnt11.hasEffect((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
                                    if (!_livEnt11.hasEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                                        infinity = true;
                                        break label742;
                                    }
                                }
                            }
                            infinity = false;
                        }
                    }

                    if (test) {
                        logic_heal_cancel = false;
                    }

                    label761: {
                        label762: {
                            if (entity instanceof LivingEntity _livEnt13 && _livEnt13.hasEffect((MobEffect)JujutsucraftModMobEffects.DAMAGE_EFFECT.get())) {
                                break label762;
                            }

                            if (((Entity)var54).getPersistentData().getDouble("skill") == 0.0 || !(((Entity)var54).getPersistentData().getDouble("Damage") > 0.0)) {
                                break label761;
                            }
                        }

                        if (!infinity || AntiInfinityProcedure.execute(var54)) {
                            logic_heal_cancel = true;
                            if (test) {
                                logic_guard = true;
                            }
                        }
                    }

                    if (test) {
                        if (!logic_guard) {
                            label716: {
                                if (entity instanceof LivingEntity _livEnt16 && _livEnt16.hasEffect((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                                    break label716;
                                }

                                if (level_neutralization > 0.0 && level_neutralization != 37.0) {
                                    logic_guard = true;
                                }
                            }
                        }

                        if (!logic_guard) {
                            label709: {
                                dis = 24.0;
                                if (!(entity instanceof UroTakakoEntity)) {
                                    if (!(entity instanceof LivingEntity _livEnt18 && _livEnt18.hasEffect((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get()))) {
                                        break label709;
                                    }
                                }
                                dis = 36.0;
                            }

                            Vec3 center = new Vec3(x, y, z);
                            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, (new AABB(center, center)).inflate(dis / 2.0), (e) -> true)) {
                                if (entity != entityiterator && (!infinity || AntiInfinityProcedure.execute(entityiterator))) {
                                    if (entityiterator instanceof Projectile) {
                                        if (DetectEnemyProjectileProcedure.execute(entity, entityiterator)) {
                                            logic_avoid = true;
                                            logic_guard = true;
                                            break;
                                        }
                                    } else if ((entityiterator.getPersistentData().getDouble("skill") != 0.0 || entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) && entityiterator.getPersistentData().getDouble("Damage") > 0.0 && LogicAttackProcedure.execute(world, entity, entityiterator)) {
                                        logic_avoid = true;
                                        logic_guard = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!logic_guard) {
                            dis = 16.0;
                            if (entity instanceof UroTakakoEntity) {
                                dis = 32.0;
                            }

                            Vec3 center = new Vec3(x, y, z);
                            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, (new AABB(center, center)).inflate(dis / 2.0), (e) -> true)) {
                                if (entity != entityiterator && (!infinity || AntiInfinityProcedure.execute(entityiterator)) && entityiterator.getPersistentData().getDouble("skill") != 0.0 && (entityiterator.getPersistentData().getDouble("Damage") > 0.0 || entityiterator.getPersistentData().getBoolean("PRESS_Z")) && LogicAttackProcedure.execute(world, entity, entityiterator)) {
                                    logic_avoid = true;
                                    logic_guard = true;
                                    break;
                                }
                            }
                        }

                        if (logic_avoid) {
                            label768: {
                                if (entity.getPersistentData().getDouble("skill") != 0.0) {
                                    logic_avoid = false;
                                }

                                if (entity instanceof LivingEntity _livEnt32 && _livEnt32.hasEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())) {
                                    int duration = _livEnt32.getEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get()).getDuration();
                                    if (duration < 6) {
                                        logic_avoid = false;
                                    }
                                    break label768;
                                }
                                logic_avoid = false;
                            }
                        }

                        if (entity.getPersistentData().getBoolean("CursedSpirit") && entity instanceof LivingEntity _livEnt35) {
                            if (_livEnt35.hasEffect((MobEffect)JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get())) {
                                logic_avoid = true;
                            }
                        }
                    }

                    logic_heal_cancel = logic_heal_cancel || logic_guard;
                    if (!logic_avoid && (!infinity || AntiInfinityProcedure.execute(var54))) {
                        int cooldown = 0;
                        if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME.get())) {
                            cooldown = _livEnt.getEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME.get()).getDuration();
                        }

                        if (cooldown > 10 || entity.getPersistentData().getDouble("cnt_x") < 0.0) {
                            int combat_amp = 0;
                            if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())) {
                                combat_amp = _livEnt.getEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get()).getAmplifier();
                            }

                            if (combat_amp > 0) {
                                int combat_duration = 0;
                                if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())) {
                                    combat_duration = _livEnt.getEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get()).getDuration();
                                }
                                if (combat_duration > 10 && distance < 6.0) {
                                    logic_avoid = true;
                                }
                            }

                            if (entity instanceof LivingEntity _livEnt40 && _livEnt40.hasEffect((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get()) && distance < 24.0 && ((Entity)var54).getPersistentData().getDouble("skill") != 0.0 && ((Entity)var54).getPersistentData().getDouble("skill") > -900.0) {
                                logic_avoid = true;
                            }
                        }
                    }

                    if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:no_guard")))) {
                        logic_guard = false;
                    }

                    logic_heal_cancel = logic_heal_cancel || logic_avoid;
                } else {
                    logic_heal_cancel = false;
                }
            }

            if (logic_guard) {
                StartGuardProcedure.execute(world, entity);
                entity.setShiftKeyDown(true);
                entity.getPersistentData().putDouble("cnt_guard", 20.0);
                if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_domain_amplification")))) {
                    label612: {
                        if (var54 instanceof LivingEntity) {
                            if (!(((Entity)var54).getPersistentData().getDouble("skill") % 100.0 > 2.0) || ((Entity)var54).getPersistentData().getBoolean("attack")) {
                                break label612;
                            }
                            if (!var54.hasEffect((MobEffect)JujutsucraftModMobEffects.ATTACKING.get())) {
                                break label612;
                            }
                        }

                        if (!entity.level().isClientSide() && entity.getServer() != null) {
                            float amp = 0.0F;
                            if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.DAMAGE_BOOST)) {
                                amp = (float)_livEnt.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
                            }

                            CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), entity.level() instanceof ServerLevel ? (ServerLevel)entity.level() : null, 4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity);
                            // Propósito: Addon logic (amp + 4)
                            entity.getServer().getCommands().performPrefixedCommand(source, "effect give @s jujutsucraft:domain_amplification 1 " + Math.round(amp + 4) + " false");
                        }
                    }
                }
            }else{
                entity.setShiftKeyDown(false);
            }

            if (logic_avoid && !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:no_guard")))) {
                if (entity.onGround() && entity.getPersistentData().getDouble("cnt_target") > 6.0) {
                    entity.getPersistentData().putBoolean("PRESS_S", true);
                    WhenBackStepProcedure.execute(world, entity);
                    entity.getPersistentData().putBoolean("PRESS_S", false);
                    entity.getPersistentData().putDouble("cnt_backstep", 20.0);
                    if (entity instanceof Mob _mob) {
                        _mob.getNavigation().stop();
                    }
                }

                if (entity instanceof LivingEntity _livEnt59 && _livEnt59.hasEffect((MobEffect)JujutsucraftModMobEffects.FLY_EFFECT.get())) {
                    entity.getPersistentData().putDouble("mode_fly", (double)(Math.random() < 0.5 ? -2 : -3));
                }
            }

            if (test && (logic_avoid || Math.random() < 0.2) && GetDistanceProcedure.execute(entity) > 8.0 && entity instanceof LivingEntity _livEnt61) {
                if (_livEnt61.hasEffect((MobEffect)JujutsucraftModMobEffects.DOUBLE_JUMP_EFFECT.get())) {
                    KeySpaceOnKeyPressedProcedure.execute(world, x, y, z, entity);
                }
            }

            float maxHealth = (entity instanceof LivingEntity _liv) ? _liv.getMaxHealth() : -1.0F;
            label810: {
                limit = (double)(maxHealth >= 800.0F ? 400 : 200);
                if (!entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_reverse_cursed_technique"))) && (!entity.getPersistentData().contains("entity_can_use_rct") || !entity.getPersistentData().getBoolean("entity_can_use_rct"))) {
                    if (!(entity instanceof GojoSatoruSchoolDaysEntity _datEntL66 && (Boolean)_datEntL66.getEntityData().get(GojoSatoruSchoolDaysEntity.DATA_awaking))) {
                        break label810;
                    }
                }

                output = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_reverse_cursed_technique_output")));
                if (!logic_heal_cancel) {
                    if (distance < 8.0) {
                        label775: {
                            logic_heal_cancel = true;
                            float currentHealth = (entity instanceof LivingEntity _liv) ? _liv.getHealth() : -1.0F;
                            if (currentHealth <= maxHealth * 0.5) {
                                logic_heal_cancel = false;
                            }

                            if (entity instanceof LivingEntity _livEnt70 && _livEnt70.hasEffect((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                break label775;
                            }

                            int neutralization_amp = 0;
                            if (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                                neutralization_amp = _livEnt.getEffect((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier();
                            }

                            if (neutralization_amp >= 1) {
                                logic_heal_cancel = false;
                            }
                        }
                    }

                    float currentHealth = (entity instanceof LivingEntity _liv) ? _liv.getHealth() : -1.0F;
                    if (currentHealth >= maxHealth) {
                        logic_heal_cancel = true;
                    }
                }

                if (entity.getPersistentData().getDouble("cnt_reverse_lim") + 1.0 >= limit) {
                    logic_heal_cancel = true;
                    output = false;
                }

                if (output) {
                    output = false;
                    if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:cant_combat"))) || entity.getPersistentData().getDouble("cnt_target") <= 6.0) {
                        dis = (double)(entity.getBbWidth() * 1.0F);
                        using = false;
                        Vec3 center = new Vec3(entity.getX() + entity.getLookAngle().x * dis, entity.getY() + (double)entity.getBbHeight() * 0.9 + entity.getLookAngle().y * dis, entity.getZ() + entity.getLookAngle().z * dis);

                        for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, (new AABB(center, center)).inflate(dis * 3.0 / 2.0), (e) -> true)) {
                            if (entityiterator instanceof LivingEntity _livIter && !entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:not_living"))) && entityiterator.isAlive() && entity != entityiterator) {
                                using = false;
                                if (LogicAttackProcedure.execute(world, entity, entityiterator)) {
                                    if (entityiterator.getPersistentData().getBoolean("CursedSpirit")) {
                                        using = true;
                                    }
                                } else if (!entityiterator.getPersistentData().getBoolean("CursedSpirit")) {
                                    if (_livIter.getHealth() < _livIter.getMaxHealth()) {
                                        using = true;
                                    }
                                }

                                if (using) {
                                    output = true;
                                    logic_heal_cancel = false;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (logic_heal_cancel) {
                    logic_heal = false;
                } else {
                    logic_heal = true;
                    if (output) {
                        entity.getPersistentData().putDouble("cnt_reverse_test", Math.max(entity.getPersistentData().getDouble("cnt_reverse_test"), 100.0));
                    }
                }

                entity.getPersistentData().putDouble("cnt_reverse_test", entity.getPersistentData().getDouble("cnt_reverse_test") + 1.0);
                if (logic_heal) {
                    if (entity.getPersistentData().getDouble("cnt_reverse_test") > 100.0) {
                        label804: {
                            entity.getPersistentData().putDouble("cnt_reverse_test", 0.0);
                            if (entity instanceof LivingEntity _livEnt90 && _livEnt90.hasEffect((MobEffect)JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get())) {
                                break label804;
                            }

                            if (!entity.getPersistentData().getBoolean("PRESS_M")) {
                                if (entity instanceof LivingEntity _living) {
                                    if (!_living.level().isClientSide()) {
                                        _living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 18, 9, false, false));
                                    }
                                }
                                entity.getPersistentData().putDouble("cnt_reverse", 15.0);
                                KeyReverseCursedTechniqueOnKeyPressedProcedure.execute(entity);
                            }
                        }
                    }
                } else if (!target) {
                    entity.getPersistentData().putDouble("cnt_reverse_lim", Math.max(entity.getPersistentData().getDouble("cnt_reverse_lim") - 0.1, 0.0));
                }

                if (entity.getPersistentData().getBoolean("PRESS_M")) {
                    if (entity instanceof LivingEntity _livEnt97 && _livEnt97.hasEffect((MobEffect)JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get())) {
                        entity.getPersistentData().putDouble("cnt_reverse_lim", entity.getPersistentData().getDouble("cnt_reverse_lim") + 1.0);
                    }

                    entity.getPersistentData().putDouble("cnt_reverse", Math.max(entity.getPersistentData().getDouble("cnt_reverse") - 1.0, 0.0));
                    if (!logic_heal) {
                        label528: {
                            float currentHealth = (entity instanceof LivingEntity _liv) ? _liv.getHealth() : -1.0F;
                            if (!(entity.getPersistentData().getDouble("cnt_reverse") <= 0.0) && !(entity.getPersistentData().getDouble("cnt_reverse_lim") >= limit)) {
                                if (!(currentHealth >= maxHealth) || output) {
                                    break label528;
                                }
                            }
                            entity.getPersistentData().putDouble("cnt_reverse", 0.0);
                            KeyReverseCursedTechniqueOnKeyReleasedProcedure.execute(entity);
                        }
                    }
                }
            }

            if (target && entity.getPersistentData().getDouble("skill") == 0.0) {
                entity.getPersistentData().putDouble("cnt_weapon_main", entity.getPersistentData().getDouble("cnt_weapon_main") + 1.0);
                entity.getPersistentData().putDouble("cnt_weapon_off", entity.getPersistentData().getDouble("cnt_weapon_off") + 1.0);
                using = false;
                using2 = false;
                mainItem = ItemStack.EMPTY.copy();
                offItem = ItemStack.EMPTY.copy();

                if (entity.getPersistentData().getDouble("cnt_weapon_main") > 20.0) {
                    entity.getPersistentData().putDouble("cnt_weapon_main", 0.0);
                    entity.getPersistentData().putDouble("cnt_weapon_off", Math.min(entity.getPersistentData().getDouble("cnt_weapon_off"), 100.0));
                    if (Math.abs(entity.getPersistentData().getDouble("cnt_weapon_main") - entity.getPersistentData().getDouble("cnt_weapon_off")) < 20.0) {
                        entity.getPersistentData().putDouble("cnt_weapon_main", entity.getPersistentData().getDouble("cnt_weapon_off") - 20.0);
                    }
                    using = true;
                    mainItem = (entity instanceof LivingEntity _liv) ? _liv.getMainHandItem() : ItemStack.EMPTY;
                }

                if (entity.getPersistentData().getDouble("cnt_weapon_off") > 60.0) {
                    entity.getPersistentData().putDouble("cnt_weapon_off", 40.0);
                    if (Math.abs(entity.getPersistentData().getDouble("cnt_weapon_main") - entity.getPersistentData().getDouble("cnt_weapon_off")) < 20.0) {
                        entity.getPersistentData().putDouble("cnt_weapon_off", entity.getPersistentData().getDouble("cnt_weapon_main") - 20.0);
                    }
                    using2 = true;
                    offItem = (entity instanceof LivingEntity _liv) ? _liv.getOffhandItem() : ItemStack.EMPTY;
                }

                if (using ^ using2) {
                    fix = 0.0;
                    if ((using && mainItem.getItem() == JujutsucraftModItems.SUPREME_MARTIAL_SOLUTION.get() || using2 && offItem.getItem() == JujutsucraftModItems.SUPREME_MARTIAL_SOLUTION.get()) && distance < 40.0) {
                        SupremeMartialSolutionRightClickedInAirProcedure.execute(world, entity);
                        fix = 200.0;
                    }
                    if ((using && mainItem.getItem() == JujutsucraftModItems.DAITENGU_FAN.get() || using2 && offItem.getItem() == JujutsucraftModItems.DAITENGU_FAN.get()) && distance < 24.0) {
                        DaitenguFanRightclickedProcedure.execute(world, x, y, z, entity);
                        fix = 200.0;
                    }
                    if ((using && mainItem.getItem() == JujutsucraftModItems.DRAGON_BONE.get() || using2 && offItem.getItem() == JujutsucraftModItems.DRAGON_BONE.get()) && distance < 32.0) {
                        DragonBoneRightclickedProcedure.execute(world, entity);
                        fix = 100.0;
                    }

                    if (fix > 0.0) {
                        if (using) entity.getPersistentData().putDouble("cnt_weapon_main", 20.0 - fix);
                        else if (using2) entity.getPersistentData().putDouble("cnt_weapon_off", 60.0 - fix);
                    }
                }
            }

            if (test && entity.getPersistentData().getDouble("skill") == 0.0 && entity.isInWaterOrBubble() && entity.isSprinting() && entity instanceof LivingEntity _livEntSprint) {
                if (_livEntSprint.getAttributes().hasAttribute((Attribute)ForgeMod.SWIM_SPEED.get())) {
                    if (var54 instanceof LivingEntity) {
                        RotateEntityProcedure.execute(var54.getX(), var54.getY() + (double)var54.getBbHeight() * 0.5, var54.getZ(), entity);
                        if (entity.getAirSupply() < 4) {
                            entity.setXRot(-90.0F);
                        } else {
                            fix = entity.getY();
                            for(int i = 0; i < (int)Math.round(Math.ceil((double)entity.getBbHeight())); ++i) {
                                if (!(world.getBlockState(BlockPos.containing(entity.getX(), fix, entity.getZ())).getBlock() instanceof LiquidBlock)) {
                                    entity.setXRot((float)Math.max((double)entity.getXRot(), 22.5));
                                    break;
                                }
                                fix++;
                            }
                        }
                        entity.setYBodyRot(entity.getYRot());
                        entity.setYHeadRot(entity.getYRot());
                        entity.yRotO = entity.getYRot();
                        entity.xRotO = entity.getXRot();
                        if (entity instanceof LivingEntity _entity) {
                            _entity.yBodyRotO = _entity.getYRot();
                            _entity.yHeadRotO = _entity.getYRot();
                        }
                    }

                    if (!InsideSolidCalculateProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), 1.0, (double)(entity.getBbWidth() + 1.0F))) {
                        fix = 1.0;
                        if (entity instanceof LivingEntity _liv && _liv.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                            int slowdown = _liv.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier();
                            fix = Math.max(fix - 0.15 * (double)(slowdown + 1), 0.0);
                        }

                        double swimSpeed = 0.0;
                        if (entity instanceof LivingEntity _liv && _liv.getAttributes().hasAttribute((Attribute)ForgeMod.SWIM_SPEED.get())) {
                            swimSpeed = _liv.getAttribute((Attribute)ForgeMod.SWIM_SPEED.get()).getValue();
                        }

                        fix *= Math.min(Math.max(swimSpeed, 0.0), 4.0);
                        speed = 0.75 * fix;

                        Vec3 motion = entity.getDeltaMovement();
                        Vec3 look = entity.getLookAngle();
                        x_power = motion.x() + look.x * speed;
                        y_power = motion.y() + look.y * speed;
                        z_power = motion.z() + look.z * speed;

                        x_power = x_power < 0.0 ? Math.min(motion.x(), Math.max(x_power, speed * -1.0)) : Math.max(motion.x(), Math.min(x_power, speed * 1.0));
                        y_power = y_power < 0.0 ? Math.min(motion.y(), Math.max(y_power, speed * -1.0)) : Math.max(motion.y(), Math.min(y_power, speed * 1.0));
                        z_power = z_power < 0.0 ? Math.min(motion.z(), Math.max(z_power, speed * -1.0)) : Math.max(motion.z(), Math.min(z_power, speed * 1.0));

                        entity.setDeltaMovement(new Vec3(x_power, y_power, z_power));
                        if (entity instanceof Mob _mob) {
                            _mob.getNavigation().stop();
                        }
                    }
                }
            }
        }
    }
}
