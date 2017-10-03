package humans.apps.steve.actionmode.fragments;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

import humans.apps.steve.actionmode.R;
import humans.apps.steve.actionmode.adapters.BanViewPager;
import humans.apps.steve.actionmode.adapters.BooksAdapter;
import humans.apps.steve.actionmode.entities.Book;
import humans.apps.steve.actionmode.interfaces.OnBookClickListener;
import humans.apps.steve.actionmode.entities.SparseBooleanArrayParcelable;

public class BooksFragment extends Fragment implements OnBookClickListener, ActionMode.Callback {

    private static final String SELECTED_IDS = "SELECTED_IDS";
    private static final String TAG = BooksFragment.class.getSimpleName();
    private View view;
    private RecyclerView recyclerView;
    private ArrayList<Book> item_books;
    private BooksAdapter adapter;
    private ActionMode mActionMode;
    private SparseBooleanArrayParcelable mSelectedIds;
    private boolean isActionModeMenuClicked;
    private TabLayout tabLayout;
    private Context mContext;


    public BooksFragment() {
        Log.d(TAG, "empty public constructor");
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            mSelectedIds = savedInstanceState.getParcelable(SELECTED_IDS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_books, container, false);
        Log.d(TAG, "onCreateView");
        populateRecyclerView();
        //implementRecyclerViewClickListeners();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        Log.d(TAG, "SIZE SAVED: " + adapter.getSelectedIds().size());
        //outState.putParcelable(SELECTED_IDS, adapter.getSelectedIds());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    //Populate ListView with dummy data
    private void populateRecyclerView() {
        Log.d(TAG, "populateRecyclerView");
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        item_books = new ArrayList<>();
        for (int i = 1; i <= 40; i++)
            item_books.add(new Book("Title " + i, "Author " + i));

        adapter = new BooksAdapter(getActivity(), item_books, this);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onBookClickListener(Book book, View view, int typeClick) {

        int position = recyclerView.getChildAdapterPosition(view);
        switch (typeClick){
            //ON CLICK
            case 0:
                //If ActionMode not null select item
                if (mActionMode != null)
                    onListItemSelect(position);
                break;
            // ON LONG CLICK
            case 1:
                //Select item on long click
                onListItemSelect(position);
                break;
        }
    }


    //List item select method
    private void onListItemSelect(int position) {
        adapter.toggleSelection(position);//Toggle the selection
        setActionMode(adapter.getSelectedCount());
    }

    private void setActionMode(int countSelected) {
        boolean hasCheckedItems = countSelected > 0;//Check if any items are already selected or not

        if (hasCheckedItems && mActionMode == null)
            // there are some selected items, start the actionMode
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);

        else if (!hasCheckedItems && mActionMode != null)
            // there no selected items, finish the actionMode
            mActionMode.finish();
        if (mActionMode != null)
            //set action mode title on item selection
            mActionMode.setTitle(String.valueOf(countSelected) + " selected");
    }

    //Set action mode null after use
    public void setNullToActionMode() {
        if (mActionMode != null)
            mActionMode = null;
    }

    //Delete selected rows
    public void deleteBooks() {

        SparseBooleanArray selected =
                adapter.getSelectedIds();//Get selected ids

        //Loop all selected ids
        for (int i = (selected.size() - 1); i >= 0; i--) {
            if (selected.valueAt(i)) {
                //If current id is selected remove the item via key
                int position = selected.keyAt(i);
                item_books.remove(position);
                adapter.notifyItemRemoved(position);//notify adapter
            }
        }
        Snackbar.make(recyclerView, selected.size() + " Items Deleted", Snackbar.LENGTH_SHORT).show();//Show Toast
        if (mActionMode != null)
            mActionMode.finish();//Finish action mode after use
    }

    //Delete selected rows
    public void loveBooks() {

        final SparseBooleanArray selected =
                adapter.getSelectedIds();//Get selected ids

        Snackbar.make(recyclerView, selected.size() + " Items Loved", Snackbar.LENGTH_SHORT).show();//Show
        //Loop all selected ids
        for (int i = (selected.size() - 1); i >= 0; i--) {
            if (selected.valueAt(i)) {
                //If current id is selected remove the item via key
                int position = selected.keyAt(i);
                adapter.toggleSelection(position);
                adapter.notifyItemChanged(position);//notify adapter
            }
        }
        if (mActionMode != null)
            mActionMode.finish();//Finish action mode after use
    }

    //Delete selected rows
    public void copyBooks() {
        final SparseBooleanArray selected =
                adapter.getSelectedIds();//Get selected ids

        Snackbar.make(recyclerView, selected.size() + " Items Copied.", Snackbar.LENGTH_SHORT).show();//Show Toast
        //Loop all selected ids
        for (int i = (selected.size() - 1); i >= 0; i--) {
            if (selected.valueAt(i)) {
                //If current id is selected remove the item via key
                int position = selected.keyAt(i);
                //item_books.remove(position);
                adapter.toggleSelection(position);
                adapter.notifyItemChanged(position);//notify adapter
            }
        }
        if (mActionMode != null)
            mActionMode.finish();//Finish action mode after use
        else {
            mActionMode = null;
        }
    }



    public SparseBooleanArray getSelectedIds() {
        return adapter.getSelectedIds();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        Log.d(TAG, "onCreateActionMode");
        isActionModeMenuClicked = false;

        if(tabLayout!=null) {
            tabLayout.setVisibility(View.GONE);

            Log.d(TAG, "onCreateActionMode  tabLayout gone");
        }else{
            Log.d(TAG, "onCreateActionMode tabLayout null");
        }
        if(viewPager!=null) viewPager.setNoScroll(true);
        mode.getMenuInflater().inflate(R.menu.menu_mode, menu);//Inflate the menu over action mode

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Log.d(TAG, "onPrepareActionMode");
        //Sometimes the meu will not be visible so for that we need to set their visibility manually in this method
        //So here show action menu according to SDK Levels
        if (Build.VERSION.SDK_INT < 11) {
            MenuItemCompat.setShowAsAction(menu.findItem(R.id.action_love), MenuItemCompat.SHOW_AS_ACTION_NEVER);
            MenuItemCompat.setShowAsAction(menu.findItem(R.id.action_delete), MenuItemCompat.SHOW_AS_ACTION_NEVER);
            MenuItemCompat.setShowAsAction(menu.findItem(R.id.action_copy), MenuItemCompat.SHOW_AS_ACTION_NEVER);
        } else {
            menu.findItem(R.id.action_love).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        Log.d(TAG, "onActionItemClicked");
        Log.d(TAG, "item.getItemId(): " + item.getItemId());
        Log.d(TAG, "item.getTitle(): " + item.getTitle());


        isActionModeMenuClicked = true;
        switch (item.getItemId()) {
            case R.id.action_delete:
                Log.d(TAG, "R.id.action_delete: " + item.getItemId());
                deleteBooks();
                break;
            case R.id.action_copy:
                Log.d(TAG, "R.id.action_copy: " + item.getItemId());
                copyBooks();
                break;
            case R.id.action_love:
                Log.d(TAG, "R.id.action_love: " + item.getItemId());
                loveBooks();
                break;
            default:
                Log.d(TAG, "default: "+ item.getItemId());
                break;
        }
        return true;
    }


    @Override
    public void onDestroyActionMode(ActionMode mode) {

        //When action mode destroyed remove selected selections and set action mode to null
        //First check current fragment action mode
        Log.d(TAG, "onDestroyActionMode");
        adapter.removeSelection(isActionModeMenuClicked);
        setNullToActionMode();
        if(tabLayout!=null) tabLayout.setVisibility(View.VISIBLE);
        if(viewPager!=null) viewPager.setNoScroll(false);

    }
    public BooksFragment setTabLayout(TabLayout mTabLayout){
        Log.d(TAG, "setTabLayout");
        this.tabLayout = mTabLayout;
        return this;
    }

    private BanViewPager viewPager;
    public BooksFragment setViewPager(BanViewPager mViewPager){
        Log.d(TAG, "setTabLayout");
        this.viewPager = mViewPager;
        return this;
    }
}
