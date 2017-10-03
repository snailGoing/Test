package humans.apps.steve.actionmode.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import humans.apps.steve.actionmode.R;
import humans.apps.steve.actionmode.entities.Book;
import humans.apps.steve.actionmode.entities.SparseBooleanArrayParcelable;
import humans.apps.steve.actionmode.holders.BooksViewHolder;
import humans.apps.steve.actionmode.interfaces.OnBookClickListener;

public class BooksAdapter extends
        RecyclerView.Adapter<BooksViewHolder> {

    private static final String TAG = BooksAdapter.class.getSimpleName();
    private ArrayList<Book> arrayList;
    private Context context;
    private SparseBooleanArrayParcelable mSelectedItemsIds;
    private OnBookClickListener listener;

    public BooksAdapter(Context context, ArrayList<Book> arrayList, OnBookClickListener listener) {
        this.context = context;
        this.arrayList = arrayList;
        mSelectedItemsIds = new SparseBooleanArrayParcelable();
        this.listener = listener;
    }

    @Override
    public BooksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater mInflater = LayoutInflater.from(parent.getContext());

        ViewGroup mainGroup = (ViewGroup) mInflater.inflate(
                R.layout.item_books, parent, false);
        return new BooksViewHolder(mainGroup);
    }

    @Override
    public void onBindViewHolder(BooksViewHolder holder, int position) {
        //Setting text over text view
        final Book book = arrayList.get(position);
        holder.title.setText(book.getTitle());
        holder.author.setText(book.getAuthor());

        int colorAccent = ContextCompat.getColor(context, R.color.gainsboro);
        /** Change background color of the selected items  **/
        holder.itemView
                .setBackgroundColor(mSelectedItemsIds.get(position, false) ? colorAccent
                        : Color.TRANSPARENT);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "holder.itemView.setOnClickListener");
                listener.onBookClickListener(book, view, 0);
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                listener.onBookClickListener(book, view, 1);
                return true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return (null != arrayList ? arrayList.size() : 0);
    }

    /***
     * Methods required for do selections, remove selections, etc.
     */

    //Toggle selection methods
    public void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }


    //Remove selected selections
    public void removeSelection(boolean isActionModeMenuClicked) {
        Log.d(TAG, "REMOVE SELECTION");
        mSelectedItemsIds = new SparseBooleanArrayParcelable();
        if (!isActionModeMenuClicked){
            notifyDataSetChanged();
        }
    }


    //Put or delete selected position into SparseBooleanArray
        private void selectView(int position, boolean value) {
            if (value)
                mSelectedItemsIds.put(position, true);
            else
                mSelectedItemsIds.delete(position);

        notifyItemChanged(position);
    }

    //Get total selected count
    public int getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    //Return all selected ids
    public SparseBooleanArrayParcelable getSelectedIds() {
        return mSelectedItemsIds;
    }
}
