[![CI](https://github.com/sonargraph/metric-extractor/actions/workflows/build.yml/badge.svg)](https://github.com/sonargraph/metric-extractor/actions/workflows/build.yml)
![badge](https://img.shields.io/endpoint?url=https://enterprise.hello2morrow.com/badges/MetricExtractor/Core:CoreMaintainabilityLevel)
![badge](https://img.shields.io/endpoint?url=https://enterprise.hello2morrow.com/badges/MetricExtractor/Core:CoreLinesOfCode)
# Sonargraph System Metric Extractor

This utility program takes a [Sonargraph](https://www.hello2morrow.com/products/sonargraph) XML report and extracts all
system level metrics into a JSON file (a map).

You run it by passing the XML input file as a command lines argument. The result
is a JSON file containing a map that can either be stored in a file or uploaded
to an instance of [Sonargraph-Enterprise](https://www.hello2morrow.com/products/sonargraph/enterprise). 
It has one value for every Sonargraph system metric (more than 100 different metrics). Besides
one entry for each metric it also contains 3 special keys `systemId`, `systemName` and
`timestamp`. `systemId` is MD5 hash that could be used as
a unique global identifier for the system. Moreover, the map contains another map
named `modules` that contains the metrics for each of the modules of your system.

The following command line arguments can be used:

- `-host=<URL of Sonargraph-Enterprise>`: use this if you want to upload the result to an instance of Sonargraph-Enterprise. If you don't kind sharing metrics you can use "https://enterprise.hello2morrow.com". If this is not set a JSON file with the same name and location as the input XML file will be created. 
- `-org=<name of your org>`: name of your organization (optional).
- `-branch=<branch name>`: name of the branch this report is based on (optional).
- `-useId`: use `systemId` and not `systemName` as identifier for your system. That only makes sense if you have a predefined Sonargraph system (optional). 

After the switches you pass the name of the XML report containing the metric data.

Here is an example how to call it:
```
java -jar MetricExtractor-1.3-jar-with-dependencies.jar -host=https://enterprise.hello2morrow.com <name of XML input file>
```
That will extract the metrics from the XML report and upload it to the [default instance](https://enterprise.hello2morrow.com) of Sonargraph-Enterprise. 
