package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.CustomCursedTechniqueChangerRightclickedProcedureProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SelectJinWooProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SelectWukongProcedure;
import net.mcreator.jujutsucraft.JujutsucraftMod;
import net.mcreator.jujutsucraft.client.gui.SelectTechniqueScreen;
import net.mcreator.jujutsucraft.network.SelectTechniqueButtonMessage;
import net.mcreator.jujutsucraft.world.inventory.SelectTechniqueMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(value = SelectTechniqueScreen.class, priority = -10000)
public abstract class SelectTechniqueScreenMixin extends AbstractContainerScreen<SelectTechniqueMenu> {

    @Unique
    private Button second_technique;
    @Unique
    private Button sun_wukong;
    @Unique
    private Button sung_jin_woo;

    public SelectTechniqueScreenMixin(SelectTechniqueMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
    }

    /**
     * @author Satushi
     * @reason Re-positioned and updated custom buttons for v43 layout parity.
     */
    @Inject(method = "init", at = @At("TAIL"))
    public void onInit(CallbackInfo ci) {
        TechniqueGuiAccessorMixin accessor = (TechniqueGuiAccessorMixin) this;
        Level world = accessor.getWorld();
        int x = accessor.getX();
        int y = accessor.getY();
        int z = accessor.getZ();
        Player entity = accessor.getEntity();
        HashMap<String, Object> guistate = accessor.getGuistate();
        HashMap<String, String> emptyState = new HashMap<>(); // v43 uses empty HashMap for selection packets

        // SECOND TECHNIQUE BUTTON (Positioned to avoid Reggie/Hazenoki)
        this.second_technique = Button.builder(Component.literal("Second Technique"), (e) -> {
            JujutsucraftMod.PACKET_HANDLER.sendToServer(new SelectTechniqueButtonMessage(1000, x, y, z, emptyState));
            CustomCursedTechniqueChangerRightclickedProcedureProcedure.execute(world, x, y, z, entity);
        }).bounds(this.leftPos + 335, this.topPos + 178, 77, 20).build();
        guistate.put("button:second_technique", this.second_technique);
        this.addRenderableWidget(this.second_technique);

        // SUN WUKONG BUTTON (Positioned to avoid Ranta/Empty)
        this.sun_wukong = Button.builder(Component.literal("Sun Wukong"), (e) -> {
            JujutsucraftMod.PACKET_HANDLER.sendToServer(new SelectTechniqueButtonMessage(100, x, y, z, emptyState));
            SelectWukongProcedure.execute(world, x, y, z, entity);
        }).bounds(this.leftPos + 335, this.topPos + 223, 77, 20).build();
        guistate.put("button:sun_wukong", this.sun_wukong);
        this.addRenderableWidget(this.sun_wukong);

        // SUNG JIN WOO BUTTON (Optional/Hidden in original but kept for structure)
        this.sung_jin_woo = Button.builder(Component.literal("Shadow Monarch"), (e) -> {
            JujutsucraftMod.PACKET_HANDLER.sendToServer(new SelectTechniqueButtonMessage(101, x, y, z, emptyState));
            SelectJinWooProcedure.execute(world, x, y, z, entity);
        }).bounds(this.leftPos + 11, this.topPos + 223, 77, 20).build();
        guistate.put("button:sung_jin_woo", this.sung_jin_woo);
        // this.addRenderableWidget(this.sung_jin_woo); // Kept commented as per original Addon logic
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (this.second_technique != null && this.second_technique.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Usage: Stores The Technique You Are Using.\nEnable via: /jjkuSecondTechnique @s true\nDisable first to store a new one."), mouseX, mouseY);
        }
        if (this.sun_wukong != null && this.sun_wukong.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("The Victorious Fighting Buddha (斗战胜佛)"), mouseX, mouseY);
        }
    }
}
