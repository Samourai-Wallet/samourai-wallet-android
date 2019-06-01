package com.samourai.wallet.whirlpool.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.Group;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.CycleDetail;
import com.samourai.wallet.whirlpool.models.Cycle;
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity;
import com.samourai.wallet.widgets.ItemDividerDecorator;

import java.util.ArrayList;
import java.util.List;

public class WhirlpoolCyclesFragment extends Fragment {

    private WhirlpoolCyclesFragmentAdapter adapter;
    private RecyclerView cyclesList;
    public WhirlpoolCyclesFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.whirlpool_cycles_list, container, false);

        adapter = new WhirlpoolCyclesFragmentAdapter(new ArrayList<>());

        cyclesList = view.findViewById(R.id.whirlpool_cycles_rv);
        cyclesList.setLayoutManager(new LinearLayoutManager(getContext()));
        cyclesList.setAdapter(adapter);
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);

        cyclesList.addItemDecoration(new ItemDividerDecorator(drawable));
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    private class WhirlpoolCyclesFragmentAdapter extends RecyclerView.Adapter<WhirlpoolCyclesFragmentAdapter.ViewHolder> {


        WhirlpoolCyclesFragmentAdapter(List<Cycle> items) {

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cycle_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.itemView.setOnClickListener( view -> startActivity(new Intent( getActivity(), CycleDetail.class)));
        }

        @Override
        public int getItemCount() {
            return 5;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;


            ViewHolder(View view) {
                super(view);
                mView = view;

            }

        }
    }

}
