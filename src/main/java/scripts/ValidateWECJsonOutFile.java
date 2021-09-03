package scripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import wec.data.MentionPojo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class ValidateWECJsonOutFile {

    private static final boolean FIX = false;

    public static void main(String[] args) throws IOException {
        Type mentionType = new TypeToken<List<MentionPojo>>() {}.getType();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String inputMentFile = "WEC/All_Event_gold_mentions_unfiltered.json_fix";
        List<MentionPojo> mentions = gson.fromJson(new JsonReader(new FileReader(inputMentFile)), mentionType);
        int faultyMents = 0;
        for (MentionPojo ment : mentions) {
            long mention_id = ment.getMention_id();
            List<String> context = ment.getMention_context();
            String mentionStr = ment.getTokens_str();
            List<Integer> tokIds = ment.getTokens_number();
            List<String> mentStrSplit = Arrays.asList(mentionStr.split(" "));

            int mentStart = tokIds.get(0);
            int mentEnd = tokIds.get(tokIds.size() -1) + 1;
            List<String> contextMentStr = context.subList(mentStart, mentEnd);

            if (tokIds.size() != mentStrSplit.size() || !mentStrSplit.equals(contextMentStr)) {
                System.out.println("MentionId: " + mention_id + " is faulty:");
                System.out.println("Context Ment Str:   " + String.join(" ", contextMentStr));
                System.out.println("Mention Str:        " + mentionStr);
                System.out.println("--------------------------------------------------------------");
                faultyMents++;

                String joinContMent = String.join("", contextMentStr);
                String joinMent = String.join("", mentStrSplit);
                if(FIX && joinMent.equals(joinContMent)) {
                    ment.setTokens_str(String.join(" ",contextMentStr));
                }
            }
        }

        System.out.println("Done checking all mentions! FaultyMents=" + faultyMents);

        if(FIX) {
            String json = gson.toJson(mentions);
            Files.writeString(new File(inputMentFile + "_fix").toPath(), json, StandardCharsets.UTF_8);
            System.out.println("File fixed");
        }
    }
}
