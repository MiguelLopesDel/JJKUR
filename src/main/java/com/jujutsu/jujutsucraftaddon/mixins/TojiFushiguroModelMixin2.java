package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.entity.FushiguroTojiEntity;
import net.mcreator.jujutsucraft.entity.model.FushiguroTojiModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FushiguroTojiModel.class, priority = -10000)
public abstract class TojiFushiguroModelMixin2 {

    /**
     * @author Satushi
     * @reason Redirect Toji Fushiguro animations to Addon's custom animation file
     */
    @Inject(method = "getAnimationResource", at = @At("RETURN"), cancellable = true, remap = false)
    private void getAnimationResource(FushiguroTojiEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(new ResourceLocation("jujutsucraftaddon", "animations/human2.animation.json"));
    }
}
