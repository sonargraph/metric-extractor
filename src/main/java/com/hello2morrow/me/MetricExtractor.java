package com.hello2morrow.me;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.IModuleInfoProcessor;
import com.hello2morrow.sonargraph.integration.access.controller.ISonargraphSystemController;
import com.hello2morrow.sonargraph.integration.access.controller.ISystemInfoProcessor;
import com.hello2morrow.sonargraph.integration.access.foundation.Result;
import com.hello2morrow.sonargraph.integration.access.model.IMetricId;
import com.hello2morrow.sonargraph.integration.access.model.IMetricLevel;
import com.hello2morrow.sonargraph.integration.access.model.IMetricValue;
import com.hello2morrow.sonargraph.integration.access.model.IModule;
import com.hello2morrow.sonargraph.integration.access.model.INamedElement;

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
                systemMetrics.put(val.getId().getProvider().getName() + ":" + val.getId().getName(), val.getValue());
            }
        }

        Map<String, Object> modules = new HashMap<>();

        for (IModule module : proc.getSoftwareSystem().getModules().values())
        {
            IModuleInfoProcessor modInfo = proc.createModuleInfoProcessor(module);
            IMetricLevel moduleLevel = modInfo.getMetricLevel(IMetricLevel.MODULE).get();
            Map<String, Object> moduleMetrics = new HashMap<>();

            for (IMetricId metricId : modInfo.getMetricIdsForLevel(moduleLevel))
            {
                for (Map.Entry<INamedElement, IMetricValue> entry : modInfo.getMetricValues(IMetricLevel.MODULE, metricId.getName()).entrySet())
                {
                    IMetricValue val = entry.getValue();

                    moduleMetrics.put(val.getId().getProvider().getName() + ":" + val.getId().getName(), val.getValue());
                }
            }
            moduleMetrics.put("moduleName", module.getName());
            moduleMetrics.put("moduleId", module.getModuleId());
            moduleMetrics.put("language", module.getLanguage());
            modules.put(module.getName(), moduleMetrics);
        }
        systemMetrics.put("modules", modules);
        systemMetrics.put("systemId", proc.getSoftwareSystem().getSystemId());
        systemMetrics.put("systemName", proc.getSoftwareSystem().getName());
        systemMetrics.put("timestamp", new Date(proc.getSoftwareSystem().getTimestamp()));

        Set<String> languages = new HashSet<>();

        for (IModule module : proc.getSoftwareSystem().getModules().values())
        {
            languages.add(module.getLanguage());
        }

        String[] languagesArray = languages.toArray(new String[languages.size()]);

        systemMetrics.put("languages", String.join(",", languagesArray));
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
