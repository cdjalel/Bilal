package org.linuxac.bilal.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.linuxac.bilal.R;
import org.linuxac.bilal.datamodels.City;

import java.util.List;


public class CityListAdapter extends BaseAdapter {
    private List<City> mCities;
    private Context mContext;

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
        View v = view.inflate(mContext, R.layout.item_list_city, null);

        City city = mCities.get(i);

        TextView name = (TextView) v.findViewById(R.id.tv_city_name);
        name.setText(city.getName());
        TextView time = (TextView) v.findViewById(R.id.tv_country_name);
        time.setText("(" + city.getCountry() + ", " + city.getRegion() + ")");

        return v;
    }
}
