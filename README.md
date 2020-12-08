## Extract WEC Dataset
Extract mentions from Wikipedia links to create a cross document and within document coref database

Building the project:
`#>./gradlew clean build -x test`

Running extract-wec:
`#>./bin/extract-wec`

CREATE DATABASE TestDB

```
{
  "poolSize": "8",
  "elasticHost": "localhost",
  "elasticPort": 9200,
  "elasticWikiIndex": "enwiki_v3",
  "elasticWikinewsIndex": "wikinews_v1",
  "infoboxConfiguration": "/infobox_config/en_infobox_config.json",
  "multiRequestInterval" : 100,
  "elasticSearchInterval" : 100,
  "totalAmountToExtract" : 10000,
  "sqlConnectionUrl": "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksExperiment.db"
}
```

Running events:
`java -Xmx90000m -DentityExpansionLimit=2147480000 -DtotalEntitySizeLimit=2147480000 -Djdk.xml.totalEntitySizeLimit=2147480000 -cp "lib/*" experimentscripts.event.ReadFilteredJsonAndProcess`
