package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.mcreator.jujutsucraft.JujutsucraftMod;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.GetEntityAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.LogicSwordProcedure;
import net.mcreator.jujutsucraft.procedures.SetupAnimationsProcedure;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class NewPlayAnimation {

    public static void execute(@Nullable Event event, LevelAccessor world, DamageSource damagesource, Entity entity) {
        if (damagesource == null || entity == null) return;

        ResourceLocation animationDamage = new ResourceLocation("jujutsucraft:start_animation");
        if (!damagesource.is(ResourceKey.create(Registries.DAMAGE_TYPE, animationDamage))) return;

        if (!(entity instanceof Player player) || !(entity instanceof LivingEntity livingEntity)) return;

        var anim1Attr = JujutsucraftModAttributes.ANIMATION_1.get();
        var anim2Attr = JujutsucraftModAttributes.ANIMATION_2.get();

        if (!livingEntity.getAttributes().hasAttribute(anim1Attr)) return;

        double num1 = livingEntity.getAttribute(anim1Attr).getBaseValue();
        double num2 = livingEntity.getAttributes().hasAttribute(anim2Attr) ? livingEntity.getAttribute(anim2Attr).getBaseValue() : 0.0;

        JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        double processedNum1 = num1 + ((num1 <= -50.0 && num1 >= -100.0) ? 100 : 0);
        double num3 = processedNum1 >= 0.0 ? processedNum1 % 100.0 : 100.0;
        boolean sword = LogicSwordProcedure.execute(entity);
        
        String animeName = "";
        boolean isAddonNamespace = false;
        boolean logicHandled = false;

        if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.ANIMATION_TWO.get())) {
            isAddonNamespace = true;
            logicHandled = true;
            if (addonVars.AnimationYuzuki >= 1 && addonVars.AnimationYuzuki < 20) {
                animeName = "swordnpc";
                addonVars.AnimationYuzuki += 1;
                addonVars.syncPlayerVariables(entity);
            } else if (addonVars.AnimationYuzuki >= 20) {
                addonVars.AnimationYuzuki = 0;
                addonVars.syncPlayerVariables(entity);
            }

            if (animeName.isEmpty()) {
                if (addonVars.AnimationDefense == 1) { animeName = "defensesword4"; addonVars.AnimationDefense = 0; }
                else if (addonVars.AnimationDefense == 2) { animeName = "defensesword3"; addonVars.AnimationDefense = 0; }
                else if (addonVars.AnimationDefense == 3) { animeName = "defensesword2"; addonVars.AnimationDefense = 0; }
                else if (addonVars.AnimationDefense >= 4 && addonVars.AnimationDefense < 24) { animeName = "red"; addonVars.AnimationDefense += 1; }
                else if (addonVars.AnimationDefense == 24) { addonVars.AnimationDefense = 0; }
                else if (addonVars.AnimationDefense >= 106 && addonVars.AnimationDefense < 116) { animeName = "cleaveweb"; addonVars.AnimationDefense += 1; }
                else if (addonVars.AnimationDefense == 116) { addonVars.AnimationDefense = 0; }
                else if (addonVars.AnimationDefense >= 99 && addonVars.AnimationDefense < 109) { animeName = "barragekick"; addonVars.AnimationDefense += 1; }
                else if (addonVars.AnimationDefense == 109) { addonVars.AnimationDefense = 0; }
                
                if (!animeName.isEmpty()) addonVars.syncPlayerVariables(entity);
            }
        }

        if (!logicHandled) {
            if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.ANIM_1.get())) animeName = "ab1player";
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.ANIM_2.get())) animeName = "ab2player";
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.ANIM_3.get())) animeName = "ab3player";
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.ANIM_4.get())) animeName = "ab4player";
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()) && !livingEntity.hasEffect(JujutsucraftaddonModMobEffects.SOKA_MONA.get())) animeName = "murasaki";
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.WORLD_GOJO.get())) {
                if (addonVars.OutputLevel >= 4) animeName = "murasaki2";
            }
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.DODGE.get())) animeName = "dodge" + Mth.nextInt(RandomSource.create(), 1, 21);
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.JACKPOT.get())) animeName = "tucadonca";
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.SOKA_MONA.get())) animeName = "soka";
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get())) animeName = "counterhr" + Mth.nextInt(RandomSource.create(), 1, 7);
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.ANIMATION_HEIAN.get())) animeName = "heianform";
            else if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get())) {
                if (addonVars.Moveset == 2 || addonVars.Moveset == 3) {
                    animeName = (addonVars.OutputLevel <= 3) ? "rapiddismantle" : "maximum3";
                } else if (addonVars.Moveset == 1) {
                    animeName = "spiderwebanim";
                } else if (addonVars.Moveset == 4) {
                    animeName = "worldslash" + Mth.nextInt(RandomSource.create(), 1, 4);
                }
            }

            if (!animeName.isEmpty()) {
                isAddonNamespace = true;
                logicHandled = true;
            }
        }

        if (!logicHandled) {
            ItemStack mainHand = livingEntity.getMainHandItem();
            boolean isWukongWeapon = mainHand.getItem() == JujutsucraftaddonModItems.WUKONG_STAFF_TRUE.get().asItem()
                    || mainHand.getItem() == JujutsucraftaddonModItems.HAMMER_WUKONG.get().asItem()
                    || mainHand.getItem() == JujutsucraftaddonModItems.HALBERD_WUKONG.get().asItem()
                    || mainHand.getItem() == JujutsucraftaddonModItems.SWORD_WUKONG.get().asItem()
                    || mainHand.getItem() == JujutsucraftaddonModItems.GREAT_SWORD_WUKONG.get().asItem();

            if (isWukongWeapon) {
                animeName = "wu" + Mth.nextInt(RandomSource.create(), 1, 17);
                isAddonNamespace = true;
                logicHandled = true;
            }
        }

        if (!logicHandled || !livingEntity.hasEffect(JujutsucraftaddonModMobEffects.ANIMATION.get())) {
            if (animeName.isEmpty()) {
                String side = GetEntityAnimationProcedure.execute(entity);
                if (side.contains("_right")) side = "left";
                else if (side.contains("_left")) side = "right";
                else side = Math.random() > 0.5 ? "right" : "left";

                if (num3 >= 0.0 && num3 <= 4.0) {
                    if (sword && num2 > 0.0) side = (num2 <= 3.0) ? "right" : "left";
                    if (num3 != 0.0 && num3 != 2.0 && num3 != 3.0) {
                        if (num3 == 1.0) animeName = sword ? "sword_to_right" : (Math.random() < 0.25 ? "punch_" : "kick_") + side;
                        else if (num3 == 4.0) {
                            if (num2 == 0.0 || num2 == 2.0 || num2 == 4.0) animeName = sword ? "sword_overhead" : "punch_overhead";
                            else if (num2 == 1.0 || num2 == 3.0 || num2 == 5.0) animeName = sword ? "sword_overhead2" : "punch_overhead2";
                        }
                    } else if (sword) animeName = "sword_to_" + side;
                    else animeName = (!(Math.random() < 0.25) && (num2 <= 0.0 || num2 >= 100.0) ? "kick_" : "punch_") + side;
                } else if (num3 == 20.0) animeName = (num1 == 220.0) ? "red" : "domain_expansion1";
                else if (num1 < 0.0) {
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
                            else if (baseVars.PlayerCurseTechnique != TechniqueIDs.WUKONG) animeName = "rotation";
                        } else if (num1 == -9.0) animeName = "guard";
                        else if (num1 == -10.0) animeName = "fall1";
                    } else if (num1 >= -15.0) animeName = "dance" + Math.round(num1 + 16.0);
                    else if (num1 >= -20.0) {
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
                    } else if (num1 == -49.0) animeName = "cancel";
                } else {
                    if (num1 == 107.0) animeName = "open";
                    else if (num1 == 207.0) animeName = "red";
                    else if (num1 == 215.0) animeName = (num2 == 1.0) ? "charge2" : "charge1";
                    else if (num1 == 618.0) animeName = "ten_shadows_technique_mahoraga";
                    else if (num1 == 1706.0) animeName = "kick_flying";
                    else if (num1 == 1715.0) animeName = "wifi";
                    else if (num1 == 2015.0) animeName = "plus_ultra";
                }
                isAddonNamespace = false;
            }
        }

        if (!animeName.isEmpty()) {
            String namespace = isAddonNamespace ? "jujutsucraftaddon" : "jujutsucraft";
            if (world.isClientSide() && player instanceof AbstractClientPlayer acp) {
                var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(acp).get(new ResourceLocation(namespace, "player_animation"));
                if (animation != null) {
                    animation.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation(namespace, animeName))));
                }
            } else {
                var msg = new SetupAnimationsProcedure.JujutsucraftModAnimationMessage(animeName, player.getId(), true);
                if (player instanceof ServerPlayer sp) {
                    JujutsucraftMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> sp), msg);
                }
                JujutsucraftMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), msg);
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
