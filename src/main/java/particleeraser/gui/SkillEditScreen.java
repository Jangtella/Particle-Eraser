package particleeraser.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import particleeraser.config.ModConfig;
import particleeraser.config.SkillConfig;
import particleeraser.data.SkillInfo;
import particleeraser.gui.components.NumberEditBox;
import particleeraser.gui.components.ParticlePreviewWidget;
import particleeraser.gui.components.SliderWidget;
import particleeraser.gui.components.StyledButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkillEditScreen extends Screen {
    private static float savedSpeed = 1.0f;

    private final Screen parentScreen;
    private final SkillInfo skillInfo;
    private final SkillConfig skillConfig;

    private ParticlePreviewWidget previewWidget;
    private StyledButton toggleButton;
    private SliderWidget alphaSlider;
    private NumberEditBox alphaBox;
    private SliderWidget cullingSlider;
    private NumberEditBox cullingBox;
    private SliderWidget speedSlider;
    private StyledButton resetButton;
    private StyledButton doneButton;
    private StyledButton closeButton;
    private final List<StyledButton> particleButtons = new ArrayList<>();

    private String selectedParticle = "[ALL]";
    private int startYOffset = 0;

    public SkillEditScreen(Screen parentScreen, SkillInfo skillInfo) {
        super(Component.literal("Edit Skill: " + skillInfo.getName()));
        this.parentScreen = parentScreen;
        this.skillInfo = skillInfo;
        this.skillConfig = ModConfig.INSTANCE.getOrCreateSkillConfig(skillInfo.getId());
    }

    @Override
    protected void init() {
        int dialogW = Math.min(this.width - 20, 560);
        int dialogH = Math.min(this.height - 20, 310);
        int startX = (this.width - dialogW) / 2;
        int startY = (this.height - dialogH) / 2;

        closeButton = new StyledButton(startX + dialogW - 24, startY + 5, 18, 18, Component.literal("X"), b -> this.onClose());
        this.addRenderableWidget(closeButton);

        int panelX = startX + 12;
        int panelY = startY + 27;
        int panelW = dialogW - 24;
        int panelH = dialogH - 35;

        previewWidget = new ParticlePreviewWidget(
                panelX + 14, panelY + 14, 230, 220,
                this::isCurrentEnabled,
                this::getCurrentAlpha,
                this::getCurrentCulling
        );
        previewWidget.setSchool(skillInfo.getSchool());
        previewWidget.setSkillInfo(skillInfo);
        previewWidget.setPlaybackSpeed(savedSpeed);
        updatePreviewWidgetParticles();
        this.addRenderableWidget(previewWidget);

        int controlsX = panelX + 258;

        particleButtons.clear();
        int offset = 0;
        List<String> particles = skillInfo.getParticles();

        if (particles.size() > 1) {
            int btnX = controlsX;
            int btnY = panelY + 14;
            int maxRight = panelX + panelW - 14;

            StyledButton allBtn = new StyledButton(btnX, btnY, 32, 16, Component.literal("ALL"), b -> {
                this.selectedParticle = "[ALL]";
                updateParticleTabButtons();
                updateWidgets();
            });
            allBtn.setTooltip(Tooltip.create(Component.literal("All Particles")));
            particleButtons.add(allBtn);
            this.addRenderableWidget(allBtn);
            btnX += 34;

            for (String p : particles) {
                String shortName = p.contains(":") ? p.substring(p.indexOf(':') + 1) : p;
                int nameW = Math.min(120, Math.max(30, this.font.width(shortName) + 10));

                if (btnX + nameW > maxRight) {
                    btnX = controlsX;
                    btnY += 18;
                }

                StyledButton pBtn = new StyledButton(btnX, btnY, nameW, 16, Component.literal(shortName), b -> {
                    this.selectedParticle = p;
                    updateParticleTabButtons();
                    updateWidgets();
                });
                pBtn.setTooltip(Tooltip.create(Component.literal(p)));
                particleButtons.add(pBtn);
                this.addRenderableWidget(pBtn);
                btnX += nameW + 2;
            }
            offset = (btnY - (panelY + 14)) + 22;
            updateParticleTabButtons();
        } else if (!particles.isEmpty()) {
            this.selectedParticle = particles.get(0);
        }

        this.startYOffset = offset;
        int controlsY = panelY + 14 + this.startYOffset;

        toggleButton = new StyledButton(controlsX + 115, controlsY, 36, 16, Component.empty(), b -> {
            boolean next = !isCurrentEnabled();
            if ("[ALL]".equals(selectedParticle)) {
                skillConfig.setEnabled(next);
                for (String p : skillInfo.getParticles()) {
                    skillConfig.getOrCreateParticleSetting(p).setEnabled(next);
                }
            } else {
                skillConfig.getOrCreateParticleSetting(selectedParticle).setEnabled(next);
            }
            updateToggleState();
            ModConfig.INSTANCE.save();
        });
        updateToggleState();
        this.addRenderableWidget(toggleButton);

        alphaSlider = new SliderWidget(controlsX + 60, controlsY + 24, 150, 16, getCurrentAlpha(), val -> {
            float fVal = val.floatValue();
            if ("[ALL]".equals(selectedParticle)) {
                skillConfig.setAlpha(fVal);
                for (String p : skillInfo.getParticles()) {
                    skillConfig.getOrCreateParticleSetting(p).setAlpha(fVal);
                }
            } else {
                skillConfig.getOrCreateParticleSetting(selectedParticle).setAlpha(fVal);
            }
            if (alphaBox != null) {
                alphaBox.setNumericValue(fVal);
            }
            ModConfig.INSTANCE.save();
        });
        alphaSlider.setCustomTooltip(Component.literal("Opacity\n(0.00 = invisible, 1.00 = default)"));
        this.addRenderableWidget(alphaSlider);

        alphaBox = new NumberEditBox(this.font, controlsX + 216, controlsY + 24, 44, 16, getCurrentAlpha(), val -> {
            if ("[ALL]".equals(selectedParticle)) {
                skillConfig.setAlpha(val);
                for (String p : skillInfo.getParticles()) {
                    skillConfig.getOrCreateParticleSetting(p).setAlpha(val);
                }
            } else {
                skillConfig.getOrCreateParticleSetting(selectedParticle).setAlpha(val);
            }
            if (alphaSlider != null) {
                alphaSlider.setSliderValue(val);
            }
            ModConfig.INSTANCE.save();
        });
        this.addRenderableWidget(alphaBox);

        cullingSlider = new SliderWidget(controlsX + 60, controlsY + 48, 150, 16, getCurrentCulling(), val -> {
            float fVal = val.floatValue();
            if ("[ALL]".equals(selectedParticle)) {
                skillConfig.setCulling(fVal);
                for (String p : skillInfo.getParticles()) {
                    skillConfig.getOrCreateParticleSetting(p).setCulling(fVal);
                }
            } else {
                skillConfig.getOrCreateParticleSetting(selectedParticle).setCulling(fVal);
            }
            if (cullingBox != null) {
                cullingBox.setNumericValue(fVal);
            }
            ModConfig.INSTANCE.save();
        });
        cullingSlider.setCustomTooltip(Component.literal("Culling Rate\n(0.00 = cull all, 1.00 = show all)"));
        this.addRenderableWidget(cullingSlider);

        cullingBox = new NumberEditBox(this.font, controlsX + 216, controlsY + 48, 44, 16, getCurrentCulling(), val -> {
            if ("[ALL]".equals(selectedParticle)) {
                skillConfig.setCulling(val);
                for (String p : skillInfo.getParticles()) {
                    skillConfig.getOrCreateParticleSetting(p).setCulling(val);
                }
            } else {
                skillConfig.getOrCreateParticleSetting(selectedParticle).setCulling(val);
            }
            if (cullingSlider != null) {
                cullingSlider.setSliderValue(val);
            }
            ModConfig.INSTANCE.save();
        });
        this.addRenderableWidget(cullingBox);

        int speedX = panelX + 54;
        int speedW = (panelX + 14 + 230) - speedX;
        double initialSliderVal = Math.max(0.0, Math.min(1.0, (savedSpeed - 0.1) / 0.9));
        speedSlider = new SliderWidget(speedX, panelY + panelH - 25, speedW, 16, initialSliderVal, val -> {
            float spd = (float) (0.1 + val * 0.9);
            savedSpeed = spd;
            if (previewWidget != null) {
                previewWidget.setPlaybackSpeed(spd);
            }
            updateSpeedTooltip();
        });
        updateSpeedTooltip();
        this.addRenderableWidget(speedSlider);

        resetButton = new StyledButton(panelX + panelW - 126, panelY + panelH - 26, 54, 18, Component.literal("Reset"), b -> {
            skillConfig.reset();
            updateWidgets();
            ModConfig.INSTANCE.save();
        });
        this.addRenderableWidget(resetButton);

        doneButton = new StyledButton(panelX + panelW - 68, panelY + panelH - 26, 54, 18, Component.literal("Done"), b -> this.onClose());
        this.addRenderableWidget(doneButton);
    }

    private void updateSpeedTooltip() {
        if (speedSlider != null) {
            speedSlider.setCustomTooltip(Component.literal(String.format(Locale.ROOT, "Playback Speed: %.2fx", savedSpeed)));
        }
    }

    private void updateParticleTabButtons() {
        if (particleButtons.isEmpty()) {
            return;
        }
        particleButtons.get(0).setSelected("[ALL]".equals(selectedParticle));
        for (int i = 0; i < skillInfo.getParticles().size() && i + 1 < particleButtons.size(); i++) {
            particleButtons.get(i + 1).setSelected(skillInfo.getParticles().get(i).equals(selectedParticle));
        }
        updatePreviewWidgetParticles();
    }

    private void updatePreviewWidgetParticles() {
        if (previewWidget == null) {
            return;
        }
        if ("[ALL]".equals(selectedParticle)) {
            previewWidget.setParticleNames(skillInfo.getParticles());
        } else {
            previewWidget.setParticleNames(List.of(selectedParticle));
        }
    }

    private boolean isCurrentEnabled() {
        if ("[ALL]".equals(selectedParticle)) {
            return skillConfig.isEnabled();
        }
        SkillConfig.ParticleSetting ps = skillConfig.getParticleSetting(selectedParticle);
        return ps != null ? ps.isEnabled() : skillConfig.isEnabled();
    }

    private float getCurrentAlpha() {
        if ("[ALL]".equals(selectedParticle)) {
            return skillConfig.getAlpha();
        }
        SkillConfig.ParticleSetting ps = skillConfig.getParticleSetting(selectedParticle);
        return ps != null ? ps.getAlpha() : skillConfig.getAlpha();
    }

    private float getCurrentCulling() {
        if ("[ALL]".equals(selectedParticle)) {
            return skillConfig.getCulling();
        }
        SkillConfig.ParticleSetting ps = skillConfig.getParticleSetting(selectedParticle);
        return ps != null ? ps.getCulling() : skillConfig.getCulling();
    }

    private void updateToggleState() {
        boolean enabled = isCurrentEnabled();
        toggleButton.setMessage(Component.literal(enabled ? "ON" : "OFF"));
        toggleButton.setAccentColor(enabled ? 0xFF55FF55 : 0xFFFF5555);
    }

    private void updateWidgets() {
        updateToggleState();
        if (alphaSlider != null) {
            alphaSlider.setSliderValue(getCurrentAlpha());
        }
        if (alphaBox != null) {
            alphaBox.setNumericValue(getCurrentAlpha());
        }
        if (cullingSlider != null) {
            cullingSlider.setSliderValue(getCurrentCulling());
        }
        if (cullingBox != null) {
            cullingBox.setNumericValue(getCurrentCulling());
        }
        updatePreviewWidgetParticles();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int dialogW = Math.min(this.width - 20, 560);
        int dialogH = Math.min(this.height - 20, 310);
        int startX = (this.width - dialogW) / 2;
        int startY = (this.height - dialogH) / 2;

        graphics.fill(startX, startY, startX + dialogW, startY + dialogH, 0xEE1A1A1E);
        graphics.fill(startX, startY, startX + dialogW, startY + 1, 0xFF4A4A55);
        graphics.fill(startX, startY + dialogH - 1, startX + dialogW, startY + dialogH, 0xFF4A4A55);
        graphics.fill(startX, startY, startX + 1, startY + dialogH, 0xFF4A4A55);
        graphics.fill(startX + dialogW - 1, startY, startX + dialogW, startY + dialogH, 0xFF4A4A55);

        String titleStr = this.title.getString();
        int maxTitleW = dialogW - 40;
        if (this.font.width(titleStr) > maxTitleW) {
            titleStr = this.font.plainSubstrByWidth(titleStr, Math.max(10, maxTitleW - 8)) + "...";
        }
        graphics.drawString(this.font, titleStr, startX + 12, startY + 9, 0xFFE0E0E0, false);

        int panelX = startX + 12;
        int panelY = startY + 27;
        int panelW = dialogW - 24;
        int panelH = dialogH - 35;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x88101014);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF2F2F38);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF2F2F38);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF2F2F38);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF2F2F38);

        int controlsX = panelX + 258;
        int controlsY = panelY + 14 + this.startYOffset;

        graphics.drawString(this.font, "Skill Particles:", controlsX, controlsY + 4, 0xFFCCCCCC, false);
        graphics.drawString(this.font, "Opacity:", controlsX, controlsY + 28, 0xFFCCCCCC, false);
        graphics.drawString(this.font, "Culling:", controlsX, controlsY + 52, 0xFFCCCCCC, false);
        graphics.drawString(this.font, "Speed:", panelX + 14, panelY + panelH - 21, 0xFFCCCCCC, false);

        String errParticle = getActiveErrorParticle();
        if (errParticle != null) {
            String msg = "*This skill(" + errParticle + ") has particle error(typo, mismatch etc), so " + errParticle + " won't show up ingame*";
            int warnW = (panelX + panelW - 14) - controlsX;
            List<FormattedCharSequence> lines = this.font.split(Component.literal(msg), warnW - 12);
            int bannerH = lines.size() * 10 + 6;
            int warnY = controlsY + 74;
            graphics.fill(controlsX, warnY, panelX + panelW - 14, warnY + bannerH, 0x2A441010);
            graphics.fill(controlsX, warnY, controlsX + 2, warnY + bannerH, 0xFFFF5555);
            int textY = warnY + 3;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(this.font, line, controlsX + 6, textY, 0xFFFF7777, false);
                textY += 10;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String getActiveErrorParticle() {
        if (!"[ALL]".equals(selectedParticle)) {
            String err = ParticlePreviewWidget.getParticleError(selectedParticle);
            if (err != null) {
                return err;
            }
        }
        for (String p : skillInfo.getParticles()) {
            String err = ParticlePreviewWidget.getParticleError(p);
            if (err != null) {
                return err;
            }
        }
        return null;
    }

    @Override
    public void onClose() {
        ModConfig.INSTANCE.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
