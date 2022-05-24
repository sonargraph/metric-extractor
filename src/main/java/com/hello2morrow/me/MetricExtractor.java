package com.hello2morrow.me;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

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
        String host = null;
        String org = null;
        String branch = null;
        String fileName = null;
        boolean useId = false;

        for (String arg : args)
        {
            if (arg.startsWith("-host="))
            {
                host = arg.substring(6);
            }
            else if (arg.startsWith("-org="))
            {
                org = arg.substring(5);
            }
            else if (arg.startsWith("-branch="))
            {
                branch = arg.substring(8);
            }
            else if (arg.equals("-useId"))
            {
                useId = true;
            }
            else if (fileName == null && !arg.startsWith("-"))
            {
                fileName = arg;
            }
            else
            {
                System.err.println("Invalid parameter: " + arg);
                System.exit(1);
            }
        }
        if (fileName != null)
        {
            me.process(fileName, host, org, branch, useId);
        }
        else
        {
            System.err.println("Missing file parameter for Sonargraph XML report");
            System.exit(1);
        }
    }

    @NotNull Map<String, Object> extractMetrics(@NotNull String xmlFileName)
    {
        final ISonargraphSystemController controller = ControllerFactory.createController();
        final Result result = controller.loadSystemReport(new File(xmlFileName));

        if (!result.isSuccess())
        {
            System.err.println(result.toString());
            System.exit(1);
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

    String process(@NotNull String xmlFileName, String host, String org, String branch, boolean useId)
    {
        Map<String, Object> systemMetrics = extractMetrics(xmlFileName);
        ObjectMapper objectMapper = new ObjectMapper();

        try
        {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(systemMetrics);

            if (host == null)
            {
                String jsonFileName = xmlFileName.substring(0, xmlFileName.length()-3) + "json";

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFileName)))
                {
                    writer.write(json);
                }
                catch (IOException e)
                {
                    System.err.println(e.getMessage());
                    System.exit(1);
                }
                return jsonFileName;
            }
            else
            {
                uploadJsonData(json, host, org, branch, useId);
            }
        }
        catch (JsonProcessingException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return null;
    }

    private void uploadJsonData(@NotNull String jsonData, @NotNull String host, String org, String branch, boolean useId)
    {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofMinutes(2))
                .build();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(host + "/upload/sonargraphJsonReport"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData));

        if (org != null && org.length() > 0)
        {
            builder.header("X-Organization", org);
        }
        if (branch != null && branch.length() > 0)
        {
            builder.header("X-SonargraphBranch", branch);
        }
        if (useId)
        {
            builder.header("X-UseSonargraphId", "true");
        }
        try
        {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200)
            {
                System.err.println(String.format("Request failed with status %d: %s", response.statusCode(), response.body()));
                System.exit(1);
            }
        }
        catch (Exception e)
        {
            System.err.println("Cannot send request: " + e.getMessage());
            System.exit(1);
        }
    }
}
