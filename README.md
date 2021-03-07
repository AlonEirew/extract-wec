# Extract WEC Dataset
This project is following the research paper: (TBD - ADD PAPER LINK).<br/>
Here can be found both The WEC-Eng cross document dataset from English Wikipedia and the method for creating WEC for other languages. <br/>

### WEC-Eng
Datset location is at `WEC` folder

## Generating WEC
### Requisites
* A Wikipedia ElasticSearch Index created by <a href="https://github.com/AlonEirew/wikipedia-to-elastic">wikipedia-to-elastic</a> project (with infobox relationType).
* Java 11

In order to WEC in current supported languages (e.g., English, French, Spanish, German, Chinese), follow the steps needed in *wikipedia-to-elastic* and below in .

### Processes
This code repo contains two main processes:
1) Code to generate the initial crude version of WEC-Lang
2) Code to generate the final Json of WEC-Lang

### WEC to DB Configuration:
Configuration file - `resources/application.properties`
```
spring.datasource.url=jdbc:h2:file:/demo => h2 database file url
poolSize=8 => Number of thread to run
elasticHost=localhost => Elastic engine host
elasticPort=9200 => (Elastic engine port)
elasticWikiIndex=enwiki_v3 => (Elastic index to read from (as generated by *wikipedia-to-elastic*)
infoboxConfiguration=/infobox_config/en_infobox_config.json => Explained below
multiRequestInterval=100 (recommended value) => Control the number of search pages to retrive from elastic
elasticSearchInterval=100 (recommended value) => Control the number of pages to read by the elasitc scroller
totalAmountToExtract=-1 => if < 0 then read all wikipedia pages, otherwise will read upto the amount specified
```

### WEC to Json Configuration:
```
main.outputDir=output => the output folder where the WEC json should be created 
main.outputFile=GenWEC.json => the output file name of WEC json file
```

### Language Adaptation
We have extracted the relevant infobox configuration for the English Wikipedia. <br/> 
In order to create a newer version of WEC-Eng, use/update the default `infobox_config/en_infobox_config.json` in configuration. <br/>

To generate WEC in one of the supported languages (other than English) follow those steps:
* Export Wikipedia in the required language using *wikipedia-to-elastic* project
* Explore for infoboxs categories, and their correlating names in the required language
* In order to see candidate as well as investigate the amount of pages related to an infobox category. Run:<br/>
`#>java -Xmx90000m -DentityExpansionLimit=2147480000 -DtotalEntitySizeLimit=2147480000 -Djdk.xml.totalEntitySizeLimit=2147480000 -cp "lib/*" scripts.ExtractInfoboxs`<br/>
Report will be generated in `output/InfoboxCount.txt` 
  
#### Infobox configuration file - `resources/infobox_config/*` <br/>
A new file will need to be created and replace in with current in `config.json`<br/>

#### English infobox example (from - `en_infobox_config.json`)
```json
{
  "infoboxLangText" : "Infobox",
  "infoboxConfigs": [
    {
      "corefType": "ACCIDENT_EVENT",
      "include": true,
      "infoboxs": [
        "airlinerincident",
        "airlineraccident",
        "aircraftcrash",
        "aircraftaccident",
        "aircraftincident",
        "aircraftoccurrence",
        "railaccident",
        "busaccident",
        "publictransitaccident"
      ]
    }
  ]
}
```

### Project Output
* By default a H2 dataset containing the crude extraction of coreference relations from Wikipedia (this resource can be used for experiments when generating the final version of WEC-Lang)
* A JSON format resource of the WEC-Lang dataset


### Extracting WEC-Lang
Make sure the Wikipedia Elastic engine is running <br/>
* Running WikiToWECMain in order to generate the H2 dataset:<br/>
  `#>./gradlew bootRun --args=wecdb`
* Generate the WEC-Lang Json format file:<br/> 
  `#>./gradlew bootRun --args=wecjson`

[comment]: <> (Running events:)
[comment]: <> (`java -Xmx90000m -DentityExpansionLimit=2147480000 -DtotalEntitySizeLimit=2147480000 -Djdk.xml.totalEntitySizeLimit=2147480000 -cp "lib/*" scripts.experiments.event.ReadFilteredJsonAndProcess`)
