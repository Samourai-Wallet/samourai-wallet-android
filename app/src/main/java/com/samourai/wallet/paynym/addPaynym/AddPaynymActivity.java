package com.samourai.wallet.paynym.addPaynym;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.samourai.wallet.R;
import com.samourai.wallet.paynym.fragments.PaynymListFragment;

public class AddPaynymActivity extends AppCompatActivity {

    private SearchView mSearchView;
    private ViewSwitcher viewSwitcher;
    private RecyclerView searchPaynymList;
    private static final String TAG = "AddPaynymActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paynym);
        setSupportActionBar(findViewById(R.id.toolbar_addpaynym));
        viewSwitcher = findViewById(R.id.viewswitcher_addpaynym);
        searchPaynymList = findViewById(R.id.search_list_addpaynym);
        searchPaynymList.setLayoutManager(new LinearLayoutManager(this));
        searchPaynymList.setAdapter(new SearchAdapter());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_paynym_menu, menu);
        mSearchView = (SearchView) menu.findItem(R.id.action_search_addpaynym).getActionView();

        menu.findItem(R.id.action_search_addpaynym).setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                viewSwitcher.showNext();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                viewSwitcher.showNext();
                return true;
            }
        });

        setUpSearch();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    private void setUpSearch() {

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i(TAG, "onQueryTextSubmit: ".concat(query));

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i(TAG, "onQueryTextChange: ".concat(newText));
                return false;
            }
        });
    }


    class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.paynym_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 12;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
