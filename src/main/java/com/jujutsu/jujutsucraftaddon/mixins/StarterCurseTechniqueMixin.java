package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.CursedTechniqueStarterRightClickedInAirProcedure;
import net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.StartCursedTechniqueProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CursedTechniqueStarterRightClickedInAirProcedure.class, priority = -10000)
public abstract class StarterCurseTechniqueMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Restores JJKUR custom technique extraction, gamerules, and Yuta manifestation.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new JujutsucraftModVariables.PlayerVariables());
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        // 1. EXTRACTOR / MANIFESTATION LOGIC (JJKUR Feature)
        if (baseVars.PlayerCurseTechnique == 5.0 || world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_EXTRACTOR_ALLOW)) {
            if (baseVars.PlayerCurseTechnique == 5.0 && addonVars.InfusedDomain && !itemstack.getOrCreateTag().getString("TechniqueName").isEmpty()) {
                double techNum = itemstack.getOrCreateTag().getDouble("TechniqueNumber1");
                baseVars.PlayerCurseTechnique = techNum;
                baseVars.PlayerCurseTechnique2 = techNum;
                baseVars.syncPlayerVariables(entity);

                if (entity instanceof LivingEntity _liv && !world.isClientSide()) {
                    _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.MANIFESTATION.get(), 6000, 0, false, false));
                }

                if (entity instanceof Player _player) {
                    _player.getInventory().clearOrCountMatchingItems(p -> itemstack.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
                }
                return; // Extraction complete
            }
        }

        // 2. SAVE TECHNIQUE TO ITEM
        if (itemstack.getOrCreateTag().getString("TechniqueName").isEmpty()) {
            itemstack.getOrCreateTag().putString("TechniqueName", baseVars.PlayerSelectCurseTechniqueName);
            itemstack.getOrCreateTag().putDouble("TechniqueNumber1", baseVars.PlayerCurseTechnique);
            itemstack.getOrCreateTag().putDouble("TechniqueNumber2", baseVars.PlayerSelectCurseTechnique);
            itemstack.setHoverName(Component.literal(baseVars.PlayerSelectCurseTechniqueName));

            playClickSound(entity);

            if (entity instanceof Player _player && !world.isClientSide()) {
                _player.displayClientMessage(Component.literal(itemstack.getDisplayName().getString()), true);
                _player.getCooldowns().addCooldown(itemstack.getItem(), 5);
            }

            baseVars.noChangeTechnique = true;
            baseVars.syncPlayerVariables(entity);
            KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
        } 
        // 3. USE SAVED TECHNIQUE
        else if (itemstack.getOrCreateTag().getDouble("TechniqueNumber1") == baseVars.PlayerCurseTechnique || 
                 itemstack.getOrCreateTag().getDouble("TechniqueNumber1") == baseVars.PlayerCurseTechnique2) {
            
            if (entity.getPersistentData().getDouble("skill") == 0.0) {
                if (entity instanceof Player _player) {
                    _player.getCooldowns().addCooldown(itemstack.getItem(), 1);
                }

                playClickSound(entity);

                // Store current state
                boolean oldSecond = baseVars.SecondTechnique;
                double oldTechnique = baseVars.PlayerCurseTechnique;
                double oldSelect = baseVars.PlayerSelectCurseTechnique;

                // Temporarily set technique from item
                baseVars.SecondTechnique = (itemstack.getOrCreateTag().getDouble("TechniqueNumber1") != baseVars.PlayerCurseTechnique);
                baseVars.PlayerCurseTechnique = itemstack.getOrCreateTag().getDouble("TechniqueNumber1");
                baseVars.PlayerSelectCurseTechnique = itemstack.getOrCreateTag().getDouble("TechniqueNumber2");
                baseVars.syncPlayerVariables(entity);

                // Execute
                StartCursedTechniqueProcedure.execute(world, x, y, z, entity);

                // Restore state
                baseVars.SecondTechnique = oldSecond;
                baseVars.PlayerCurseTechnique = oldTechnique;
                baseVars.PlayerSelectCurseTechnique = oldSelect;
                baseVars.noChangeTechnique = true;
                baseVars.syncPlayerVariables(entity);

                KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
                entity.getPersistentData().putBoolean("PRESS_Z", true);

                if (entity instanceof Player _player && !world.isClientSide()) {
                    _player.displayClientMessage(Component.literal(itemstack.getDisplayName().getString()), true);
                    _player.getCooldowns().addCooldown(itemstack.getItem(), 5);
                }
            } else {
                entity.getPersistentData().putBoolean("PRESS_Z", false);
            }
        }
    }

    private static void playClickSound(Entity entity) {
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            entity.getServer().getCommands().performPrefixedCommand(
                new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), 
                (ServerLevel) entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), 
                entity.level().getServer(), entity), "playsound ui.button.click master @s"
            );
        }
    }
}
