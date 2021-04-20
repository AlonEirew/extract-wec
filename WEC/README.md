# WEC-Eng: A cross-document event coreference dataset derived from English Wikipedia
We provide the following data sets under a <a href="https://creativecommons.org/licenses/by-sa/3.0/deed.en_US">Creative Commons Attribution-ShareAlike 3.0 Unported License</a>. It is based on content extracted from Wikipedia that is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License
## Dataset's Files
### `WEC-Eng.zip` 
**Final version of the English CD event coreference dataset**<br>
Including 3 files: Train_Event_gold_mentions.json, Test_Event_gold_mentions_validated.json, Dev_Event_gold_mentions_validated.json

### `WEC-Eng-Unfiltered.zip`
**The non (within clusters) controlled version of the dataset (lexical diversity)**<br>
Include 1 file: All_Event_gold_mentions_unfiltered.json

## Json mention example
```json
{
        "coref_chain": 2293469,
        "coref_link": "Family Values Tour 1998",
        "doc_id": "House of Pain",
        "mention_context": [
            "From",
            "then",
            "on",
            ",",
            "the",
            "members",
            "continued",
            "their"
  ],
  "mention_head": "Tour",
  "mention_head_lemma": "Tour",
  "mention_head_pos": "PROPN",
  "mention_id": "108172",
  "mention_index": 1,
  "mention_ner": "UNK",
  "mention_type": 8,
  "predicted_coref_chain": null,
  "sent_id": 2,
  "tokens_number": [
    50,
    51,
    52,
    53
  ],
  "tokens_str": "Family Values Tour 1998",
  "topic_id": -1
}
```
## Json mention fields
|Field|Value Type|Value|
|---|:---:|---|
|coref_chain|Numeric|Coreference chain/cluster ID|
|coref_link|String|Coreference link wikipeida page/article title|
|doc_id|String|Mention page/article title|
|mention_context|List[String]|Tokenized mention paragraph (including mention)|
|mention_head|String|Mention span head token|
|mention_head_lemma|String|Mention span head token lemma|
|mention_head_pos|String|Mention span head token POS|
|mention_id|String|Mention id|
|mention_index|Numeric|Mention index in json file|
|mention_ner|String|Mention NER|
|tokens_number|List[Numeric]|Mentions tokens ids within the context|
|tokens_str|String|Mention span text|
|topic_id|Ignore|Ignore|
|mention_type|Ignore|Ignore|
|predicted_coref_chain|Ignore|Ignore|
|sent_id|Ignore|Ignore|
