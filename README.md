# Sonargraph System Metric Extractor

This utility program takes a [Sonargraph](https://www.hello2morrow.com/products/sonargraph) XML report and extracts all
system level metrics into a JSON file (a map).

You run it by passing the XML input file as a command lines argument. The result
is a JSON file containing a map with the same name and location as the XML file. It has
one value for every Sonargraph system metric (more than 100 different metrics). Besides
one entry for each metric it also contains 3 special keys `systemId`, `systemName` and
`timestamp`. `systemId` is MD5 hash that could be used as
a unique global identifier for the system.

Here is how you call it:
```
java -jar MetricExtractor-1.0.jar <name of XML input file>
```