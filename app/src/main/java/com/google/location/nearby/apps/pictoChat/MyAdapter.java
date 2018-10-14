package com.google.location.nearby.apps.pictoChat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class MyAdapter extends ArrayAdapter<item> {
    private Context context;
    private ArrayList<item> items;

    public MyAdapter(Context context, ArrayList<item> items){
        super(context, R.layout.activity_listview, items);
        this.context = context;
        this.items=items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View currentView = null;
        if(currentView == null) {
            currentView = LayoutInflater.from(context).inflate(R.layout.activity_listview, parent, false);
        }
        item currentItem = items.get(position);

        ImageView image = currentView.findViewById(R.id.imageView);
        TextView message = currentView.findViewById(R.id.label);
        boolean mode = currentItem.getMode();
        if(!mode){
            message.setText(currentItem.getMessage());
            image.setVisibility(View.INVISIBLE);
        }
        else{
            Bitmap receivedImage = BitmapFactory.decodeByteArray(currentItem.getDrawing(),0,currentItem.getDrawing().length);
            image.setImageBitmap(receivedImage);
        }
        return currentView;
    }
}
