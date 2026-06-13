package com.skylark.detectormonitor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;

import com.skylark.detectormonitor.R;

import java.util.List;

public class DeviceAdapter extends ArrayAdapter<String> {

    private final List<String> rooms;
    private final LayoutInflater inflater;

    public DeviceAdapter(@NonNull Context context, int resource, @NonNull List<String> rooms) {
        super(context, resource, rooms);
        this.rooms = rooms;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_device, parent, false);
        }

        TextView tvRoomName = convertView.findViewById(R.id.tv_room_name);
        tvRoomName.setText(rooms.get(position));

        return convertView;
    }
}
