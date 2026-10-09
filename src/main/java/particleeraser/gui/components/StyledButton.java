package particleeraser.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class StyledButton extends Button {
    private boolean selected = false;
    private int accentColor = 0;
    private ResourceLocation icon = null;
    private int iconTextureWidth = 36;
    private int iconTextureHeight = 36;
    private List<FormattedCharSequence> customTooltipLines = null;

    public StyledButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public void setCustomTooltip(Component component) {
        if (component == null) {
            this.customTooltipLines = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
        for (String line : component.getString().split("\n")) {
            lines.addAll(mc.font.split(Component.literal(line), 350));
        }
        this.customTooltipLines = lines;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
    }

    public void setIcon(ResourceLocation icon) {
        setIcon(icon, 36, 36);
    }

    public void setIcon(ResourceLocation icon, int texWidth, int texHeight) {
        this.icon = icon;
        this.iconTextureWidth = texWidth;
        this.iconTextureHeight = texHeight;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int bg;
        int border;
        int textColor;

        if (this.selected) {
            bg = 0xFF353545;
            border = 0xFFE5B036;
            textColor = 0xFFFFDD66;
        } else if (!this.active) {
            bg = 0x66181820;
            border = 0xFF444450;
            textColor = 0xFF666666;
        } else if (isHovered) {
            bg = 0xDD2A2A35;
            border = 0xFFFFFFFF;
            textColor = (accentColor != 0) ? accentColor : 0xFFFFFFFF;
        } else {
            bg = 0xAA181820;
            border = (accentColor != 0) ? accentColor : 0xFF353540;
            textColor = (accentColor != 0) ? accentColor : 0xFFCCCCCC;
        }

        graphics.fill(x, y, x + w, y + h, bg);
        graphics.fill(x, y, x + w, y + 1, border);
        graphics.fill(x, y + h - 1, x + w, y + h, border);
        graphics.fill(x, y, x + 1, y + h, border);
        graphics.fill(x + w - 1, y, x + w, y + h, border);

        if (this.icon != null) {
            try {
                RenderSystem.enableBlend();
                graphics.blit(this.icon, x + (w - 14) / 2, y + (h - 14) / 2, 14, 14, 0.0f, 0.0f, iconTextureWidth, iconTextureHeight, iconTextureWidth, iconTextureHeight);
            } catch (Throwable ignored) {
            }
        } else if (!getMessage().getString().isEmpty()) {
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
        }

        if (this.isHovered && this.customTooltipLines != null) {
            net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                screen.setTooltipForNextRenderPass(this.customTooltipLines, this.createTooltipPositioner(), isFocused());
            }
        }
    }
}
