package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import net.mcreator.jujutsucraft.entity.FushiguroTojiBugEntity;
import net.mcreator.jujutsucraft.entity.SukunaEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.GetEntityAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.LogicSwordProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationEntity2Procedure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;

import javax.annotation.Nullable;

public class PlayAnimationEntity {

    public static void execute(@Nullable Event event, LevelAccessor world, DamageSource damagesource, Entity entity) {
        if (damagesource == null || entity == null) return;

        ResourceLocation animDamage = new ResourceLocation("jujutsucraft:start_animation");
        if (!damagesource.is(ResourceKey.create(Registries.DAMAGE_TYPE, animDamage))) return;

        if (!(entity instanceof GeoEntity) || !(entity instanceof LivingEntity livingEntity)) return;

        Attribute anim1Attr = JujutsucraftModAttributes.ANIMATION_1.get();
        Attribute anim2Attr = JujutsucraftModAttributes.ANIMATION_2.get();

        if (!livingEntity.getAttributes().hasAttribute(anim1Attr)) return;

        double num1 = livingEntity.getAttribute(anim1Attr).getBaseValue();
        double num2 = livingEntity.getAttributes().hasAttribute(anim2Attr) ? livingEntity.getAttribute(anim2Attr).getBaseValue() : 0.0;

        double processedNum1 = num1 + ((num1 <= -50.0 && num1 >= -100.0) ? 100 : 0);
        double num3 = processedNum1 >= 0.0 ? processedNum1 % 100.0 : 100.0;
        boolean sword = LogicSwordProcedure.execute(entity);
        String animeName = "";
        String side = GetEntityAnimationProcedure.execute(entity);

        if (side.contains("_right")) side = "left";
        else if (side.contains("_left")) side = "right";
        else side = Math.random() > 0.5 ? "right" : "left";

        if (num3 >= 0.0 && num3 <= 4.0) {
            if (sword && num2 > 0.0) {
                if (num2 <= 3.0) side = "right";
                else if (num2 <= 6.0) side = "left";
            }

            if (num3 != 0.0 && num3 != 2.0 && num3 != 3.0) {
                if (num3 == 1.0) {
                    animeName = sword ? "sword_to_right" : (Math.random() < 0.25 ? "punch_" : "kick_") + side;
                } else if (num3 == 4.0) {
                    if (num2 == 0.0 || num2 == 4.0) animeName = sword ? "sword_overhead" : "punch_overhead";
                    else if (num2 == 1.0 || num2 == 5.0) animeName = sword ? "sword_overhead2" : "punch_overhead2";
                    else if (num2 == 2.0) animeName = "punch_overhead";
                    else if (num2 == 3.0) animeName = "punch_overhead2";
                }
            } else if (sword) {
                animeName = "sword_to_" + side;
            } else {
                animeName = (!(Math.random() < 0.25) && (num2 <= 0.0 || num2 >= 100.0) ? "kick_" : "punch_") + side;
            }
        } else if (num3 == 20.0) {
            animeName = (num1 == 220.0) ? "red" : "domain_expansion1";
        } else if (num1 < 0.0) {
            if (num1 >= -10.0) {
                if (num1 == -1.0) animeName = "backstep";
                else if (num1 == -2.0) animeName = "death";
                else if (num1 == -3.0) animeName = "right_arm_up";
                else if (num1 == -4.0) animeName = "both_arm_front";
                else if (num1 == -5.0) animeName = (num2 == 1.0) ? "sword_to_left" : "sword_to_right";
                else if (num1 == -6.0) {
                    if (num2 == 1.0) animeName = "kashimo_kick";
                    else if (num2 == 2.0) animeName = "kick_right";
                    else if (num2 == 3.0) animeName = "kick_left";
                    else if (num2 == 4.0) animeName = "kick_rotate4";
                    else animeName = "kick_" + side;
                } else if (num1 == -7.0) {
                    if (num2 == 1.0) animeName = "combo1";
                    else if (num2 == 2.0) animeName = "combo2";
                    else if (num2 == 3.0) animeName = "combo3";
                    else animeName = "punch_" + side;
                } else if (num1 == -8.0) {
                    if (num2 == 1.0) animeName = "rotation2";
                    else if (num2 == 2.0) animeName = "ragnaraku2";
                    else animeName = "rotation";
                } else if (num1 == -9.0) animeName = "guard";
                else if (num1 == -10.0) animeName = "fall1";
            } else if (num1 >= -15.0) {
                animeName = "dance" + Math.round(num1 + 16.0);
            } else if (num1 >= -20.0) {
                if (num1 == -16.0) animeName = (num2 == 0.0) ? "simple_domain1" : "simple_domain2";
                else if (num1 == -17.0) animeName = "clap";
                else if (num1 == -18.0) animeName = "fly";
                else if (num1 == -19.0) animeName = (num2 == 1.0) ? "breath2" : "breath1";
                else if (num1 == -20.0) {
                    if (num2 == 1.0) animeName = "step_right";
                    else if (num2 == 2.0) animeName = "step_left";
                    else if (num2 == 3.0) animeName = "step_front";
                    else animeName = "step_back";
                }
            } else if (num1 >= -25.0) {
                if (num1 == -21.0) animeName = "invisibility";
                else if (num1 == -22.0 && num2 == 1.0) animeName = "swim_butterfly";
            } else if (num1 == -49.0) {
                animeName = "empty";
            }
        } else {
            if (num1 == 107.0) {
                if (entity instanceof SukunaFushiguroEntity || entity instanceof SukunaEntity || entity instanceof SukunaPerfectEntity) {
                    animeName = livingEntity.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) ? "fuga2" : "fuga1";
                } else {
                    animeName = "fuga";
                }
            } else if (num1 == 207.0) {
                if (entity.onGround()) {
                    if (entity instanceof SukunaFushiguroEntity || entity instanceof SukunaEntity || entity instanceof SukunaPerfectEntity) {
                        animeName = "sukuna" + Mth.nextInt(RandomSource.create(), 1, 26);
                    } else {
                        animeName = "red";
                    }
                } else {
                    if (entity instanceof SukunaFushiguroEntity || entity instanceof SukunaEntity || entity instanceof SukunaPerfectEntity) {
                        animeName = "dismantleback" + Mth.nextInt(RandomSource.create(), 1, 2);
                    } else {
                        animeName = "red5";
                    }
                }
            } else if (num1 == 215.0) {
                animeName = (num2 == 1.0) ? "charge2" : "charge1";
            } else if (num1 == 618.0) {
                animeName = "ten_shadows_technique_mahoraga";
            } else if (num1 == 406.0) {
                animeName = "burn_out";
            } else if (num1 == 415.0) {
                animeName = "meteor";
            } else if (num1 == 240.0) {
                animeName = "worldslash" + Mth.nextInt(RandomSource.create(), 2, 4);
            } else if (num1 == 1706.0) {
                animeName = "kick_flying";
            } else if (num1 == 1715.0) {
                animeName = "wifi";
            } else if (num1 == 2015.0) {
                animeName = "plus_ultra";
            } else if (num1 == 20010.0) {
                animeName = "murasaki";
            }
        }

