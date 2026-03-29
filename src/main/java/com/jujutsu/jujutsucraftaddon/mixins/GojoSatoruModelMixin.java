package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.entity.GojoSatoruEntity;
import net.mcreator.jujutsucraft.entity.model.GojoSatoruModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GojoSatoruModel.class, priority = -10000)
public abstract class GojoSatoruModelMixin {

    @Inject(method = "getAnimationResource", at = @At("RETURN"), cancellable = true, remap = false)
    private void getAnimationResource(GojoSatoruEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(new ResourceLocation("jujutsucraftaddon", "animations/human2.animation.json"));
    }
}
