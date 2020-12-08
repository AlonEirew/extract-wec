package experimentscripts.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import data.BaseMention;
import data.EventSubEventPair;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GenMoreEventsStats {
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String INPUT_JSON = "output/sub_events_sharing_context.json";

    public static void main(String[] args) throws Exception {
        Type listType = new TypeToken<ArrayList<EventSubEventPair>>(){}.getType();
        List<EventSubEventPair> eventPairs = GSON.fromJson(new FileReader(INPUT_JSON), listType);
        System.out.println();
    }
}
