package ca.bkaw.torque.components;

import ca.bkaw.torque.light.LightPattern;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates that the warning lights configuration shipped in the police car
 * vehicle type parses.
 */
public class WarningLightsConfigTest {
    @Test
    void policeCarConfigParses() throws Exception {
        JsonObject vehicleJson;
        try (InputStream stream = WarningLightsConfigTest.class
            .getResourceAsStream("/data/torque/torque_vehicle/police_car.json")) {
            assertNotNull(stream, "police_car.json not found on classpath");
            vehicleJson = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject();
        }

        JsonObject componentJson = null;
        for (JsonElement element : vehicleJson.getAsJsonArray("components")) {
            JsonObject json = element.getAsJsonObject();
            if (json.get("type").getAsString().equals("torque:warning_lights")) {
                componentJson = json;
            }
        }
        assertNotNull(componentJson, "police car has no warning_lights component");

        Object config = WarningLightsComponent.TYPE.configParser().apply(componentJson);
        WarningLightsComponent.Config parsed = (WarningLightsComponent.Config) config;

        assertEquals("emergency", parsed.defaultMode());
        Map<String, LightPattern> modes = parsed.modes();
        assertTrue(modes.containsKey("emergency"));
        assertTrue(modes.containsKey("scene"));
        assertTrue(modes.containsKey("road_block"));
        assertTrue(modes.containsKey("stop_signal"));

        // The emergency pattern must alternate: the two lightbar sides are
        // never fully lit at the same time.
        LightPattern emergency = modes.get("emergency");
        for (int t = 0; t < 200; t++) {
            boolean left = emergency.getState("lightbar_2", t).on();
            boolean right = emergency.getState("lightbar_5", t).on();
            assertTrue(!(left && right), "both lightbar sides lit at tick " + t);
        }
    }
}