        ItemStack mainHand = livingEntity.getMainHandItem();
        boolean isWukongWeapon = mainHand.getItem() == JujutsucraftaddonModItems.WUKONG_STAFF_TRUE.get().asItem()
                || mainHand.getItem() == JujutsucraftaddonModItems.HAMMER_WUKONG.get().asItem()
                || mainHand.getItem() == JujutsucraftaddonModItems.HALBERD_WUKONG.get().asItem()
                || mainHand.getItem() == JujutsucraftaddonModItems.SWORD_WUKONG.get().asItem()
                || mainHand.getItem() == JujutsucraftaddonModItems.GREAT_SWORD_WUKONG.get().asItem();

        if (isWukongWeapon) {
            animeName = "wu" + Mth.nextInt(RandomSource.create(), 1, 17);
        }

        if (!animeName.isEmpty()) {
            if (entity instanceof FushiguroTojiBugEntity toji) {
                if (!"playfulrush".equals(toji.animationprocedure)) {
                    PlayAnimationEntity2Procedure.execute(entity, animeName);
                }
            } else {
                PlayAnimationEntity2Procedure.execute(entity, animeName);
            }
        }

        livingEntity.getAttribute(anim1Attr).setBaseValue(0.0);
        if (livingEntity.getAttributes().hasAttribute(anim2Attr)) {
            livingEntity.getAttribute(anim2Attr).setBaseValue(0.0);
        }

        if (event != null && event.isCancelable()) {
            event.setCanceled(true);
        }
    }
}
