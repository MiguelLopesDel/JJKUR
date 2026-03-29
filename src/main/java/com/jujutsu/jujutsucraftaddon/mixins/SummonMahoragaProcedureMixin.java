package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.procedures.SummonMahoragaProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = SummonMahoragaProcedure.class, priority = -10000)
public abstract class SummonMahoragaProcedureMixin {

    /**
     * @author Satushi
     * @reason Adds advancement for Sukuna progression when summoning a fully adapted Mahoraga.
     */
    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void onExecuteHead(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer _sp && _sp.server != null) {
            // Check for Bath Ritual completion
            Advancement bathAdv = _sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:bath_ritual"));
            if (bathAdv != null && _sp.getAdvancements().getOrStartProgress(bathAdv).isDone()) {
                
                ItemStack headItem = (_sp.getItemBySlot(EquipmentSlot.HEAD)).copy();
                String itemName = ForgeRegistries.ITEMS.getKey(headItem.getItem()).toString();

                // If holding/wearing the Mahoraga Wheel
                if (itemName.contains("wheel")) {
                    // Check adaptation level against Gojo (10+ ticks)
                    if (headItem.getOrCreateTag().getDouble("jujutsucraft:gojo_satoru") >= 10.0) {
                        Advancement saveMeAdv = _sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:save_me_mahoraga"));
                        if (saveMeAdv != null) {
                            AdvancementProgress progress = _sp.getAdvancements().getOrStartProgress(saveMeAdv);
                            if (!progress.isDone()) {
                                for (String criteria : progress.getRemainingCriteria()) {
                                    _sp.getAdvancements().award(saveMeAdv, criteria);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
