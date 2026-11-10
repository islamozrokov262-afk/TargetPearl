package com.islamozroko.pearlmirror;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;

public class PearlMirrorConfigScreen extends OptionsSubScreen {

    public PearlMirrorConfigScreen(Screen parent) {
        super(parent, net.minecraft.client.Minecraft.getInstance().options, Component.literal("Pearl Mirror Settings"));
    }

    @Override
    protected void addOptions() {
        PearlMirrorConfig cfg = PearlMirrorMod.getConfig();
        if (cfg == null) return;

        if (this.list != null) {
            // Основные настройки
            this.list.addSmall(
                    new net.minecraft.client.OptionInstance[]{
                            // Радиус
                            new net.minecraft.client.OptionInstance<>(
                                    "Радиус поиска",
                                    net.minecraft.client.OptionInstance.doubleRange(5.0, 100.0),
                                    (v) -> String.format("%.0f", v),
                                    cfg.radius,
                                    (v) -> cfg.radius = v.floatValue()
                            ),
                            // Задержка
                            new net.minecraft.client.OptionInstance<>(
                                    "Задержка (тики)",
                                    net.minecraft.client.OptionInstance.intRange(1, 40),
                                    (v) -> String.valueOf(v),
                                    cfg.delayTicks,
                                    (v) -> cfg.delayTicks = v
                            )
                    }
            );

            // Булевы настройки
            this.list.addSmall(
                    new net.minecraft.client.OptionInstance[]{
                            net.minecraft.client.OptionInstance.createBoolean("Игнорить тиммейтов", cfg.ignoreTeammates, (v) -> cfg.ignoreTeammates = v),
                            net.minecraft.client.OptionInstance.createBoolean("Только ближайший перл", cfg.preferClosest, (v) -> cfg.preferClosest = v)
                    }
            );

            // Сохранение
            this.list.addSmall(
                    new net.minecraft.client.OptionInstance[]{
                            net.minecraft.client.OptionInstance.createBoolean("Включить модуль", cfg.enabled, (v) -> {
                                cfg.enabled = v;
                                cfg.save();
                            })
                    }
            );
        }
    }

    @Override
    public void onClose() {
        PearlMirrorConfig cfg = PearlMirrorMod.getConfig();
        if (cfg != null) cfg.save();
        super.onClose();
    }
}
