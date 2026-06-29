package com.mangolocation.internal;

import com.mangolocation.api.Area;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredAreaServiceTest {

    @Test
    void detectsAdministrativeAreasOnlyInMainWorld() {
        ConfiguredAreaService service = loadDefaultService();

        assertEquals("san_cheng", service.findArea("world", 0, 10_000).orElseThrow().id());
        assertEquals("huadu", service.findArea("world", 12_000, 10_000).orElseThrow().id());
        assertEquals("Hutong", service.findArea("world", 0, 0).orElseThrow().id());
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

        assertEquals("Hutong", service.findArea("world", 5348, 4892).orElseThrow().id());
    }

    @Test
    void loadsPoeticEnterAndLeaveMessages() {
        YamlConfiguration config = loadDefaultConfig();
        config.set("tracking.area-messages", null);
        ConfiguredAreaService service = new ConfiguredAreaService();
        service.reload(config);
        Area hutong = service.findArea("world", 0, 0).orElseThrow();

        assertTrue(service.getEnterMessages(hutong).getFirst().contains("魅力互通"));
        assertTrue(service.getLeaveMessages(hutong).getLast().contains("一路有清风"));
    }

    @Test
    void migratesLegacyInterchangeIdToHutong() {
        YamlConfiguration config = loadDefaultConfig();
        Map<String, Object> values = config.getConfigurationSection("areas.Hutong").getValues(false);
        config.set("areas.Hutong", null);
        config.createSection("areas.interchange", values);

        ConfiguredAreaService service = new ConfiguredAreaService();
        service.reload(config);

        assertEquals("Hutong", service.findArea("world", 0, 0).orElseThrow().id());
    }

    private ConfiguredAreaService loadDefaultService() {
        ConfiguredAreaService service = new ConfiguredAreaService();
        YamlConfiguration config = loadDefaultConfig();
        service.reload(config);
        return service;
    }

    private YamlConfiguration loadDefaultConfig() {
        return YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
    }
}
