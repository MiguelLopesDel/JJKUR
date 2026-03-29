package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.procedures.RenderFogProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderFogProcedure.class, priority = -1000)
public class RenderFogProcedureMixin {
    @Inject(
            method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void execute(Event event, Entity entity, CallbackInfo ci) {
        if (entity != null) {
            // Se a variável 'fog' do addon estiver desativada (false), cancelamos a alteração da névoa do domínio
            boolean fogEnabled = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables()).fog;
            
            if (!fogEnabled) {
                ci.cancel(); // Cancela a execução para não mudar a distância da névoa
            }
        }
    }
}
