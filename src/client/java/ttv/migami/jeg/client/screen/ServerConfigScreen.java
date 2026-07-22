package ttv.migami.jeg.client.screen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.client.ServerConfigClient;
import ttv.migami.jeg.config.ServerConfigOptions;

public final class ServerConfigScreen extends Screen {
    private static final int GOLD = 0xFFE0B15A;
    private static final int PANEL = 0xD920252B;
    private static final int PANEL_DARK = 0xE612161A;
    private static final int ROW_HEIGHT = 24;
    private static final int CONTROL_WIDTH = 136;

    private final Screen parent;
    private final Map<String, String> baseline = new LinkedHashMap<>();
    private final Map<String, String> drafts = new LinkedHashMap<>();
    private final Set<String> invalidKeys = new HashSet<>();
    private final String[] growthTypes = Config.gunnerGrowthTypes();
    private final Map<ServerConfigOptions.Category, Integer> scrollOffsets = new EnumMap<>(ServerConfigOptions.Category.class);
    private final List<VisibleRow> visibleRows = new ArrayList<>();

    private ServerConfigOptions.Category category = ServerConfigOptions.Category.UI;
    private int growthTypeIndex;
    private int panelLeft;
    private int panelRight;
    private int contentTop;
    private int contentBottom;
    private int workspaceLeft;
    private int workspaceRight;
    private int visibleRowCount;
    private boolean wideLayout;
    private boolean applying;
    private Button applyButton;
    private Component statusMessage = Component.empty();

    public ServerConfigScreen(Screen parent, Map<String, String> values) {
        super(Component.translatable("gui.jegn.config.title"));
        this.parent = parent;
        this.baseline.putAll(values);
        this.drafts.putAll(values);
        for (ServerConfigOptions.Category value : ServerConfigOptions.Category.values()) {
            this.scrollOffsets.put(value, 0);
        }
    }

    public Screen parent() {
        return this.parent;
    }

