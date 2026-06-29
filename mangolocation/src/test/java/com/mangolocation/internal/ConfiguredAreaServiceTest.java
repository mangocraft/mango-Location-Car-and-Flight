package com.mangolocation.internal;

import com.mangolocation.api.Area;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredAreaServiceTest {

    @Test
    void detectsAdministrativeAreasOnlyInMainWorld() {
        ConfiguredAreaService service = loadDefaultService();

        assertEquals("san_cheng", service.findArea("world", 0, 10_000).orElseThrow().id());
        assertEquals("huadu", service.findArea("world", 12_000, 10_000).orElseThrow().id());
        assertEquals("interchange", service.findArea("world", 0, 0).orElseThrow().id());
        Area outskirts = service.findArea("world", -20_000, -20_000).orElseThrow();
        assertEquals("outskirts", outskirts.id());
        assertEquals("远郊", outskirts.name());
    }

    @Test
    void returnsFriendlyWorldAreasOutsideMainWorld() {
        ConfiguredAreaService service = loadDefaultService();

        Area nether = service.findArea("world_nether", 0, 10_000).orElseThrow();
        Area end = service.findArea("world_the_end", 12_000, 10_000).orElseThrow();
        Area custom = service.findArea("resource_world", 0, 0).orElseThrow();

        assertEquals("地狱", nether.name());
        assertEquals("末地", end.name());
        assertEquals("resource_world", custom.name());
        assertTrue(nether.isWorldArea());
    }

    @Test
    void usesPriorityOnSharedBoundaries() {
        ConfiguredAreaService service = loadDefaultService();

        assertEquals("interchange", service.findArea("world", 5348, 4892).orElseThrow().id());
    }

    private ConfiguredAreaService loadDefaultService() {
        ConfiguredAreaService service = new ConfiguredAreaService();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        service.reload(config);
        return service;
    }
}
