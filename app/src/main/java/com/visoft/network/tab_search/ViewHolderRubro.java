package com.visoft.network.tab_search;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.visoft.network.R;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.viewholders.FlexibleViewHolder;

public class ViewHolderRubro extends FlexibleViewHolder {
    public TextView nombre;
    public ImageView img;

    public ViewHolderRubro(View view, FlexibleAdapter adapter) {
        super(view, adapter);

        img = view.findViewById(R.id.img);
        nombre = view.findViewById(R.id.tvTitle);
    }
}
