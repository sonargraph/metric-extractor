package com.hello2morrow.me;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ExtractorTest
{
    @Test
    public void testMetricExtractor()
    {
        MetricExtractor me = new MetricExtractor();
        Map<String, Object> result = me.extractMetrics("src/test/data/Sonargraph_2022-04-22_11-54-43.xml");

        assertNotNull(result);
        assertTrue(result.size() >= 115);
    }

    @Test
    public void testMetricExtractorJson()
    {
        MetricExtractor me = new MetricExtractor();
        String jsonFileName = me.process("src/test/data/Sonargraph_2022-04-22_11-54-43.xml");

        assertNotNull(jsonFileName);
        try
        {
            ObjectMapper mapper = new ObjectMapper();

            // convert JSON file to map
            File jsonFile = Paths.get(jsonFileName).toFile();
            Map<?, ?> map = mapper.readValue(jsonFile, Map.class);

            assertTrue(map.size() >= 115);
            boolean done = jsonFile.delete();
            assertTrue(done);
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }
}
