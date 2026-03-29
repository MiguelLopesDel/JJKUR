package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.PlayerTickSecondTechniqueProcedure;
import net.mcreator.jujutsucraft.procedures.ReturnInsideItemProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerTickSecondTechniqueProcedure.class, priority = -10000)
public abstract class PlayerSecondTechniqueMixin {

    /**
     * @author Satushi
     * @reason Optimized for v43 (6-tick interval) and preserves Addon's SecondAllowed logic
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity instanceof ServerPlayer _player && _player.tickCount % 6 == 0) {
            JujutsucraftModVariables.PlayerVariables baseVars = _player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables());
            JujutsucraftaddonModVariables.PlayerVariables addonVars = _player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

            boolean changeTechnique = false;
            boolean sync = false;
            ItemStack mainHand = _player.getMainHandItem();

            // 1. Loudspeaker Logic
            if (mainHand.getItem() == JujutsucraftModItems.LOUDSPEAKER.get() && !mainHand.getOrCreateTag().getBoolean("Used") 
                && !_player.getCooldowns().isOnCooldown(mainHand.getItem())) {
                
                if (!baseVars.SecondTechnique) {
                    baseVars.SecondTechnique = true;
                    sync = true;
                }
                if (baseVars.PlayerCurseTechnique2 != 3.0) {
                    baseVars.PlayerCurseTechnique2 = 3.0;
                    changeTechnique = true;
                    sync = true;
                }
            } 
            // 2. Sukuna Effect Logic
            else if (_player.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                if (baseVars.PlayerCurseTechnique2 != 1.0) {
                    baseVars.PlayerCurseTechnique2 = 1.0;
                    baseVars.SecondTechnique = true;
                    changeTechnique = true;
                    sync = true;
                }
                if (baseVars.PlayerCurseTechnique == 21.0 && !baseVars.SecondTechnique) {
                    baseVars.SecondTechnique = true;
                    sync = true;
                }
            } 
            // 3. Addon Custom Logic: SecondAllowed
            else if (addonVars.SecondAllowed) {
                if (baseVars.PlayerCurseTechnique2 != addonVars.SecondTechnique) {
                    baseVars.PlayerCurseTechnique2 = addonVars.SecondTechnique;
                    baseVars.SecondTechnique = true;
                    changeTechnique = true;
                    sync = true;
                }
            }
            // 4. Death Painting or Revert Logic
            else {
                ItemStack deathPainting = ReturnInsideItemProcedure.execute(_player).copy();
                if (deathPainting.getItem() == JujutsucraftModItems.DEATH_PAINTING.get() && deathPainting.getCount() >= 9) {
                    if (baseVars.PlayerCurseTechnique2 != 10.0) {
                        baseVars.PlayerCurseTechnique2 = 10.0;
                        sync = true;
                    }
                } else {
                    if (baseVars.SecondTechnique) {
                        baseVars.SecondTechnique = false;
                        sync = true;
                    }
                    if (baseVars.PlayerCurseTechnique2 != baseVars.PlayerCurseTechnique) {
                        baseVars.PlayerCurseTechnique2 = baseVars.PlayerCurseTechnique;
                        changeTechnique = true;
                        sync = true;
                    }
                }
            }

            // 5. Final Execution
            if (changeTechnique) {
                sync = true;
                if (!_player.level().isClientSide() && _player.getServer() != null) {
                    _player.getServer().getCommands().performPrefixedCommand(
                        new CommandSourceStack(CommandSource.NULL, _player.position(), _player.getRotationVector(), 
                        (ServerLevel)_player.level(), 4, _player.getName().getString(), _player.getDisplayName(), 
                        _player.level().getServer(), _player), "playsound ui.button.click master @s"
                    );
                }
                baseVars.noChangeTechnique = true;
                KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, _player);
            }

            if (sync) {
                baseVars.syncPlayerVariables(_player);
            }
        }
    }
}
