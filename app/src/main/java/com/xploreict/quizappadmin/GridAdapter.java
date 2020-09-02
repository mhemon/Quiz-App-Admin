package com.xploreict.quizappadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class GridAdapter extends BaseAdapter {

    public List<String> sets;
    private String category;
    private GridListner listner;

    public GridAdapter(List<String> sets, String category, GridListner listner) {
        this.sets = sets;
        this.category = category;
        this.listner = listner;
    }
    @Override
    public int getCount() {
        return sets.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertview, final ViewGroup parent) {
        View view;
        if (convertview == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_item, parent, false);
        } else {
            view = convertview;
        }

        if (position == 0) {
            ((TextView) view.findViewById(R.id.text_view)).setText("+");
        } else {
            ((TextView) view.findViewById(R.id.text_view)).setText(String.valueOf(position));
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (position == 0) {
                    listner.addset();
                } else {
                    Intent qusIntent = new Intent(parent.getContext(), QuestionsActivity.class);
                    qusIntent.putExtra("category", category);
                    //original code was position
                    qusIntent.putExtra("setId", sets.get(position-1));
                    parent.getContext().startActivity(qusIntent);
                }
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (position != 0) {
                    listner.onLongClick(sets.get(position-1),position);
                }
                return false;
            }
        });

        return view;
    }
    public interface GridListner{
        public void addset();
        void onLongClick(String setId, int position);
    }
}

