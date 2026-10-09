package particleeraser.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import particleeraser.config.ModConfig;
import particleeraser.data.SkillInfo;
import particleeraser.data.SkillRegistry;
import particleeraser.gui.components.NumberEditBox;
import particleeraser.gui.components.ParticlePreviewWidget;
import particleeraser.gui.components.SkillListWidget;
import particleeraser.gui.components.SliderWidget;
import particleeraser.gui.components.StyledButton;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    public static final List<ResourceLocation> GLOBAL_PREVIEW_POOL = List.of(
            new ResourceLocation("minecraft", "flame"),
            new ResourceLocation("minecraft", "soul_fire_flame"),
            new ResourceLocation("minecraft", "crit"),
            new ResourceLocation("minecraft", "enchant"),
            new ResourceLocation("minecraft", "dragon_breath"),
            new ResourceLocation("minecraft", "electric_spark")
    );

    public static final List<String> GLOBAL_PREVIEW_NAMES = List.of(
            "Flame",
            "Soul Flame",
            "Crit",
            "Enchant",
            "Dragon Breath",
            "Electric Spark"
    );

    private enum Tab {
        GLOBAL,
        DETAILED
    }

    private static final String[] CATEGORY_KEYS = {
            "all", "warrior", "hunter", "sorcerer", "warlock", "crusader", "cryolancer",
            "brawler", "rogue", "shaman", "minstrel", "chronomancer", "sanguimancer", "merc", "misc", "depr"
    };

    private static final String[] CATEGORY_LABELS = {
            "ALL", "Fighter", "Hunter", "Elementalist", "Warlock", "Crusader", "Cryolancer",
            "Brawler", "Rogue", "Shaman", "Minstrel", "Chronomancer", "Sanguimancer", "Mercenary", "Misc", "Deprecated"
    };

    private final Screen lastScreen;
    private Tab currentTab = Tab.GLOBAL;
    private String savedSearchQuery = "";
    private String selectedCategory = "all";
    private double savedScrollAmount = 0.0;
    private int currentPreviewIndex = 0;
    private boolean isDropdownOpen = false;

    private StyledButton closeButton;
    private StyledButton globalTabButton;
    private StyledButton detailedTabButton;

    private ParticlePreviewWidget previewWidget;
    private StyledButton particleDropdownButton;
    private StyledButton globalToggleButton;
    private SliderWidget globalAlphaSlider;
    private NumberEditBox globalAlphaBox;
    private SliderWidget globalCullingSlider;
    private NumberEditBox globalCullingBox;
    private StyledButton resetGlobalButton;
    private StyledButton resetAllDetailedButton;

    private StyledButton detailedToggleButton;
    private final List<StyledButton> categoryButtons = new ArrayList<>();
    private EditBox searchBox;
    private SkillListWidget skillListWidget;

    public ConfigScreen(Screen lastScreen) {
        super(Component.literal("Particle Eraser"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        SkillRegistry.INSTANCE.initialize();

        if (this.skillListWidget != null) {
            this.savedScrollAmount = this.skillListWidget.getScrollAmount();
        }

        int dialogW = Math.min(this.width - 20, 560);
        int dialogH = Math.min(this.height - 20, 310);
        int startX = (this.width - dialogW) / 2;
        int startY = (this.height - dialogH) / 2;

        closeButton = new StyledButton(startX + dialogW - 24, startY + 5, 18, 18, Component.literal("X"), b -> this.onClose());
        this.addRenderableWidget(closeButton);

        int tabY = startY + 27;
        globalTabButton = new StyledButton(startX + 12, tabY, 110, 18, Component.literal("Global Settings"), b -> switchTab(Tab.GLOBAL));
        this.addRenderableWidget(globalTabButton);

        detailedTabButton = new StyledButton(startX + 126, tabY, 110, 18, Component.literal("Detailed Settings"), b -> switchTab(Tab.DETAILED));
        this.addRenderableWidget(detailedTabButton);

        initGlobalWidgets(startX, startY, dialogW, dialogH);
        initDetailedWidgets(startX, startY, dialogW, dialogH);

        updateTabVisibility();
    }

    private void initGlobalWidgets(int startX, int startY, int dialogW, int dialogH) {
        int panelX = startX + 12;
        int panelY = startY + 49;
        int panelW = dialogW - 24;
        int panelH = dialogH - 57;

        particleDropdownButton = new StyledButton(
                panelX + 16, panelY + 14, 170, 18,
                Component.literal(GLOBAL_PREVIEW_NAMES.get(currentPreviewIndex) + " \u2228"),
                b -> isDropdownOpen = !isDropdownOpen
        );
        this.addRenderableWidget(particleDropdownButton);

        previewWidget = new ParticlePreviewWidget(
                panelX + 16, panelY + 38, 170, 170,
                () -> ModConfig.INSTANCE.isGlobalEnabled(),
                () -> ModConfig.INSTANCE.getGlobalAlpha(),
                () -> ModConfig.INSTANCE.getGlobalCulling()
        );
        previewWidget.setParticleTypes(List.of(GLOBAL_PREVIEW_POOL.get(currentPreviewIndex)));
        this.addRenderableWidget(previewWidget);

        int controlsX = panelX + 195;

        globalToggleButton = new StyledButton(controlsX + 130, panelY + 20, 36, 16, Component.empty(), b -> {
            ModConfig.INSTANCE.setGlobalEnabled(!ModConfig.INSTANCE.isGlobalEnabled());
            updateGlobalToggleState();
            ModConfig.INSTANCE.save();
        });
        globalToggleButton.setCustomTooltip(Component.literal("Global particle settings for all MnS skills.\n(*If disabled, turns off all skill particles)"));
        updateGlobalToggleState();
        this.addRenderableWidget(globalToggleButton);

        detailedToggleButton = new StyledButton(controlsX + 130, panelY + 44, 36, 16, Component.empty(), b -> {
            ModConfig.INSTANCE.setDetailedEnabled(!ModConfig.INSTANCE.isDetailedEnabled());
            updateDetailedToggleState();
            ModConfig.INSTANCE.save();
        });
        detailedToggleButton.setCustomTooltip(Component.literal("Detailed particle settings for individual skills.\n(*If disabled, skills will follow global settings)"));
        updateDetailedToggleState();
        this.addRenderableWidget(detailedToggleButton);

        globalAlphaSlider = new SliderWidget(controlsX + 60, panelY + 70, 160, 16, ModConfig.INSTANCE.getGlobalAlpha(), val -> {
            ModConfig.INSTANCE.setGlobalAlpha(val.floatValue());
            if (globalAlphaBox != null) {
                globalAlphaBox.setNumericValue(val.floatValue());
            }
            ModConfig.INSTANCE.save();
        });
        globalAlphaSlider.setCustomTooltip(Component.literal("Opacity\n(0.00 = invisible, 1.00 = default)"));
        this.addRenderableWidget(globalAlphaSlider);

        globalAlphaBox = new NumberEditBox(this.font, controlsX + 226, panelY + 70, 44, 16, ModConfig.INSTANCE.getGlobalAlpha(), val -> {
            ModConfig.INSTANCE.setGlobalAlpha(val);
            if (globalAlphaSlider != null) {
                globalAlphaSlider.setSliderValue(val);
            }
            ModConfig.INSTANCE.save();
        });
        this.addRenderableWidget(globalAlphaBox);

        globalCullingSlider = new SliderWidget(controlsX + 60, panelY + 96, 160, 16, ModConfig.INSTANCE.getGlobalCulling(), val -> {
            ModConfig.INSTANCE.setGlobalCulling(val.floatValue());
            if (globalCullingBox != null) {
                globalCullingBox.setNumericValue(val.floatValue());
            }
            ModConfig.INSTANCE.save();
        });
        globalCullingSlider.setCustomTooltip(Component.literal("Culling\n(0.00 = cull all, 1.00 = keep all)"));
        this.addRenderableWidget(globalCullingSlider);

        globalCullingBox = new NumberEditBox(this.font, controlsX + 226, panelY + 96, 44, 16, ModConfig.INSTANCE.getGlobalCulling(), val -> {
            ModConfig.INSTANCE.setGlobalCulling(val);
            if (globalCullingSlider != null) {
                globalCullingSlider.setSliderValue(val);
            }
            ModConfig.INSTANCE.save();
        });
        this.addRenderableWidget(globalCullingBox);

        resetGlobalButton = new StyledButton(panelX + panelW - 274, panelY + panelH - 26, 136, 18, Component.literal("Reset Global Settings"), b -> {
            ModConfig.INSTANCE.resetGlobal();
            updateGlobalToggleState();
            updateDetailedToggleState();
            if (globalAlphaSlider != null) {
                globalAlphaSlider.setSliderValue(ModConfig.INSTANCE.getGlobalAlpha());
            }
            if (globalAlphaBox != null) {
                globalAlphaBox.setNumericValue(ModConfig.INSTANCE.getGlobalAlpha());
            }
            if (globalCullingSlider != null) {
                globalCullingSlider.setSliderValue(ModConfig.INSTANCE.getGlobalCulling());
            }
            if (globalCullingBox != null) {
                globalCullingBox.setNumericValue(ModConfig.INSTANCE.getGlobalCulling());
            }
        });
        this.addRenderableWidget(resetGlobalButton);

        resetAllDetailedButton = new StyledButton(panelX + panelW - 134, panelY + panelH - 26, 126, 18, Component.literal("Reset All Detailed"), b -> {
            ModConfig.INSTANCE.resetAllDetailed();
            if (skillListWidget != null) {
                skillListWidget.refreshEntries(savedSearchQuery);
            }
        });
        this.addRenderableWidget(resetAllDetailedButton);
    }

    private void updateGlobalToggleState() {
        boolean enabled = ModConfig.INSTANCE.isGlobalEnabled();
        globalToggleButton.setMessage(Component.literal(enabled ? "ON" : "OFF"));
        globalToggleButton.setAccentColor(enabled ? 0xFF55FF55 : 0xFFFF5555);
    }

    private void initDetailedWidgets(int startX, int startY, int dialogW, int dialogH) {
        int panelX = startX + 12;
        int panelY = startY + 49;
        int panelW = dialogW - 24;
        int panelH = dialogH - 57;

        categoryButtons.clear();
        int catX = panelX + 8;
        int catY = panelY + 8;

        for (int i = 0; i < CATEGORY_KEYS.length; i++) {
            final String key = CATEGORY_KEYS[i];
            final String label = CATEGORY_LABELS[i];
            int btnW = 18;
            String text = "";
            if ("all".equals(key)) {
                btnW = 26;
                text = "ALL";
            } else if ("misc".equals(key)) {
                btnW = 30;
                text = "Misc";
            } else if ("depr".equals(key)) {
                btnW = 32;
                text = "Depr";
            }

            StyledButton btn = new StyledButton(catX, catY, btnW, 18, Component.literal(text), b -> {
                this.selectedCategory = key;
                updateCategoryButtons();
                if (skillListWidget != null) {
                    skillListWidget.setSelectedCategory(this.selectedCategory);
                    skillListWidget.refreshEntries(savedSearchQuery);
                }
            });

            if ("merc".equals(key)) {
                btn.setIcon(new ResourceLocation("mmorpg", "textures/gui/main_hub/icons/mercenary.png"), 16, 16);
            } else if (btnW == 18) {
                btn.setIcon(new ResourceLocation("mmorpg", "textures/gui/asc_classes/class/" + key + ".png"));
            }
            btn.setTooltip(Tooltip.create(Component.literal(label)));
            categoryButtons.add(btn);
            this.addRenderableWidget(btn);
            catX += btnW + 2;
        }
        updateCategoryButtons();

        searchBox = new EditBox(this.font, panelX + 8, panelY + 30, panelW - 16, 18, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search skill, class, or particle..."));
        searchBox.setValue(savedSearchQuery);
        searchBox.setResponder(query -> {
            savedSearchQuery = query;
            if (skillListWidget != null) {
                skillListWidget.refreshEntries(query);
            }
        });
        this.addRenderableWidget(searchBox);

        int listX = panelX + 8;
        int listY = panelY + 52;
        int listW = panelW - 16;
        int listH = panelH - 56;
        skillListWidget = new SkillListWidget(this.minecraft, this, listW, this.height, listY, listY + listH, 28);
        skillListWidget.setLeftPos(listX);
        skillListWidget.setSelectedCategory(selectedCategory);
        skillListWidget.refreshEntries(savedSearchQuery);
        skillListWidget.setScrollAmount(savedScrollAmount);
    }

    private void updateCategoryButtons() {
        for (int i = 0; i < CATEGORY_KEYS.length && i < categoryButtons.size(); i++) {
            categoryButtons.get(i).setSelected(CATEGORY_KEYS[i].equalsIgnoreCase(selectedCategory));
        }
    }

    private void updateDetailedToggleState() {
        boolean enabled = ModConfig.INSTANCE.isDetailedEnabled();
        detailedToggleButton.setMessage(Component.literal(enabled ? "ON" : "OFF"));
        detailedToggleButton.setAccentColor(enabled ? 0xFF55FF55 : 0xFFFF5555);
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        updateTabVisibility();
    }

    private void updateTabVisibility() {
        boolean isGlobal = this.currentTab == Tab.GLOBAL;
        if (!isGlobal) {
            this.isDropdownOpen = false;
        }

        globalTabButton.setSelected(isGlobal);
        detailedTabButton.setSelected(!isGlobal);

        if (previewWidget != null) {
            previewWidget.visible = isGlobal;
        }
        if (particleDropdownButton != null) {
            particleDropdownButton.visible = isGlobal;
        }

        globalToggleButton.visible = isGlobal;
        globalAlphaSlider.visible = isGlobal;
        globalAlphaBox.visible = isGlobal;
        globalCullingSlider.visible = isGlobal;
        globalCullingBox.visible = isGlobal;
        resetGlobalButton.visible = isGlobal;
        resetAllDetailedButton.visible = isGlobal;

        detailedToggleButton.visible = isGlobal;
        for (StyledButton catBtn : categoryButtons) {
            catBtn.visible = !isGlobal;
        }
        searchBox.visible = !isGlobal;
        if (isGlobal && searchBox != null && searchBox.isFocused()) {
            searchBox.setFocused(false);
        }

        if (skillListWidget != null) {
            if (!isGlobal) {
                if (!this.children().contains(skillListWidget)) {
                    this.addWidget(skillListWidget);
                }
            } else {
                this.removeWidget(skillListWidget);
            }
        }
    }

    public void openSkillEdit(SkillInfo skillInfo) {
        if (this.skillListWidget != null) {
            this.savedScrollAmount = this.skillListWidget.getScrollAmount();
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(new SkillEditScreen(this, skillInfo));
        }
    }

    public void refreshDetailedList() {
        if (this.skillListWidget != null) {
            this.skillListWidget.refreshEntries(savedSearchQuery);
        }
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

        graphics.drawString(this.font, this.title, startX + 12, startY + 9, 0xFFE0E0E0, false);

        int panelX = startX + 12;
        int panelY = startY + 49;
        int panelW = dialogW - 24;
        int panelH = dialogH - 57;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x88101014);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF2F2F38);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF2F2F38);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF2F2F38);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF2F2F38);

        if (this.currentTab == Tab.GLOBAL) {
            int controlsX = panelX + 195;
            graphics.drawString(this.font, "Global Particles:", controlsX, panelY + 24, 0xFFCCCCCC, false);
            graphics.drawString(this.font, "Detailed Overrides:", controlsX, panelY + 48, 0xFFCCCCCC, false);
            graphics.drawString(this.font, "Opacity:", controlsX, panelY + 74, 0xFFCCCCCC, false);
            graphics.drawString(this.font, "Culling:", controlsX, panelY + 100, 0xFFCCCCCC, false);
        } else {
            if (skillListWidget != null) {
                skillListWidget.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.currentTab == Tab.GLOBAL && this.isDropdownOpen && particleDropdownButton != null) {
            renderDropdownList(graphics, mouseX, mouseY);
        }
    }

    private void renderDropdownList(GuiGraphics graphics, int mouseX, int mouseY) {
        int dropX = particleDropdownButton.getX();
        int dropY = particleDropdownButton.getY() + particleDropdownButton.getHeight() + 2;
        int dropW = particleDropdownButton.getWidth();
        int itemH = 15;
        int dropH = GLOBAL_PREVIEW_POOL.size() * itemH;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        graphics.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF814141A);
        graphics.fill(dropX, dropY, dropX + dropW, dropY + 1, 0xFFE5B036);
        graphics.fill(dropX, dropY + dropH - 1, dropX + dropW, dropY + dropH, 0xFFE5B036);
        graphics.fill(dropX, dropY, dropX + 1, dropY + dropH, 0xFFE5B036);
        graphics.fill(dropX + dropW - 1, dropY, dropX + dropW, dropY + dropH, 0xFFE5B036);

        for (int i = 0; i < GLOBAL_PREVIEW_POOL.size(); i++) {
            int itemY = dropY + i * itemH;
            boolean hovered = mouseX >= dropX && mouseX < dropX + dropW && mouseY >= itemY && mouseY < itemY + itemH;
            boolean selected = (i == currentPreviewIndex);

            if (hovered) {
                graphics.fill(dropX + 1, itemY, dropX + dropW - 1, itemY + itemH, 0xDD2A2A38);
            }

            int textColor = selected ? 0xFFFFDD66 : (hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            graphics.drawString(this.font, GLOBAL_PREVIEW_NAMES.get(i), dropX + 6, itemY + 4, textColor, false);
        }

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.currentTab == Tab.GLOBAL && this.isDropdownOpen && particleDropdownButton != null) {
            int dropX = particleDropdownButton.getX();
            int dropY = particleDropdownButton.getY() + particleDropdownButton.getHeight() + 2;
            int dropW = particleDropdownButton.getWidth();
            int itemH = 15;
            int dropH = GLOBAL_PREVIEW_POOL.size() * itemH;

            if (mouseX >= dropX && mouseX < dropX + dropW && mouseY >= dropY && mouseY < dropY + dropH) {
                int clickedIdx = (int) ((mouseY - dropY) / itemH);
                if (clickedIdx >= 0 && clickedIdx < GLOBAL_PREVIEW_POOL.size()) {
                    currentPreviewIndex = clickedIdx;
                    particleDropdownButton.setMessage(Component.literal(GLOBAL_PREVIEW_NAMES.get(currentPreviewIndex) + " \u2228"));
                    if (previewWidget != null) {
                        previewWidget.setParticleTypes(List.of(GLOBAL_PREVIEW_POOL.get(currentPreviewIndex)));
                    }
                    isDropdownOpen = false;
                    return true;
                }
            } else if (!particleDropdownButton.isMouseOver(mouseX, mouseY)) {
                isDropdownOpen = false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ModConfig.INSTANCE.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
