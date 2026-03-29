package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.procedures.RenderSkyProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderSkyProcedure.class, priority = -1000)
public class RenderSkyProcedureMixin {
    @Inject(
            method = "execute(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void execute(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null) {
            // Se a variável 'fog' do addon estiver desativada (false), cancelamos a renderização do céu do domínio
            boolean fogEnabled = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables()).fog;
            
            if (!fogEnabled) {
                cir.setReturnValue(false); // Retorna false para indicar que o céu padrão deve ser mantido
            }
        }
    }
}
