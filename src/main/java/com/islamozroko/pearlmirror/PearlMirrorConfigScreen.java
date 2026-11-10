package com.islamozroko.pearlmirror;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class PearlMirrorConfigScreen extends OptionsSubScreen {

    public PearlMirrorConfigScreen(Screen parent) {
        super(parent, net.minecraft.client.Minecraft.getInstance().options, Component.literal("Pearl Mirror Settings"));
    }

    // Добавили метод create для совместимости с PearlMirrorModMenu
    public static Screen create(Screen parent) {
        return new PearlMirrorConfigScreen(parent);
    }

    @Override
    protected void addOptions() {
        PearlMirrorConfig cfg = PearlMirrorMod.getConfig();
        if (cfg == null || this.list == null) return;

        this.list.addSmall(
                OptionInstance.createBoolean("Включить модуль", cfg.enabled, (v) -> cfg.enabled = v),
                OptionInstance.createBoolean("Игнорить тиммейтов", cfg.ignoreTeammates, (v) -> cfg.ignoreTeammates = v)
        );

        this.list.addSmall(
                OptionInstance.createBoolean("Только ближайший перл", cfg.preferClosest, (v) -> cfg.preferClosest = v),
                OptionInstance.createBoolean("Только цель Aura", cfg.onlyAuraTarget, (v) -> cfg.onlyAuraTarget = v)
        );
    }

    @Override
    public void onClose() {
        PearlMirrorConfig cfg = PearlMirrorMod.getConfig();
        if (cfg != null) cfg.save();
        super.onClose();
    }
}
