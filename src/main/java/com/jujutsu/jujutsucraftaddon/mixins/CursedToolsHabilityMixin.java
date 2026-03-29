package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.IgrisEntity;
import com.jujutsu.jujutsucraftaddon.entity.Shadow1Entity;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.CursedSpiritGrade010Entity;
import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.CursedToolsAbilityProcedure;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CursedToolsAbilityProcedure.class, priority = -10000)
public abstract class CursedToolsHabilityMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(Entity entity, Entity entityiterator, CallbackInfo ci) {
        ci.cancel();

        if (entity == null || entityiterator == null) return;
        if (!(entityiterator instanceof LivingEntity target)) return;

        JujutsucraftModVariables.PlayerVariables pVars = null;
        if (entity instanceof Player player) {
            pVars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
        }

        // --- Lógica de Arma Amaldiçoada na Mão Principal ---
        if (entity.getPersistentData().getBoolean("attack")) {
            ItemStack mainHandItem = (entity instanceof LivingEntity liv ? liv.getMainHandItem() : ItemStack.EMPTY).copy();
            
            if (!(entity instanceof Player player && player.getCooldowns().isOnCooldown(mainHandItem.getItem()))
                && mainHandItem.is(ItemTags.create(new ResourceLocation("forge:cursed_tool")))
                && (mainHandItem.getItem() == JujutsucraftModItems.INVERTED_SPEAR_OF_HEAVEN.get() || mainHandItem.getItem() == JujutsucraftModItems.BLACK_ROPE.get())) {
                
                if (!target.level().isClientSide()) {
                    target.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CANCEL_CURSED_TECHNIQUE.get(), 1, 0));
                    target.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get(), 10, 0));
                }
                target.removeEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get());

                if (target.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) && !target.level().isClientSide()) {
                    int amplifier = target.hasEffect(MobEffects.DAMAGE_BOOST) ? target.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, amplifier, false, false));
                }
            }
        }

        // --- Lógica de Adaptação do Mahoraga (Capacete/Roda) ---
        ItemStack helmet = (entity instanceof LivingEntity liv ? liv.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY).copy();
        
        if (!(entity instanceof Player player && player.getCooldowns().isOnCooldown(helmet.getItem()))
            && (helmet.getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get() || helmet.getItem() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get())) {
            
            boolean mahorage = false;
            if (entity instanceof Player player) {
                JujutsucraftaddonModVariables.PlayerVariables aVars = player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());
                mahorage = (pVars != null && (pVars.PlayerCurseTechnique == 16.0 || pVars.PlayerCurseTechnique2 == 16.0))
                           || entity.getPersistentData().getDouble("IsMahoraga") == 1.0
                           || aVars.Mahoraga == 1;
            } else {
                mahorage = entity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity 
                           || entity instanceof CursedSpiritGrade010Entity
                           || entity instanceof IgrisEntity 
                           || entity instanceof Shadow1Entity;
            }

            double NUM1 = 0.0;
            String STR1 = "";

            // Adaptação ao Infinito
            if (target.hasEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get()) && entity.getPersistentData().getBoolean("attack")) {
                STR1 = "skill205";
                if (helmet.getOrCreateTag().getDouble(STR1) == 0.0) {
                    NUM1 = 1.0;
                } else if (helmet.getOrCreateTag().getDouble(STR1) >= 100.0 && mahorage) {
                    STR1 = "";
                    if (!target.level().isClientSide()) {
                        target.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get(), 1, 1));
                    }
                    target.removeEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get());
                }
            }

            // Adaptação a Seres Vivos (Boost de CursePower)
            if (NUM1 == 0.0 && STR1.equals("") && mahorage) {
                STR1 = "toLiving";
                if (helmet.getOrCreateTag().getDouble(STR1) == 0.0) {
                    NUM1 = 1.0;
                } else if (helmet.getOrCreateTag().getDouble(STR1) >= 100.0) {
                    double boost = (double) Math.round(Math.floor(helmet.getOrCreateTag().getDouble(STR1) / 100.0) * 2.5);
                    ItemStack mainHand = (entity instanceof LivingEntity liv ? liv.getMainHandItem() : ItemStack.EMPTY);
                    if (mainHand.isEnchantable()) {
                        double currentCP = mainHand.getOrCreateTag().getDouble("CursePower");
                        double newCP;
                        if (target.getPersistentData().getBoolean("CursedSpirit") && mainHand.getItem() == JujutsucraftModItems.SWORD_OF_EXTERMINATION.get()) {
                            newCP = Math.min(boost * -1.0, currentCP);
                        } else {
                            newCP = Math.max(boost, currentCP);
                        }
                        mainHand.getOrCreateTag().putDouble("CursePower", newCP);
                    }
                    NUM1 = 0.0;
                }
            }

            // Início da Adaptação (Se NUM1 > 0)
            if (NUM1 > 0.0) {
                for (int i = 0; i < 800; i++) {
                    String dataKey = "DATA" + Math.round(NUM1);
                    if (helmet.getOrCreateTag().getString(dataKey).equals("") || helmet.getOrCreateTag().getString(dataKey).equals(STR1)) {
                        ItemStack actualHelmet = (entity instanceof LivingEntity liv ? liv.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY);
                        actualHelmet.getOrCreateTag().putString(dataKey, STR1);
                        actualHelmet.getOrCreateTag().putDouble(STR1, 1.0);
                        
                        if (entity instanceof Player player && !player.level().isClientSide()) {
                            player.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.adaptation_start").getString()), false);
                        }
                        break;
                    }
                    NUM1++;
                }
            }
        }
    }
}
