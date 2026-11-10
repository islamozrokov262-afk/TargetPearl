package com.islamozroko.pearlmirror;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PearlMirrorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Path.of("config", "pearlmirror.json");

    public boolean enabled = true;

    // Поиск чужого перла
    public float radius = 30.0F;
    public boolean allPearls = false;
    public float minHearts = 0.0F;

    // Тайминги
    public int delayTicks = 7;
    public int throwTicks = 2;

    // Фильтр целей
    public boolean onlyAuraTarget = false;   // ловить только цель Aura (если Aura есть)
    public boolean fallbackToAny = true;     // если цели Aura нет — ловить любой
    public boolean ignoreTeammates = true;   // не реагировать на тиммейтов
    public List<String> teammateList = new ArrayList<>(); // ники тиммейтов
    public boolean onlyFromList = false;     // ловить только игроков из playerList
    public List<String> playerList = new ArrayList<>();   // ники врагов
    public boolean preferClosest = true;     // при нескольких перлах — ближайший

    public static PearlMirrorConfig load() {
        try {
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH);
                PearlMirrorConfig cfg = GSON.fromJson(json, PearlMirrorConfig.class);
                if (cfg != null) {
                    if (cfg.teammateList == null) cfg.teammateList = new ArrayList<>();
                    if (cfg.playerList == null) cfg.playerList = new ArrayList<>();
                    return cfg;
                }
            }
        } catch (IOException ignored) {}
        PearlMirrorConfig cfg = new PearlMirrorConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) {}
    }
}
