package humans.apps.steve.actionmode.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import humans.apps.steve.actionmode.R;

/**
 * Created by Steve on 2/08/2016.
 */

public class BooksViewHolder extends RecyclerView.ViewHolder {
    public TextView title, author;


    public BooksViewHolder(View view) {
        super(view);

        this.title = (TextView) view.findViewById(R.id.title);
        this.author = (TextView) view.findViewById(R.id.author);

    }
}
