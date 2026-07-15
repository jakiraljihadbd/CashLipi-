package com.jrappspot.cashlipi.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jrappspot.cashlipi.R;

public class MethodSpinnerAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] names;
    private final int[] icons;

    public MethodSpinnerAdapter(Context context, String[] names, int[] icons) {
        super(context, R.layout.item_spinner_method, names);
        this.context = context;
        this.names = names;
        this.icons = icons;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, R.layout.item_spinner_method);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, R.layout.item_spinner_method_dropdown);
    }

    private View createView(int position, View convertView, ViewGroup parent, int layoutRes) {
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        ImageView img = view.findViewById(R.id.imgMethodIcon);
        TextView txt = view.findViewById(R.id.tvMethodName);
        txt.setText(names[position]);
        if (icons != null && position < icons.length) {
            img.setImageResource(icons[position]);
        }
        return view;
    }
}
