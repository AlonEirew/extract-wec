SELECT count(DISTINCT(mentionText)) from Mentions where coreChainId=159029;
SELECT * from Mentions where coreChainId=35570 GROUP BY(LOWER(mentionText));
SELECT DISTINCT(mentionText) from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId where corefType=1 and mentionsCount > 50;
Select count(distinct(lower(mentionText))) from Mentions;
SELECT coreChainId, mentionText, COUNT(*) FROM Mentions GROUP BY coreChainId, mentionText;
Select coreChainId, mentionText, count(*) from (
	SELECT coreChainId, mentionText, count(*) from (
		SELECT coreChainId, mentionText from Mentions INNER JOIN CorefChains ON 
		Mentions.coreChainId=CorefChains.corefId where corefType>=2 and corefType<=8) 
	GROUP BY coreChainId, mentionText) 
GROUP BY mentionText HAVING COUNT(*) > 1 ORDER BY count(*) DESC;
SELECT * from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId;
SELECT * from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId where corefId=480203 and PartOfSpeech like "%VB%";
SELECT * from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId where corefType>=2 and corefType<=8;
SELECT * from Mentions where coreChainId=86429;
SELECT * from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId where corefType>=2 and corefType<=8 and corefValue="Dawson's Field hijackings";
select * from CorefChains where corefValue="Israeli legislative election, 1949";

select * from CorefChains WHERE corefType=8;

select * from CorefChains WHERE corefType=2;
SELECT * FROM Mentions WHERE PartOfSpeech like '%VB%';

SELECT * from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId where corefType in (2,3,4,6,7,8) and PartOfSpeech like '%VB%';

select count(*) from Validation WHERE split="VALIDATION";
select count(*) from Validation WHERE split="TRAIN";
select count(*) from Validation WHERE split="TEST";
select count(*) from Validation;
select * from Validation where split="VALIDATION" group by Validation.coreChainId;
select * from Validation where split="TEST" group by Validation.coreChainId;
select * from Validation where split="TRAIN" group by Validation.coreChainId;

SELECT count(*) from Validation3 INNER JOIN CorefChains ON Validation3.coreChainId=CorefChains.corefId where corefType=6;
SELECT * from Validation3 INNER JOIN CorefChains ON Validation3.coreChainId=CorefChains.corefId where corefType=8 and split="TRAIN" GROUP BY coreChainId LIMIT 3;
SELECT * FROM Validation limit 100;
Select * from Validation where coreChainId=1088285
select count(*) from Validation WHERE split="TRAIN" GROUP BY Validation.coreChainId HAVING count(*)=1;

Select * from Validation3 INNER JOIN CorefChains ON Validation3.coreChainId=CorefChains.corefId where split='TRAIN' and corefType=8;

Select * from Validation Where coreChainId=383260;
Select * from Validation4 where mentionId=35369707;
Select * from Validation INNER JOIN CorefChains ON Validation.coreChainId=CorefChains.corefId where corefType=8;

Select * from CorefChains where corefValue = "Anund Jacob";


-- Delete from Validation WHERE mentionId=41982340;
-- ALTER TABLE Validation RENAME TO Validation3;
