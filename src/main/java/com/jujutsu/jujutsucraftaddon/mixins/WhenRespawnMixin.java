package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.procedures.ConcorrentSpawnProcedure;
import net.mcreator.jujutsucraft.procedures.WhenRespawnProcedure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WhenRespawnProcedure.class, priority = -10000)
public abstract class WhenRespawnMixin {

    /**
     * @author Satushi / Rigorous Restoration
     * @reason RESTORED: Bypass logic for non-mod entities. Prevents base mod logic from corrupting external entities.
     * FIXED: Added ci.cancel() back to ensure addon priority for its own entities.
     */
    @Inject(at = @At("HEAD"), method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", remap = false, cancellable = true)
    private static void execute(Event event, LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        
        // If the entity is NOT from the base mod namespace, handle addon spawning and CANCEL base logic
        if (entityTypeKey != null && !entityTypeKey.getNamespace().equals("jujutsucraft")) {
            if (entity.isAlive()) {
                if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.RESPAWNED.get(), 20, 1, false, false));
                }
                ConcorrentSpawnProcedure.execute(entity, world, x, y, z);
                
                // RESTORED: This prevents the base mod's WhenRespawn from running on non-JJK entities
                ci.cancel();
            }
        }
    }
}
