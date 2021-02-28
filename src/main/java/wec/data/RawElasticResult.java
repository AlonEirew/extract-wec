package wec.data;

public class RawElasticResult {

    private String id = "-1";
    private final String title;
    private final String text;
    private final String infobox;
    private String redirect;

    public RawElasticResult(String title, String text, String infobox) {
        this.title = title;
        this.text = text;
        this.infobox = infobox;
    }

    public RawElasticResult(String id, String title, String text, String infobox, String redirect) {
        this(title, text, infobox);
        this.id = id;
        this.redirect = redirect;
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

    public String getRedirect() {
        return redirect;
    }
}
