package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.KashimoHajimeEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.procedures.MythicalBeastAmberEffectEffectStartedappliedProcedure;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MythicalBeastAmberEffectEffectStartedappliedProcedure.class, priority = -10000)
public abstract class MythicalBeastAmberStartMixin {

    /**
     * @author Satushi
     * @reason Buffs Mythical Beast Amber starting damage based on Addon Variables
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(Entity entity, double amplifier, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        if (entity instanceof Player _player) {
            ItemStack item_a = new ItemStack((ItemLike) JujutsucraftModItems.MYTHICAL_BEAST_AMBER_HELMET.get());
            item_a.getOrCreateTag().putBoolean("effect_item", true);
            item_a.getOrCreateTag().putBoolean("hand", true);

            // Set items in hands if empty
            if (_player.getMainHandItem().isEmpty()) {
                _player.setItemInHand(InteractionHand.MAIN_HAND, item_a.copy());
            } else {
                ItemHandlerHelper.giveItemToPlayer(_player, item_a.copy());
            }

            if (_player.getOffhandItem().isEmpty()) {
                _player.setItemInHand(InteractionHand.OFF_HAND, item_a.copy());
            } else {
                ItemHandlerHelper.giveItemToPlayer(_player, item_a.copy());
            }

            // Set helmet if not already wearing amber helmet
            ItemStack headArmor = _player.getItemBySlot(EquipmentSlot.HEAD);
            if (headArmor.getItem() != JujutsucraftModItems.MYTHICAL_BEAST_AMBER_HELMET.get()) {
                if (!headArmor.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(_player, headArmor.copy());
                }
                ItemStack item_b = new ItemStack((ItemLike) JujutsucraftModItems.MYTHICAL_BEAST_AMBER_HELMET.get());
                item_b.getOrCreateTag().putBoolean("effect_item", true);
                _player.setItemSlot(EquipmentSlot.HEAD, item_b);
            }
            
            _player.getInventory().setChanged();
        }

        double num_level = amplifier + 1.0;
        if (num_level > 0.0 && entity instanceof LivingEntity _liv) {
            if (_liv.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                double currentBase = _liv.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
                
                // Addon Logic: Calculate multiplier based on Subrace and Clan
                JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

                double baseValue = 1.2; // Restored to JJKUR standard
                double multiplier;
                if ("Perfect Vessel".equals(addonVars.Subrace)) {
                    multiplier = "Kashimo".equals(addonVars.Clans) ? baseValue * 1.15 : baseValue * 1.20;
                } else if ("Kashimo".equals(addonVars.Clans)) {
                    multiplier = baseValue * 1.1;
                } else {
                    multiplier = baseValue;
                }

                _liv.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(currentBase + num_level * multiplier);
            }
        }

        if (entity instanceof KashimoHajimeEntity animatable) {
            animatable.setTexture("kashimo_hajime_cursedtechnique");
        }
    }
}
