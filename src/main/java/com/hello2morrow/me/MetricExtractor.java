package com.hello2morrow.me;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.ISonargraphSystemController;
import com.hello2morrow.sonargraph.integration.access.controller.ISystemInfoProcessor;
import com.hello2morrow.sonargraph.integration.access.foundation.Result;
import com.hello2morrow.sonargraph.integration.access.model.IMetricId;
import com.hello2morrow.sonargraph.integration.access.model.IMetricLevel;
import com.hello2morrow.sonargraph.integration.access.model.IMetricValue;

public class MetricExtractor
{
    public static void main(String[] args)
    {
        MetricExtractor me = new MetricExtractor();

        for (String xmlFileName : args)
        {
            me.process(xmlFileName);
        }
    }

    Map<String, Object> extractMetrics(String xmlFileName)
    {
        final ISonargraphSystemController controller = ControllerFactory.createController();
        final Result result = controller.loadSystemReport(new File(xmlFileName));

        if (!result.isSuccess())
        {
            System.err.println(result.toString());
            System.exit(1);
            return null;
        }

        ISystemInfoProcessor proc = controller.createSystemInfoProcessor();
        IMetricLevel systemLevel = proc.getMetricLevel(IMetricLevel.SYSTEM).get();
        Map<String, Object> systemMetrics = new HashMap<>();

        for (IMetricId metricId : proc.getMetricIdsForLevel(systemLevel))
        {
            for (IMetricValue val : proc.getMetricValues(IMetricLevel.SYSTEM, metricId.getName()).values())
            {
                systemMetrics.put(val.getId().getName(), val.getValue());
            }
        }
        systemMetrics.put("systemId", proc.getSoftwareSystem().getSystemId());
        systemMetrics.put("systemName", proc.getSoftwareSystem().getName());
        systemMetrics.put("timestamp", new Date(proc.getSoftwareSystem().getTimestamp()));
        return systemMetrics;
    }

    String process(String xmlFileName)
    {
        Map<String, Object> systemMetrics = extractMetrics(xmlFileName);
        ObjectMapper objectMapper = new ObjectMapper();

        try
        {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(systemMetrics);
            String jsonFileName = xmlFileName.substring(0, xmlFileName.length()-3) + "json";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFileName)))
            {
                writer.write(json);
            }
            catch (IOException e)
            {
                System.err.println(e.getMessage());
            }
            return jsonFileName;
        }
        catch (JsonProcessingException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
