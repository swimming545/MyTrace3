package com.holy.mytrace.adapters;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.holy.mytrace.R;
import com.holy.mytrace.models.Hospital;

import java.util.List;
import java.util.Locale;

public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.ViewHolder> {

    private final List<Hospital> list;
    private OnItemClickListener onItemClickListener;

    public HospitalAdapter(List<Hospital> list) {
        this.list = list;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameText;
        private final TextView addressText;
        private final TextView telText;
        private final TextView distanceText;

        public ViewHolder(View itemView) {
            super(itemView);

            nameText = itemView.findViewById(R.id.txtHospitalName);
            addressText = itemView.findViewById(R.id.txtHospitalAddress);
            telText = itemView.findViewById(R.id.txtHospitalTel);
            distanceText = itemView.findViewById(R.id.txtHospitalDistance);
        }
        public void bind(Hospital model, OnItemClickListener listener) {

            nameText.setText(model.getName());
            addressText.setText(model.getAddress());
            telText.setText(model.getTel());

            String strDistance = String.format(Locale.getDefault(),
                    "%dm", Math.round(model.getDistance()));
            distanceText.setText(strDistance);

            if (listener != null) {
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                });
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hospital, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Hospital item = list.get(position);
        holder.bind(item, onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

}