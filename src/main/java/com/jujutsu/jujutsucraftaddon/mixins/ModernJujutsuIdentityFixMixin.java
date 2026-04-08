package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PlayerVariables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.slyrienmodern.com.domain.DomainExpansionRenderer", remap = false)
public class ModernJujutsuIdentityFixMixin {

    @Redirect(method = "tickAndRender", at = @At(value = "FIELD", target = "Lnet/mcreator/jujutsucraft/network/JujutsucraftModVariables$PlayerVariables;PlayerCurseTechnique:D"), remap = false)
    private static double redirectTechniqueId(PlayerVariables instance) {
        return instance.SecondTechnique ? instance.PlayerCurseTechnique2 : instance.PlayerCurseTechnique;
    }
}
