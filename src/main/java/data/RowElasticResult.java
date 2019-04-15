package data;

public class RowElasticResult {

    private String id;
    private String title;
    private String text;

    public RowElasticResult(String id, String title, String text) {
        this.id = id;
        this.title = title;
        this.text = text;
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
