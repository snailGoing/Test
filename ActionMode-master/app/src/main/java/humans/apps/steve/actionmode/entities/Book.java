package humans.apps.steve.actionmode.entities;

/**
 * Created by Steve on 2/08/2016.
 */

public class Book {
    private String title;
    private String author;

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public Book() {
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
