package com.jujutsu.jujutsucraftaddon.mixins;


import com.jujutsu.jujutsucraftaddon.procedures.JJKURMalevolentShrineActiveProcedure;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;
import net.mcreator.jujutsucraft.procedures.CanSeeSukunaSlashProcedure;
import net.mcreator.jujutsucraft.procedures.MalevolentShrineActiveProcedure;
import net.mcreator.jujutsucraft.procedures.RangeAttackProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MalevolentShrineActiveProcedure.class, priority = -10000)
public abstract class MalevolentShrineActiveProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void onExecute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        JJKURMalevolentShrineActiveProcedure.onExecute(world, entity, ci);
    }
}
