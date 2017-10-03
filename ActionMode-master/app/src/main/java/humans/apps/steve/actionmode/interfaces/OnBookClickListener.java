package humans.apps.steve.actionmode.interfaces;

import android.view.View;

import humans.apps.steve.actionmode.entities.Book;

/**
 * Created by Steve on 3/08/2016.
 */

public interface OnBookClickListener {
    void onBookClickListener(Book book, View view, int typeClick);
}
