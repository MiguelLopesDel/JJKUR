package com.jujutsu.jujutsucraftaddon.client.renderer;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.util.JJKUClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;


public class SukunaMarksLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation MARKS_TEXTURE = ResourceLocation.fromNamespaceAndPath("jujutsucraftaddon", "textures/entities/sukunamark.png");

    public SukunaMarksLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) || player.getPersistentData().getBoolean("FlagSukuna")) {
            
            boolean isLocalPlayer = player == Minecraft.getInstance().player;
            boolean showToOthers = player.level().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_SHOW_MARKS_TO_OTHERS);

            if (isLocalPlayer) {
                if (!JJKUClientConfig.SHOW_SUKUNA_MARKS.get()) return;
            } else {
                if (!showToOthers) return;
            }

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(MARKS_TEXTURE));
            
            this.getParentModel().renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
