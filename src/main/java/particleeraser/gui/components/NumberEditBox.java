package particleeraser.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

public class NumberEditBox extends EditBox {
    private final Consumer<Float> onValidValue;
    private boolean updatingInternally = false;

    public NumberEditBox(Font font, int x, int y, int width, int height, float initialValue, Consumer<Float> onValidValue) {
        super(font, x, y, width, height, Component.empty());
        this.onValidValue = onValidValue;
        this.setMaxLength(6);
        this.setNumericValue(initialValue);
        this.setResponder(this::handleTextChange);
    }

    private void handleTextChange(String text) {
        if (updatingInternally) {
            return;
        }
        try {
            String trimmed = text.trim();
            if (trimmed.endsWith("%")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
                float percent = Float.parseFloat(trimmed);
                float normalized = Math.max(0.0f, Math.min(1.0f, percent / 100.0f));
                if (onValidValue != null) {
                    onValidValue.accept(normalized);
                }
            } else if (!trimmed.isEmpty()) {
                float val = Float.parseFloat(trimmed);
                if (val > 1.0f && val <= 100.0f) {
                    val = val / 100.0f;
                }
                float normalized = Math.max(0.0f, Math.min(1.0f, val));
                if (onValidValue != null) {
                    onValidValue.accept(normalized);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public void setNumericValue(float val) {
        this.updatingInternally = true;
        this.setValue(String.format(Locale.ROOT, "%.2f", Math.max(0.0f, Math.min(1.0f, val))));
        this.updatingInternally = false;
    }
}
