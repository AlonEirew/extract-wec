package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SportInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.HALFTIME_SHOW, CorefSubType.MATCH, CorefSubType.DRAFT,
            CorefSubType.RACE, CorefSubType.CHAMPIONSHIPS, CorefSubType.ATHLETICS, CorefSubType.SAILING,
            CorefSubType.PARALYMPIC, CorefSubType.TENNIS, CorefSubType.SWIMMING, CorefSubType.FOOTBALL,
            CorefSubType.BASKETBALL, CorefSubType.WRESTLING, CorefSubType.HOCKEY, CorefSubType.FRC,
            CorefSubType.SWC, CorefSubType.OLYMPIC, CorefSubType.COMPETITION, CorefSubType.TOURNAMENT,
            CorefSubType.GAMES, CorefSubType.SPORT_EVENT, CorefSubType.CUP_FINAL, CorefSubType.BASEBALL,
            CorefSubType.SINGLE_GAME, CorefSubType.YEARLY_GAME, CorefSubType.OUTDOOR_GAME};


    private static final Pattern SPORT_PATTERN = Pattern.compile(
            "\\{\\{infobox[\\w\\|]*?(halftimeshow|match|draft|racereport|indy500|championships|athleticscompetition|sailingcompetition" +
                    "|competitionevent|paralympicevent|tennisevent|grandslamevent|tournamentevent|swimmingevent|all-stargame|grandprixevent|wrestlingevent" +
                    "|cupfinal|ncaabasketballsinglegame|baseballgame|nflgame|nflchamp|aflchamp|basketballgame|race|sportevent" +
                    "|singlegame|hockeygame|yearlygame|outdoorgame|frcgame|olympicevent|mmaevent|daytona500|gamesevent|swcevent)");

    public SportInfoboxExtractor() {
        super(subTypes, CorefType.SPORT_EVENT);
    }

    @Override
    protected CorefSubType extract(String infobox, String title) {
        Matcher linkMatcher = SPORT_PATTERN.matcher(infobox);
        boolean titleMatch = titleNumberMatch(title);
        if (linkMatcher.find() && titleMatch) {
            if(linkMatcher.group(1).contains("halftimeshow")) {
                return CorefSubType.HALFTIME_SHOW;
            } else if(linkMatcher.group(1).contains("match")) {
                return CorefSubType.MATCH;
            } else if(linkMatcher.group(1).contains("draft")) {
                return CorefSubType.DRAFT;
            } else if(linkMatcher.group(1).contains("racereport") || linkMatcher.group(1).contains("indy500") ||
                    linkMatcher.group(1).contains("race") || linkMatcher.group(1).contains("daytona500") ||
                    linkMatcher.group(1).contains("grandprixevent")) {
                return CorefSubType.RACE;
            } else if(linkMatcher.group(1).contains("championships")) {
                return CorefSubType.CHAMPIONSHIPS;
            } else if(linkMatcher.group(1).contains("athleticscompetition")) {
                return CorefSubType.ATHLETICS;
            } else if(linkMatcher.group(1).contains("sailingcompetition")) {
                return CorefSubType.SAILING;
            } else if(linkMatcher.group(1).contains("paralympicevent")) {
                return CorefSubType.PARALYMPIC;
            } else if(linkMatcher.group(1).contains("tennisevent") || linkMatcher.group(1).contains("grandslamevent")) {
                return CorefSubType.TENNIS;
            } else if(linkMatcher.group(1).contains("swimmingevent")) {
                return CorefSubType.SWIMMING;
            } else if(linkMatcher.group(1).contains("nflchamp") || linkMatcher.group(1).contains("aflchamp") ||
                    linkMatcher.group(1).contains("nflgame")) {
                return CorefSubType.FOOTBALL;
            } else if(linkMatcher.group(1).contains("ncaabasketballsinglegame") || linkMatcher.group(1).contains("basketballgame") ||
                    linkMatcher.group(1).contains("all-stargame")) {
                return CorefSubType.BASKETBALL;
            } else if(linkMatcher.group(1).contains("wrestlingevent") || linkMatcher.group(1).contains("mmaevent")) {
                return CorefSubType.WRESTLING;
            } else if(linkMatcher.group(1).contains("hockeygame")) {
                return CorefSubType.HOCKEY;
            } else if(linkMatcher.group(1).contains("frcgame")) {
                return CorefSubType.FRC;
            } else if(linkMatcher.group(1).contains("swcevent")) {
                return CorefSubType.SWC;
            } else if(linkMatcher.group(1).contains("olympicevent")) {
                return CorefSubType.OLYMPIC;
            } else if(linkMatcher.group(1).contains("competitionevent")) {
                return CorefSubType.COMPETITION;
            } else if(linkMatcher.group(1).contains("tournamentevent")) {
                return CorefSubType.TOURNAMENT;
            } else if(linkMatcher.group(1).contains("gamesevent")) {
                return CorefSubType.GAMES;
            } else if(linkMatcher.group(1).contains("sportevent")) {
                return CorefSubType.SPORT_EVENT;
            } else if(linkMatcher.group(1).contains("cupfinal")) {
                return CorefSubType.CUP_FINAL;
            } else if(linkMatcher.group(1).contains("baseballgame")) {
                return CorefSubType.BASEBALL;
            } else if(linkMatcher.group(1).contains("singlegame")) {
                return CorefSubType.SINGLE_GAME;
            } else if(linkMatcher.group(1).contains("yearlygame")) {
                return CorefSubType.YEARLY_GAME;
            } else if(linkMatcher.group(1).contains("outdoorgame")) {
                return CorefSubType.OUTDOOR_GAME;
            }
        }

        return CorefSubType.NA;
    }
}
