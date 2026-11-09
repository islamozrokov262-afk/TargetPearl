package com.islamozroko.pearlmirror;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PearlMirrorConfigScreen {
    public static Screen create(Screen parent) {
        PearlMirrorConfig config = PearlMirrorMod.getConfig();
        
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Pearl Mirror"))
                .setSavingRunnable(config::save);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("Основное"));
        
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Включить"), config.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.enabled = newValue)
                .build());
                
        general.addEntry(entryBuilder.startDoubleField(Component.literal("Радиус поиска (блоки)"), config.radius)
                .setDefaultValue(70.0)
                .setMin(5.0).setMax(100.0)
                .setSaveConsumer(newValue -> config.radius = newValue)
                .build());
        
        return builder.build();
    }
}
