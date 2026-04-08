package com.jujutsu.jujutsucraftaddon.client.renderer;

import com.jujutsu.jujutsucraftaddon.util.JJKUClientConfig;
import com.jujutsu.jujutsucraftaddon.util.ZenithPerfectBodyManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class ZenithFourArmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public ZenithFourArmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (ZenithPerfectBodyManager.hasAllRequirements(player)) {
            if (player == Minecraft.getInstance().player) {
                if (!JJKUClientConfig.SHOW_ZENITH_ARMS.get()) return;
            }
            renderAbsoluteZenithArms(poseStack, buffer, packedLight, player);
        }
    }

    private void renderAbsoluteZenithArms(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player) {
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        ResourceLocation skin = player.getSkinTextureLocation();

        float lxr = model.leftArm.xRot, lyr = model.leftArm.yRot, lzr = model.leftArm.zRot;
        float rxr = model.rightArm.xRot, ryr = model.rightArm.yRot, rzr = model.rightArm.zRot;
        float slxr = model.leftSleeve.xRot, slyr = model.leftSleeve.yRot, slzr = model.leftSleeve.zRot;
        float srxr = model.rightSleeve.xRot, sryr = model.rightSleeve.yRot, srzr = model.rightSleeve.zRot;

        model.leftArm.xRot = (lxr * 0.5F);
        model.leftArm.yRot = 0.0F;
        model.leftArm.zRot = -0.6F;

        model.rightArm.xRot = (rxr * 0.5F);
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.6F;

        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);

        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, 0.12D);

        VertexConsumer armConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        model.leftArm.render(poseStack, armConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        model.rightArm.render(poseStack, armConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        VertexConsumer sleeveConsumer = buffer.getBuffer(RenderType.entityTranslucent(skin));
        model.leftSleeve.render(poseStack, sleeveConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        model.rightSleeve.render(poseStack, sleeveConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        model.leftArm.setRotation(lxr, lyr, lzr);
        model.rightArm.setRotation(rxr, ryr, rzr);
        model.leftSleeve.setRotation(slxr, slyr, slzr);
        model.rightSleeve.setRotation(srxr, sryr, srzr);
    }
}