    @Override
    protected void init() {
        this.visibleRows.clear();
        this.wideLayout = this.width >= 520;
        int panelWidth = Math.min(this.width - 20, 620);
        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelRight = this.panelLeft + panelWidth;
        this.contentTop = this.wideLayout ? 52 : 78;
        this.contentBottom = this.height - 42;

        if (this.wideLayout) {
            this.workspaceLeft = this.panelLeft + 126;
            this.addCategoryButtons();
        } else {
            this.workspaceLeft = this.panelLeft + 10;
            this.addCompactCategoryControls();
        }
        this.workspaceRight = this.panelRight - 10;

        int rowsTop = this.contentTop;
        if (this.category == ServerConfigOptions.Category.MOB) {
            this.addGrowthTypeControls(rowsTop);
            rowsTop += ROW_HEIGHT + 4;
        }
        this.visibleRowCount = Math.max(1, (this.contentBottom - rowsTop) / ROW_HEIGHT);
        this.addOptionControls(rowsTop);
        this.addScrollButtons(rowsTop);

        int footerY = this.height - 28;
        int buttonWidth = Math.min(100, (this.width - 28) / 3);
        int footerLeft = (this.width - buttonWidth * 3 - 8) / 2;
        Button resetButton = Button.builder(
                Component.translatable("gui.jegn.config.reset"),
                button -> this.resetCategory()
        ).bounds(footerLeft, footerY, buttonWidth, 20).build();
        resetButton.setTooltip(Tooltip.create(Component.translatable("gui.jegn.config.reset.tooltip")));
        this.addRenderableWidget(resetButton);
        this.applyButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jegn.config.apply"),
                button -> this.applyChanges()
        ).bounds(footerLeft + buttonWidth + 4, footerY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> this.onClose()
        ).bounds(footerLeft + (buttonWidth + 4) * 2, footerY, buttonWidth, 20).build());
        this.updateApplyButton();
    }

    private void addCategoryButtons() {
        int y = this.contentTop;
        for (ServerConfigOptions.Category value : ServerConfigOptions.Category.values()) {
            Button button = Button.builder(Component.translatable(value.translationKey()), ignored -> this.selectCategory(value))
                    .bounds(this.panelLeft + 10, y, 106, 20)
                    .build();
            button.active = value != this.category;
            this.addRenderableWidget(button);
            y += ROW_HEIGHT;
        }
    }

    private void addCompactCategoryControls() {
        int center = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.shiftCategory(-1))
                .bounds(center - 104, 48, 20, 20)
                .build());
        Button current = Button.builder(Component.translatable(this.category.translationKey()), button -> {})
                .bounds(center - 80, 48, 160, 20)
                .build();
        current.active = false;
        this.addRenderableWidget(current);
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.shiftCategory(1))
                .bounds(center + 84, 48, 20, 20)
                .build());
    }

    private void addGrowthTypeControls(int y) {
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.shiftGrowthType(-1))
                .bounds(this.workspaceLeft, y, 20, 20)
                .build());
        int centerWidth = Math.max(80, this.workspaceRight - this.workspaceLeft - 48);
        Button current = Button.builder(this.growthTypeLabel(), button -> {})
                .bounds(this.workspaceLeft + 24, y, centerWidth, 20)
                .build();
        current.active = false;
        current.setTooltip(Tooltip.create(Component.translatable("gui.jegn.config.growth.type.tooltip")));
        this.addRenderableWidget(current);
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.shiftGrowthType(1))
                .bounds(this.workspaceRight - 20, y, 20, 20)
                .build());
    }

    private void addOptionControls(int rowsTop) {
        List<ServerConfigOptions.Option> options = this.currentOptions();
        int maxOffset = Math.max(0, options.size() - this.visibleRowCount);
        int offset = Mth.clamp(this.scrollOffsets.get(this.category), 0, maxOffset);
        this.scrollOffsets.put(this.category, offset);
        int end = Math.min(options.size(), offset + this.visibleRowCount);
        for (int index = offset; index < end; index++) {
            ServerConfigOptions.Option option = options.get(index);
            int y = rowsTop + (index - offset) * ROW_HEIGHT;
            int controlX = Math.max(this.workspaceLeft + 110, this.workspaceRight - CONTROL_WIDTH - 24);
            AbstractWidget control = this.createControl(option, controlX, y);
            this.addRenderableWidget(control);
            this.visibleRows.add(new VisibleRow(option, y, controlX - 8));
        }
    }

    private AbstractWidget createControl(ServerConfigOptions.Option option, int x, int y) {
        if (option.type() == ServerConfigOptions.ValueType.BOOLEAN) {
            boolean enabled = Boolean.parseBoolean(this.drafts.get(option.key()));
            Button button = Button.builder(this.booleanLabel(enabled), ignored -> {
                boolean next = !Boolean.parseBoolean(this.drafts.get(option.key()));
                this.drafts.put(option.key(), Boolean.toString(next));
                ignored.setMessage(this.booleanLabel(next));
                this.invalidKeys.remove(option.key());
                this.statusMessage = Component.empty();
                this.updateApplyButton();
            }).bounds(x, y, CONTROL_WIDTH, 20).build();
            button.setTooltip(this.optionTooltip(option));
            return button;
        }

        EditBox input = new EditBox(this.font, x, y, CONTROL_WIDTH, 20, Component.translatable(option.labelKey()));
        input.setMaxLength(option.type() == ServerConfigOptions.ValueType.BLOCK_ID ? 256 : 64);
        input.setValue(this.drafts.getOrDefault(option.key(), ""));
        input.setTooltip(this.optionTooltip(option));
        input.setResponder(value -> {
            this.drafts.put(option.key(), value);
            if (this.isValid(option, value)) {
                this.invalidKeys.remove(option.key());
                input.setTextColor(0xFFE0E0E0);
            } else {
                this.invalidKeys.add(option.key());
                input.setTextColor(0xFFFF7777);
            }
            this.statusMessage = Component.empty();
            this.updateApplyButton();
        });
        if (!this.isValid(option, input.getValue())) {
            input.setTextColor(0xFFFF7777);
        }
        return input;
    }

    private void addScrollButtons(int rowsTop) {
        List<ServerConfigOptions.Option> options = this.currentOptions();
        int maxOffset = Math.max(0, options.size() - this.visibleRowCount);
        if (maxOffset == 0) {
            return;
        }
        int offset = this.scrollOffsets.get(this.category);
        Button up = Button.builder(Component.literal("^"), button -> this.scrollBy(-1))
                .bounds(this.workspaceRight - 20, rowsTop, 20, 20)
                .build();
        Button down = Button.builder(Component.literal("v"), button -> this.scrollBy(1))
                .bounds(this.workspaceRight - 20, this.contentBottom - 20, 20, 20)
                .build();
        up.active = offset > 0;
        down.active = offset < maxOffset;
        this.addRenderableWidget(up);
        this.addRenderableWidget(down);
    }

    private List<ServerConfigOptions.Option> currentOptions() {
        return ServerConfigOptions.forCategory(this.category, this.growthTypes[this.growthTypeIndex]);
    }

    private void selectCategory(ServerConfigOptions.Category value) {
        this.category = value;
        this.statusMessage = Component.empty();
        this.rebuildWidgets();
    }

    private void shiftCategory(int direction) {
        ServerConfigOptions.Category[] values = ServerConfigOptions.Category.values();
        int index = Math.floorMod(this.category.ordinal() + direction, values.length);
        this.selectCategory(values[index]);
    }

    private void shiftGrowthType(int direction) {
        this.growthTypeIndex = Math.floorMod(this.growthTypeIndex + direction, this.growthTypes.length);
        this.scrollOffsets.put(ServerConfigOptions.Category.MOB, 0);
        this.statusMessage = Component.empty();
        this.rebuildWidgets();
    }

    private void scrollBy(int amount) {
        List<ServerConfigOptions.Option> options = this.currentOptions();
        int maxOffset = Math.max(0, options.size() - this.visibleRowCount);
        int current = this.scrollOffsets.get(this.category);
        int next = Mth.clamp(current + amount, 0, maxOffset);
        if (next != current) {
            this.scrollOffsets.put(this.category, next);
            this.rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= this.workspaceLeft && mouseX <= this.workspaceRight
                && mouseY >= this.contentTop && mouseY <= this.contentBottom) {
            this.scrollBy(deltaY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void applyChanges() {
        if (this.applying || !this.invalidKeys.isEmpty()) {
            return;
        }
        Map<String, String> changes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this.drafts.entrySet()) {
            if (!entry.getValue().equals(this.baseline.get(entry.getKey()))) {
                changes.put(entry.getKey(), entry.getValue());
            }
        }
        if (changes.isEmpty()) {
            return;
        }
        this.applying = true;
        this.statusMessage = Component.translatable("gui.jegn.config.status.applying").withStyle(ChatFormatting.YELLOW);
        this.updateApplyButton();
        ServerConfigClient.apply(changes);
    }

    private void resetCategory() {
        for (ServerConfigOptions.Option option : ServerConfigOptions.all()) {
            if (option.category() == this.category) {
                this.drafts.put(option.key(), String.valueOf(Config.getDefaultConfigValue(option.key())));
                this.invalidKeys.remove(option.key());
            }
        }
        this.statusMessage = Component.empty();
        this.rebuildWidgets();
    }

    public void applyAuthoritativeValues(Map<String, String> values, int changedCount) {
        this.baseline.clear();
        this.baseline.putAll(values);
        this.drafts.clear();
        this.drafts.putAll(values);
        this.invalidKeys.clear();
        this.applying = false;
        this.statusMessage = Component.translatable("gui.jegn.config.status.applied", changedCount).withStyle(ChatFormatting.GREEN);
        this.rebuildWidgets();
    }

    public void showInvalidValues(List<String> keys) {
        this.applying = false;
        this.invalidKeys.addAll(keys);
        this.statusMessage = Component.translatable("gui.jegn.config.status.invalid").withStyle(ChatFormatting.RED);
        this.rebuildWidgets();
    }

    private void updateApplyButton() {
        if (this.applyButton != null) {
            this.applyButton.active = !this.applying && this.invalidKeys.isEmpty() && this.hasDirtyValues();
        }
    }

    private boolean hasDirtyValues() {
        for (Map.Entry<String, String> entry : this.drafts.entrySet()) {
            if (!entry.getValue().equals(this.baseline.get(entry.getKey()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isValid(ServerConfigOptions.Option option, String rawValue) {
        try {
            return switch (option.type()) {
                case BOOLEAN -> "true".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue);
                case INTEGER -> {
                    int value = Integer.parseInt(rawValue);
                    yield value >= option.min() && value <= option.max();
                }
                case DOUBLE -> {
                    double value = Double.parseDouble(rawValue);
                    yield Double.isFinite(value) && value >= option.min() && value <= option.max();
                }
                case BLOCK_ID -> {
                    Identifier id = Identifier.tryParse(rawValue);
                    yield id != null;
                }
            };
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private Component booleanLabel(boolean enabled) {
        return Component.translatable(enabled ? "options.on" : "options.off");
    }

    private Component growthTypeLabel() {
        return Component.translatable("gui.jegn.config.growth.type." + this.growthTypes[this.growthTypeIndex]);
    }

    private Tooltip optionTooltip(ServerConfigOptions.Option option) {
        MutableComponent tooltip = Component.translatable(option.labelKey()).withStyle(ChatFormatting.GOLD);
        tooltip.append("\\n").append(Component.translatable("gui.jegn.config.tooltip.key", option.key()));
        if (option.hasRange()) {
            tooltip.append("\n").append(Component.translatable("gui.jegn.config.tooltip.range", formatNumber(option.min()), formatNumber(option.max())));
        }
        if (option.isGrowthOption()) {
            tooltip.append("\n").append(Component.translatable("gui.jegn.config.tooltip.inherit"));
        }
        return Tooltip.create(tooltip);
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(this.panelLeft, 10, this.panelRight, this.height - 34, PANEL);
        guiGraphics.fill(this.workspaceLeft - 6, this.contentTop - 6, this.workspaceRight + 6, this.contentBottom + 2, PANEL_DARK);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 20, GOLD);

        for (VisibleRow row : this.visibleRows) {
            int color = this.invalidKeys.contains(row.option.key()) ? 0xFFFF7777 : 0xFFE0E0E0;
            Component label = Component.translatable(row.option.labelKey());
            int maxWidth = Math.max(40, row.labelRight - this.workspaceLeft - 8);
            boolean dirty = !this.drafts.getOrDefault(row.option.key(), "").equals(this.baseline.get(row.option.key()));
            String suffix = dirty ? " *" : "";
            String rendered = this.font.plainSubstrByWidth(label.getString(), Math.max(20, maxWidth - this.font.width(suffix))) + suffix;
            if (dirty) {
                color = GOLD;
            }
            guiGraphics.text(this.font, rendered, this.workspaceLeft + 4, row.y + 6, color, false);
        }
        if (!this.statusMessage.getString().isEmpty()) {
            guiGraphics.centeredText(this.font, this.statusMessage, this.width / 2, this.height - 39, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (!this.hasDirtyValues()) {
            this.minecraft.setScreenAndShow(this.parent);
            return;
        }
        this.minecraft.setScreenAndShow(new ConfirmScreen(
                discard -> this.minecraft.setScreenAndShow(discard ? this.parent : this),
                Component.translatable("gui.jegn.config.discard.title"),
                Component.translatable("gui.jegn.config.discard.message"),
                Component.translatable("gui.yes"),
                Component.translatable("gui.no")
        ));
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private record VisibleRow(ServerConfigOptions.Option option, int y, int labelRight) {}
}
