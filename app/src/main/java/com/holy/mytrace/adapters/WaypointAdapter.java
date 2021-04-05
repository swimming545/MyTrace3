package com.holy.mytrace.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.holy.mytrace.R;
import com.holy.mytrace.models.Waypoint;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class WaypointAdapter extends RecyclerView.Adapter<WaypointAdapter.ViewHolder> {

    private final List<Waypoint> list;
    private OnItemClickListener onItemClickListener;

    public WaypointAdapter(List<Waypoint> list) {
        this.list = list;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView locationText;
        private final TextView beginTimeText;
        private final TextView endTimeText;

        public ViewHolder(View itemView) {
            super(itemView);

            locationText = itemView.findViewById(R.id.txtWaypointLocation);
            beginTimeText = itemView.findViewById(R.id.txtWaypointBeginTime);
            endTimeText = itemView.findViewById(R.id.txtWaypointEndTime);
        }

        public void bind(Waypoint model, OnItemClickListener listener) {

            String strLocation = String.format(Locale.getDefault(), "%f, %f",
                    model.getLatitude(), model.getLongitude());
            locationText.setText(strLocation);

            String strBeginTime = String.format(Locale.getDefault(), "%02d : %02d",
                    model.getBeginTime().getHour(), model.getBeginTime().getMinute());
            beginTimeText.setText(strBeginTime);

            String strEndTime = String.format(Locale.getDefault(), "%02d : %02d",
                    model.getEndTime().getHour(), model.getEndTime().getMinute());
            endTimeText.setText(strEndTime);

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
                .inflate(R.layout.item_waypoint, parent, false);

        return new ViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Waypoint item = list.get(position);
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