package com.jujutsu.jujutsucraftaddon.client.gui;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.network.SkillTreeSPButtonMessage;
import com.jujutsu.jujutsucraftaddon.procedures.*;
import com.jujutsu.jujutsucraftaddon.util.JJKUClientConfig;
import com.jujutsu.jujutsucraftaddon.util.JJKUVariables;
import com.jujutsu.jujutsucraftaddon.world.inventory.SkillTreeSPMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class SkillTreeSPScreen extends AbstractContainerScreen<SkillTreeSPMenu> {
    private final Level world;
    private final int x, y, z;
    private final Player entity;

    private int currentPage = 0;

    Button button_empty, button_empty1, button_empty2, button_fight_your_spirit, button_rct_mastery_quest;
    Button button_unlock_extension, button_rebirth, button_binding_vows, button_meditation;

    Checkbox checkSukunaMarks, checkMainHud, checkBackstepHud, checkZenithArms;

    Button buttonNext, buttonPrev;

    public SkillTreeSPScreen(SkillTreeSPMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 500;
        this.imageHeight = 500;
    }

    private void handleButtonClick(int id) {
        int amount = 1;
        if (id == 0 || id == 1) {
            if (Screen.hasShiftDown()) amount = 10;
            else if (Screen.hasControlDown()) amount = 100;
        }
        JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new SkillTreeSPButtonMessage(id, x, y, z, amount));
        SkillTreeSPButtonMessage.handleButtonAction(entity, id, x, y, z, amount);
    }

    private void updateWidgetVisibility() {
        boolean page0 = currentPage == 0;
        boolean page1 = currentPage == 1;

        if (button_empty != null) button_empty.visible = page0;
        if (button_empty1 != null) button_empty1.visible = page0;
        if (button_empty2 != null) button_empty2.visible = page0;
        if (button_fight_your_spirit != null) button_fight_your_spirit.visible = page0;
        if (button_rct_mastery_quest != null)
            button_rct_mastery_quest.visible = page0 && ReturnSecretProcedure.execute(entity);
        if (button_unlock_extension != null) button_unlock_extension.visible = page0;
        if (button_rebirth != null) button_rebirth.visible = page0 && ReturnRebirthProcedure.execute(world, entity);
        if (button_binding_vows != null) button_binding_vows.visible = page0;
        if (button_meditation != null) button_meditation.visible = page0;

        if (checkSukunaMarks != null) checkSukunaMarks.visible = page1;
        if (checkMainHud != null) checkMainHud.visible = page1;
        if (checkBackstepHud != null) checkBackstepHud.visible = page1;
        if (checkZenithArms != null) checkZenithArms.visible = page1;

        if (buttonNext != null) buttonNext.visible = page0;
        if (buttonPrev != null) buttonPrev.visible = page1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        updateWidgetVisibility();
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (currentPage == 0) {
            renderTooltips(guiGraphics, mouseX, mouseY);
        } else if (currentPage == 1) {
            int xPos = this.leftPos + 350;
            int yPos = this.topPos + 420;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, xPos, yPos, 100, (float) (xPos) - mouseX, (float) (yPos - 150) - mouseY, this.entity);
            guiGraphics.pose().popPose();

            guiGraphics.drawString(this.font, "Zenith Preferences", this.leftPos + 50, this.topPos + 180, -1, true);
            guiGraphics.drawString(this.font, "Preview (Appearance)", this.leftPos + 300, this.topPos + 180, -1, true);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (mouseX > leftPos + 127 && mouseX < leftPos + 151 && mouseY > topPos + 235 && mouseY < topPos + 259)
            guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_changes_your_health_attribute"), mouseX, mouseY);
        if (mouseX > leftPos + 127 && mouseX < leftPos + 151 && mouseY > topPos + 271 && mouseY < topPos + 295)
            guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_changes_your_ce_cursed_energ"), mouseX, mouseY);
        if (mouseX > leftPos + 126 && mouseX < leftPos + 150 && mouseY > topPos + 306 && mouseY < topPos + 330)
            guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_doubles_your_base_speed"), mouseX, mouseY);
        if (mouseX > leftPos + 363 && mouseX < leftPos + 387 && mouseY > topPos + 275 && mouseY < topPos + 299)
            guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_meditate_agressive_with_your_spi"), mouseX, mouseY);
        if (mouseX > leftPos + 363 && mouseX < leftPos + 387 && mouseY > topPos + 306 && mouseY < topPos + 330)
            guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_for_unlock_extension_just_click"), mouseX, mouseY);
        if (ReturnSecretProcedure.execute(entity))
            if (mouseX > leftPos + 367 && mouseX < leftPos + 391 && mouseY > topPos + 199 && mouseY < topPos + 223)
                guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_start_rct_mastery_quest"), mouseX, mouseY);
        if (mouseX > leftPos + 58 && mouseX < leftPos + 82 && mouseY > topPos + 207 && mouseY < topPos + 231)
            guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_prestige_your_character"), mouseX, mouseY);
        if (mouseX > leftPos + 232 && mouseX < leftPos + 256 && mouseY > topPos + 271 && mouseY < topPos + 295)
            guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_display_binding_vows_menu"), mouseX, mouseY);
        if (mouseX > leftPos + 232 && mouseX < leftPos + 256 && mouseY > topPos + 302 && mouseY < topPos + 326)
            guiGraphics.renderTooltip(font, Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.tooltip_start_meditationuse_again_for_s"), mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (currentPage == 0) {
            guiGraphics.blit(ResourceLocation.parse("jujutsucraftaddon:textures/screens/healer.png"), this.leftPos + 125, this.topPos + 204, 0, 0, 32, 32, 32, 32);
            guiGraphics.blit(ResourceLocation.parse("jujutsucraftaddon:textures/screens/teste3.png"), this.leftPos + 101, this.topPos + 224, 0, 0, 82, 46, 82, 46);
            guiGraphics.blit(ResourceLocation.parse("jujutsucraftaddon:textures/screens/ce.png"), this.leftPos + 101, this.topPos + 260, 0, 0, 82, 46, 82, 46);
            guiGraphics.blit(ResourceLocation.parse("jujutsucraftaddon:textures/screens/speed.png"), this.leftPos + 100, this.topPos + 295, 0, 0, 82, 46, 82, 46);
            guiGraphics.blit(ResourceLocation.parse("jujutsucraftaddon:textures/screens/sukunaspirit.png"), this.leftPos + 347, this.topPos + 217, 0, 0, 60, 60, 60, 60);
        } else if (currentPage == 1) {
            guiGraphics.fill(this.leftPos + 40, this.topPos + 170, this.leftPos + 460, this.topPos + 450, 0xAA000000);
        }

        RenderSystem.disableBlend();
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (currentPage == 0) {
            guiGraphics.drawString(this.font, ReturnSkillPointsProcedure.execute(entity), 320, 350, -16711690, false);
            guiGraphics.drawString(this.font, ReturnBurnoutExpProcedure.execute(entity), 42, 157, -4325121, false);
            if (ReturnSecret2Procedure.execute(entity))
                guiGraphics.drawString(this.font, ReturnAmountProcedure.execute(entity), 104, 351, -65536, false);
            guiGraphics.drawString(this.font, ReturnBarrierlessExpProcedure.execute(entity), 41, 180, -12139777, false);
            guiGraphics.drawString(this.font, ReturnSimpleProcedure.execute(entity), 43, 138, -3342337, false);
        }
    }

    @Override
    public void init() {
        super.init();

        button_empty = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_empty"), e -> handleButtonClick(0)).bounds(this.leftPos + 69, this.topPos + 238, 30, 20).build();
        this.addRenderableWidget(button_empty);
        button_empty1 = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_empty1"), e -> handleButtonClick(1)).bounds(this.leftPos + 69, this.topPos + 273, 30, 20).build();
        this.addRenderableWidget(button_empty1);
        button_empty2 = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_empty2"), e -> handleButtonClick(2)).bounds(this.leftPos + 69, this.topPos + 309, 30, 20).build();
        this.addRenderableWidget(button_empty2);
        button_fight_your_spirit = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_fight_your_spirit"), e -> handleButtonClick(3)).bounds(this.leftPos + 322, this.topPos + 277, 113, 20).build();
        this.addRenderableWidget(button_fight_your_spirit);

        button_rct_mastery_quest = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_rct_mastery_quest"), e -> {
            if (ReturnSecretProcedure.execute(entity)) handleButtonClick(4);
        }).bounds(this.leftPos + 325, this.topPos + 201, 113, 20).build();
        this.addRenderableWidget(button_rct_mastery_quest);

        button_unlock_extension = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_unlock_extension"), e -> handleButtonClick(5)).bounds(this.leftPos + 323, this.topPos + 307, 108, 20).build();
        this.addRenderableWidget(button_unlock_extension);

        button_rebirth = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_rebirth"), e -> {
            if (ReturnRebirthProcedure.execute(world, entity)) handleButtonClick(6);
        }).bounds(this.leftPos + 41, this.topPos + 210, 61, 20).build();
        this.addRenderableWidget(button_rebirth);

        button_binding_vows = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_binding_vows"), e -> handleButtonClick(7)).bounds(this.leftPos + 203, this.topPos + 273, 87, 20).build();
        this.addRenderableWidget(button_binding_vows);

        button_meditation = Button.builder(Component.translatable("gui.jujutsucraftaddon.skill_tree_sp.button_meditation"), e -> handleButtonClick(8)).bounds(this.leftPos + 207, this.topPos + 304, 77, 20).build();
        this.addRenderableWidget(button_meditation);

        buttonNext = Button.builder(Component.literal(">"), e -> {
            currentPage = 1;
        }).bounds(this.leftPos + 440, this.topPos + 250, 20, 20).build();
        buttonPrev = Button.builder(Component.literal("<"), e -> {
            currentPage = 0;
        }).bounds(this.leftPos + 40, this.topPos + 250, 20, 20).build();
        this.addRenderableWidget(buttonNext);
        this.addRenderableWidget(buttonPrev);
        int startX = this.width / 2 - 100;
        int startY = this.height / 2 - 40;
        checkSukunaMarks = new Checkbox(startX, startY, 20, 20, Component.literal("Show Sukuna Marks"), JJKUClientConfig.SHOW_SUKUNA_MARKS.get(), true) {
            @Override
            public void onPress() {
                super.onPress();
                JJKUClientConfig.SHOW_SUKUNA_MARKS.set(this.selected());
                JJKUClientConfig.SPEC.save();
            }
        };

        checkMainHud = new Checkbox(startX, startY + 30, 20, 20, Component.literal("Show Main HUD"), JJKUClientConfig.SHOW_HUD.get(), true) {
            @Override
            public void onPress() {
                super.onPress();
                JJKUClientConfig.SHOW_HUD.set(this.selected());
                JJKUClientConfig.SPEC.save();
            }
        };

        checkBackstepHud = new Checkbox(startX, startY + 60, 20, 20, Component.literal("Show Backstep HUD"), JJKUClientConfig.SHOW_BACKSTEP_HUD.get(), true) {
            @Override
            public void onPress() {
                super.onPress();
                JJKUClientConfig.SHOW_BACKSTEP_HUD.set(this.selected());
                JJKUClientConfig.SPEC.save();
            }
        };
        Boolean show = JJKUVariables.get(Minecraft.getInstance().player)
                .map(cap -> cap.zenith_show_arms)
                .orElse(true);
        checkZenithArms = new Checkbox(startX, startY + 90, 20, 20, Component.literal("Show Zenith Arms"), !show, true) {
            @Override
            public void onPress() {
                super.onPress();
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new SkillTreeSPButtonMessage(100, x, y, z, 0));
            }
        };

        this.addRenderableWidget(checkSukunaMarks);
        this.addRenderableWidget(checkMainHud);
        this.addRenderableWidget(checkBackstepHud);
        this.addRenderableWidget(checkZenithArms);

        updateWidgetVisibility();
    }
}