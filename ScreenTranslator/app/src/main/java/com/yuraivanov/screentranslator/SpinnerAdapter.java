package com.yuraivanov.screentranslator;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class SpinnerAdapter extends BaseAdapter {
    private final String TAG = "SpinnerAdapter";

    private LayoutInflater layoutInflater;
    private ArrayList<String> arrayList = new ArrayList<>();

    public SpinnerAdapter(Context context){
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return arrayList.size();
    }
    @Override
    public String getItem(int position) {
        return arrayList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return 0;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(getItem(position)==null || getItem(position).isEmpty()){
            Log.d(TAG,"getDropDownView item" + position + " null\n"+arrayList);
            return null;
        }
        View view = layoutInflater.inflate(R.layout.spinner_item,parent,false);
        TextView textView = view.findViewById(R.id.item);
        textView.setText(getItem(position));
        return view;
    }
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if(getItem(position)==null || getItem(position).isEmpty()){
            Log.d(TAG,"getDropDownView item null");
            return null;
        }
        View view = layoutInflater.inflate(R.layout.spinner_item,parent,false);
        TextView textView = view.findViewById(R.id.item);
        textView.setText(getItem(position));
        return view;
    }
    public void add(String item){
        arrayList.add(item);
    }
    public boolean contain(String item){
        if(!item.isEmpty()){
            for(int i=0;i<arrayList.size();i++){
                String text = arrayList.get(i);
                if (text.contains(item)){
                    return true;
                }
            }
        }
        return false;
    }
}
