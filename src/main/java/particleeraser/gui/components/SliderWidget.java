package particleeraser.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SliderWidget extends AbstractSliderButton {
    private final Consumer<Double> onChange;
    private List<FormattedCharSequence> customTooltipLines = null;

    public SliderWidget(int x, int y, int width, int height, double initialValue, Consumer<Double> onChange) {
        super(x, y, width, height, Component.empty(), Math.max(0.0, Math.min(1.0, initialValue)));
        this.onChange = onChange;
        this.updateMessage();
    }

    public void setCustomTooltip(Component component) {
        if (component == null) {
            this.customTooltipLines = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String line : component.getString().split("\n")) {
            lines.addAll(mc.font.split(Component.literal(line), 350));
        }
        this.customTooltipLines = lines;
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.empty());
    }

    @Override
    protected void applyValue() {
        if (onChange != null) {
            onChange.accept(this.value);
        }
    }

    public void setSliderValue(double val) {
        this.value = Math.max(0.0, Math.min(1.0, val));
        this.updateMessage();
    }

    public double getSliderValue() {
        return this.value;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int trackH = 4;
        int trackY = y + (h - trackH) / 2;

        graphics.fill(x - 1, trackY - 1, x + w + 1, trackY + trackH + 1, 0xFF282830);
        graphics.fill(x, trackY, x + w, trackY + trackH, 0xFF141418);

        int fillW = (int) (w * this.value);
        if (fillW > 0) {
            int fillColor = !this.active ? 0xFF334455 : (isHovered ? 0xFF4A90E2 : 0xFF3568A0);
            graphics.fill(x, trackY, x + fillW, trackY + trackH, fillColor);
        }

        int thumbW = 4;
        int thumbH = trackH + 6;
        int thumbX = x + (int) ((w - thumbW) * this.value);
        int thumbY = y + (h - thumbH) / 2;

        graphics.fill(thumbX - 1, thumbY - 1, thumbX + thumbW + 1, thumbY + thumbH + 1, 0xFF282830);
        int thumbColor = !this.active ? 0xFF555560 : (isHovered ? 0xFFFFFFFF : 0xFFD0D0D0);
        graphics.fill(thumbX, thumbY, thumbX + thumbW, thumbY + thumbH, thumbColor);

        if (this.isHovered && this.customTooltipLines != null) {
            net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                screen.setTooltipForNextRenderPass(this.customTooltipLines, this.createTooltipPositioner(), isFocused());
            }
        }
    }
}
