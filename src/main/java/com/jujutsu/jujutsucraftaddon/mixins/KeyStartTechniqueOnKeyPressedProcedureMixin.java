package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.Animations2Procedure;
import com.jujutsu.jujutsucraftaddon.procedures.HRAttack1Procedure;
import com.jujutsu.jujutsucraftaddon.procedures.NueSummonProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SpawnCloneProcedure;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyStartTechniqueOnKeyPressedProcedure.class, priority = -10000)
public abstract class KeyStartTechniqueOnKeyPressedProcedureMixin {

    /**
     * @author Satushi
     * @reason Adds some logics to the KeyStartTechniqueOnKeyPressedProcedure for display animations and new attacks
     */
    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        // 1. Nue Summon Logic
        NueSummonProcedure.execute(world, x, y, z, entity);

        // 2. Output & Technique Logic
        entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(baseVars -> {
                
                // Boost cnt6 based on Output variable
                if (addonVars.Output > 0) {
                    double currentCnt6 = entity.getPersistentData().getDouble("cnt6");
                    if (currentCnt6 <= addonVars.Output) {
                        entity.getPersistentData().putDouble("cnt6", currentCnt6 + addonVars.Output);
                    }
                }

                // HR Attack Logic
                if (baseVars.PlayerCurseTechnique2 == -1) {
                    HRAttack1Procedure.execute(world, x, y, z, entity);
                }

                // Clone Spawning Logic
                if (baseVars.PlayerCursePower > 1500 && baseVars.PlayerSelectCurseTechniqueName.contains("Clone")) {
                    if (entity instanceof LivingEntity _liv && !_liv.hasEffect(JujutsucraftaddonModMobEffects.CLONE_TICKED.get()) && !entity.isShiftKeyDown()) {
                        SpawnCloneProcedure.execute(world, x, y, z, entity);
                        
                        boolean isWukong = "Wukong".equals(addonVars.Clans);
                        if (!isWukong || Math.random() < (1.0 / 3.0)) {
                            if (!_liv.level().isClientSide()) {
                                _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.CLONE_TICKED.get(), -1, 1, false, false));
                            }
                        }

                        if (entity instanceof Player _player) {
                            baseVars.PlayerCursePower -= 1500.0;
                            baseVars.syncPlayerVariables(_player);
                            if (!_player.level().isClientSide()) {
                                _player.displayClientMessage(Component.literal("Clone Spawned"), true);
                            }
                        }
                    }
                }

                // Custom Animations Logic (Sukuna/Gojo)
                if (baseVars.PlayerCurseTechnique2 == 1 || baseVars.PlayerCurseTechnique2 == 2) {
                    Animations2Procedure.execute(world, x, y, z, entity);
                }
            });
        });
    }
}
