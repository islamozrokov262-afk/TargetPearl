package com.islamozroko.pearlmirror;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PearlMirrorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pearlmirror.json").toFile();

    public boolean enabled = true;
    public double radius = 70.0;
    public double pitchBoost = 8.0;
    
    // Код кнопки Килки (R = 82)
    public int auraKeyCode = 82; 

    public static PearlMirrorConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                PearlMirrorConfig config = GSON.fromJson(reader, PearlMirrorConfig.class);
                if (config != null) return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PearlMirrorConfig config = new PearlMirrorConfig();
        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
