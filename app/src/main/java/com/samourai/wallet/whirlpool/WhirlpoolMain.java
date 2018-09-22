package com.samourai.wallet.whirlpool;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.adapters.CyclesAdapter;
import com.samourai.wallet.whirlpool.models.Coin;
import com.samourai.wallet.whirlpool.models.Cycle;

import java.util.ArrayList;

public class WhirlpoolMain extends AppCompatActivity {

    private RecyclerView CycleRecyclerView;
    private CyclesAdapter CyclesAdapter;
    private ArrayList<Cycle> cycles = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whirlpool_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CycleRecyclerView = findViewById(R.id.rv_cycles);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        CycleRecyclerView.setLayoutManager(linearLayoutManager);

        loadDummyCycles();
        CyclesAdapter = new CyclesAdapter(this, cycles);
        CycleRecyclerView.setAdapter(CyclesAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(CycleRecyclerView.getContext(),
                linearLayoutManager.getOrientation());
        CyclesAdapter.setItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WhirlpoolMain.this,CycleDetail.class);
                startActivity(intent);
            }
        });

        CycleRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_whirl_pool_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void loadDummyCycles() {
        for (int i = 0; i <= 62; i++) {
            Cycle cycle = new Cycle();
            cycle.setAmount(300+0.0030f+i);
            cycle.setStatus(Cycle.CycleStatus.PENDING);
            cycles.add(cycle);
        }
    }


}
