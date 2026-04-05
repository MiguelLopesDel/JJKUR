package com.jujutsu.jujutsucraftaddon.client.screens;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModKeyMappings;
import com.jujutsu.jujutsucraftaddon.network.AltarMessageThree;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ChosoScreen extends Screen {
    private static final int RADIUS_IN = 50;
    private static final int RADIUS_OUT = RADIUS_IN * 2;
    private static final int MAX_ITEMS = 24;
    private int hover;
    private static int page;
    private final List<List<AltarOption>> pages = new ArrayList<>();
    private int hovered = -1;
    private boolean selected = false;

    public ChosoScreen() {
        super(Component.literal("Skill List"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.pages.clear();
        List<AltarOption> options = this.getOptions();
        int count = options.size() / MAX_ITEMS;
        for (int i = 0; i < count; i++) {
            this.pages.add(options.subList(i * MAX_ITEMS, (i + 1) * MAX_ITEMS));
        }
        int remainder = options.size() % MAX_ITEMS;
        if (remainder > 0) {
            this.pages.add(options.subList(count * MAX_ITEMS, count * MAX_ITEMS + remainder));
        }
        if (page >= this.pages.size()) {
            page = 0;
        }
        if (this.pages.isEmpty()) {
            this.onClose();
        }
    }

    private List<AltarOption> getOptions() {
        List<AltarOption> options = new ArrayList<>();
        options.add(new AltarOption(Component.translatable("jujutsu.technique.choso1").getString()));
        options.add(new AltarOption(Component.translatable("jujutsu.technique.choso3").getString()));
        options.add(new AltarOption("Blood Poison"));
        options.add(new AltarOption(Component.translatable("jujutsu.technique.choso5").getString() + " Stack"));
        options.add(new AltarOption(Component.translatable("gui.jujutsucraft.select_technique.button_itadori1").getString()));
        options.add(new AltarOption("Blood Recovery"));
        options.add(new AltarOption(Component.translatable("jujutsu.technique.choso2").getString() + " Senketsu"));
        options.add(new AltarOption("Convergence"));
        options.add(new AltarOption(Component.translatable("jujutsu.technique.choso4").getString()));
        return options;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == JujutsucraftaddonModKeyMappings.ALTAR_SELECTOR.getKey().getValue()) {
            this.onClose();
        }
        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.pose().pushPose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        List<? extends AltarOption> currentOptions = this.getCurrent();
        for (int i = 0; i < currentOptions.size(); i++) {
            float startAngle = this.getAngleFor(i - 0.5F);
            float endAngle = this.getAngleFor(i + 0.5F);
            int color = this.hovered == i ? toRGB24(255, 255, 255, 150) : toRGB24(255, 0, 0, 150);
            this.drawTexturedCircle(guiGraphics.pose(), buffer, centerX, centerY, startAngle, endAngle, color);
        }
        tesselator.end();
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
        renderText(guiGraphics, centerX, centerY);
    }

    private void drawTexturedCircle(PoseStack poseStack, BufferBuilder buffer, float centerX, float centerY, float startAngle, float endAngle, int color) {
        float angle = endAngle - startAngle;
        float precision = 2.5F / 360.0F;
        int sections = Math.max(1, Mth.ceil(angle / precision));
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        float slice = angle / sections;
        Matrix4f matrix4f = poseStack.last().pose();
        for (int i = 0; i < sections; i++) {
            float angle1 = startAngle + i * slice;
            float angle2 = startAngle + (i + 1) * slice;
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);
            buffer.vertex(matrix4f, centerX + RADIUS_OUT * cos1, centerY + RADIUS_OUT * sin1, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix4f, centerX + RADIUS_IN * cos1, centerY + RADIUS_IN * sin1, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix4f, centerX + RADIUS_IN * cos2, centerY + RADIUS_IN * sin2, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix4f, centerX + RADIUS_OUT * cos2, centerY + RADIUS_OUT * sin2, 0.0F).color(r, g, b, a).endVertex();
        }
    }

    private static int toRGB24(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderText(@NotNull GuiGraphics guiGraphics, int centerX, int centerY) {
        float radius = (RADIUS_IN + RADIUS_OUT) / 2.0F;
        List<? extends AltarOption> currentOptions = this.getCurrent();
        for (int i = 0; i < currentOptions.size(); i++) {
            float angle = getAngleFor(i);
            int x = (int) (centerX + radius * Math.cos(angle));
            int y = (int) (centerY + radius * Math.sin(angle));
            int color = (this.hovered == i) ? 0xFFFFFF : 0x888888;
            guiGraphics.drawCenteredString(this.font, currentOptions.get(i).name, x, y, color);
        }
    }

    private List<? extends AltarOption> getCurrent() {
        return this.pages.get(page);
    }

    private float getAngleFor(float index) {
        return (index / this.getCurrent().size()) * Mth.TWO_PI - (Mth.PI / 2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.hovered >= 0 && !this.selected) {
            this.selected = true;
            JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new AltarMessageThree(this.hovered));
            this.getCurrent().get(this.hovered).onSelect();
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        super.mouseMoved(pMouseX, pMouseY);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        double mouseXRel = pMouseX - centerX;
        double mouseYRel = pMouseY - centerY;
        double mouseAngle = Math.atan2(mouseYRel, mouseXRel);
        double mousePosSq = mouseXRel * mouseXRel + mouseYRel * mouseYRel;
        List<? extends AltarOption> currentOptions = this.getCurrent();
        if (!currentOptions.isEmpty()) {
            float startAngle = this.getAngleFor(-0.5F);
            float endAngle = this.getAngleFor(currentOptions.size() - 0.5F);
            while (mouseAngle < startAngle) mouseAngle += Mth.TWO_PI;
            while (mouseAngle >= endAngle) mouseAngle -= Mth.TWO_PI;
            this.hovered = -1;
            if (mousePosSq >= RADIUS_IN * RADIUS_IN && mousePosSq < RADIUS_OUT * RADIUS_OUT) {
                for (int i = 0; i < currentOptions.size(); i++) {
                    float currentStart = this.getAngleFor(i - 0.5F);
                    float currentEnd = this.getAngleFor(i + 0.5F);
                    if (mouseAngle >= currentStart && mouseAngle < currentEnd) {
                        this.hovered = i;
                        break;
                    }
                }
            }
            if (mousePosSq < RADIUS_OUT * RADIUS_OUT) return;
            if (this.pages.size() > 1) {
                if (this.pages.size() - 1 > page) {
                    if (pMouseX > (double) this.width / 2 && pMouseX < this.width && pMouseY > 0 && pMouseY < this.height) {
                        if (++this.hover == 20) page++;
                        if (this.hover == 60) this.hover = 0;
                        return;
                    }
                }
                if (page > 0) {
                    if (pMouseX > 0 && pMouseX < (double) this.width / 2 && pMouseY > 0 && pMouseY < this.height) {
                        if (++this.hover == 20) page--;
                        if (this.hover == 60) this.hover = 0;
                        return;
                    }
                }
                if (this.hover > 0) this.hover = 0;
            }
        }
    }

    private static class AltarOption {
        final String name;
        AltarOption(String name) { this.name = name; }
        void onSelect() {}
    }
}
