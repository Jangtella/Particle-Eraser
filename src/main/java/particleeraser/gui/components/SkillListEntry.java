package particleeraser.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import particleeraser.config.ModConfig;
import particleeraser.config.SkillConfig;
import particleeraser.data.SkillInfo;
import particleeraser.gui.ConfigScreen;

import java.util.Arrays;
import java.util.List;

public class SkillListEntry extends ContainerObjectSelectionList.Entry<SkillListEntry> {
    private final Minecraft minecraft;
    private final ConfigScreen configScreen;
    private final SkillInfo skillInfo;
    private final StyledButton editButton;
    private final StyledButton resetButton;

    public SkillListEntry(Minecraft minecraft, ConfigScreen configScreen, SkillInfo skillInfo) {
        this.minecraft = minecraft;
        this.configScreen = configScreen;
        this.skillInfo = skillInfo;

        this.editButton = new StyledButton(0, 0, 44, 18, Component.literal("Edit"), b -> {
            this.configScreen.openSkillEdit(this.skillInfo);
        });

        this.resetButton = new StyledButton(0, 0, 44, 18, Component.literal("Reset"), b -> {
            SkillConfig cfg = ModConfig.INSTANCE.getOrCreateSkillConfig(this.skillInfo.getId());
            cfg.reset();
            ModConfig.INSTANCE.save();
            this.configScreen.refreshDetailedList();
        });
    }

    @Override
    public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
        Font font = this.minecraft.font;
        SkillConfig cfg = ModConfig.INSTANCE.getOrCreateSkillConfig(this.skillInfo.getId());
        boolean modified = cfg.isModified();

        int cardBg = isHovered ? 0xDD22222B : 0x88181820;
        int cardBorder = isHovered ? 0xFFFFFFFF : (modified ? 0xFF558855 : 0xFF2F2F38);

        graphics.fill(left, top, left + width, top + height - 2, cardBg);
        graphics.fill(left, top, left + width, top + 1, cardBorder);
        graphics.fill(left, top + height - 3, left + width, top + height - 2, cardBorder);
        graphics.fill(left, top, left + 1, top + height - 2, cardBorder);
        graphics.fill(left + width - 1, top, left + width, top + height - 2, cardBorder);

        int titleColor = modified ? 0xFFFFAA : 0xFFE0E0E0;
        graphics.drawString(font, this.skillInfo.getName(), left + 6, top + 4, titleColor, false);

        String badge = modified ? "[Custom]" : "[Default]";
        int badgeColor = modified ? 0xFF55FF55 : 0xFF888888;
        int badgeWidth = font.width(badge);
        graphics.drawString(font, badge, left + width - 104 - badgeWidth, top + 4, badgeColor, false);

        String detail = this.skillInfo.getId();
        if (!this.skillInfo.getParticles().isEmpty()) {
            detail += " (" + String.join(", ", this.skillInfo.getParticles()) + ")";
        }
        int maxDetailW = width - 104;
        if (font.width(detail) > maxDetailW) {
            detail = font.plainSubstrByWidth(detail, Math.max(10, maxDetailW - 8)) + "...";
        }
        graphics.drawString(font, detail, left + 6, top + 15, 0xFF888888, false);

        editButton.setX(left + width - 96);
        editButton.setY(top + 4);
        editButton.render(graphics, mouseX, mouseY, partialTick);

        resetButton.setX(left + width - 48);
        resetButton.setY(top + 4);
        resetButton.active = modified;
        resetButton.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Arrays.asList(editButton, resetButton);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Arrays.asList(editButton, resetButton);
    }

    public SkillInfo getSkillInfo() {
        return skillInfo;
    }
}
