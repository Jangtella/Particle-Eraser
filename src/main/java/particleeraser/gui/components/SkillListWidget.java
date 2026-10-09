package particleeraser.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import particleeraser.config.ModConfig;
import particleeraser.data.SkillInfo;
import particleeraser.data.SkillRegistry;
import particleeraser.gui.ConfigScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkillListWidget extends ContainerObjectSelectionList<SkillListEntry> {
    private final ConfigScreen configScreen;
    private String selectedCategory = "all";

    public SkillListWidget(Minecraft minecraft, ConfigScreen configScreen, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.configScreen = configScreen;
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    public void setSelectedCategory(String category) {
        this.selectedCategory = category != null ? category.toLowerCase(Locale.ROOT) : "all";
    }

    public String getSelectedCategory() {
        return this.selectedCategory;
    }

    public void refreshEntries(String filterQuery) {
        double currentScroll = this.getScrollAmount();
        this.clearEntries();
        String query = filterQuery == null ? "" : filterQuery.trim().toLowerCase(Locale.ROOT);
        List<SkillInfo> allSkills = SkillRegistry.INSTANCE.getAllSkills();

        List<SkillInfo> filtered = new ArrayList<>();
        for (SkillInfo skill : allSkills) {
            if (skill.getParticles().isEmpty()) {
                continue;
            }
            if (!"all".equalsIgnoreCase(selectedCategory)) {
                if (!skill.getSchool().equalsIgnoreCase(selectedCategory)) {
                    continue;
                }
            }
            if (query.isEmpty() || skill.getSearchableText().contains(query)) {
                filtered.add(skill);
            }
        }

        filtered.sort((a, b) -> {
            boolean aMod = ModConfig.INSTANCE.getOrCreateSkillConfig(a.getId()).isModified();
            boolean bMod = ModConfig.INSTANCE.getOrCreateSkillConfig(b.getId()).isModified();
            if (aMod != bMod) {
                return aMod ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (SkillInfo skill : filtered) {
            this.addEntry(new SkillListEntry(this.minecraft, this.configScreen, skill));
        }

        this.setScrollAmount(currentScroll);
    }

    @Override
    public int getRowWidth() {
        return Math.max(100, this.width - 16);
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x0 + this.width - 6;
    }
}
