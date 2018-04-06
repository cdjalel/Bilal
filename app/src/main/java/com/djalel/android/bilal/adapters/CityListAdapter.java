package com.djalel.android.bilal.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.djalel.android.bilal.R;
import com.djalel.android.bilal.datamodels.City;

import java.util.List;


public class CityListAdapter extends BaseAdapter {
    private final List<City> mCities;
    private final Context mContext;

    static class ViewHolder {
        public TextView text;
    }

    public CityListAdapter(Context context , List<City> cityList) {
        mContext = context;
        mCities = cityList;
    }

    @Override
    public int getCount() {
        return mCities != null ? mCities.size() : 0;
    }

    @Override
    public Object getItem(int position) { return mCities != null ? mCities.get(position) : null; }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = View.inflate(mContext, R.layout.item_list_city, null);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.text = (TextView) view.findViewById(R.id.tv_city);
            view.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.text.setText(mCities.get(i).toLongString());

        return view;
    }
}
