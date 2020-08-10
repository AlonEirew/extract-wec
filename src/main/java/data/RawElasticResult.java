package data;

public class RawElasticResult {

    private String id = "-1";
    private final String title;
    private final String text;
    private final String infobox;

    public RawElasticResult(String title, String text, String infobox) {
        this.title = title;
        this.text = text;
        this.infobox = infobox;
    }

    public RawElasticResult(String id, String title, String text, String infobox) {
        this(title, text, infobox);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getInfobox() {
        return infobox;
    }
}
