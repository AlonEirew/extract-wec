package data;

public class RawElasticResult {

    private String id = "-1";
    private final String title;
    private final String text;

    public RawElasticResult(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public RawElasticResult(String id, String title, String text) {
        this(title, text);
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
}
