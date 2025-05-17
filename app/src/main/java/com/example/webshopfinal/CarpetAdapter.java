package com.example.webshopfinal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class CarpetAdapter extends RecyclerView.Adapter<CarpetAdapter.CarpetViewHolder> {
    public interface OnCarpetClickListener {
        void onCarpetClick(String carpetDocId);
    }

    private final List<FirestoreDao.CarpetWithId> carpets;
    private final Context context;
    private final OnCarpetClickListener listener;

    public CarpetAdapter(Context context, List<FirestoreDao.CarpetWithId> carpets, OnCarpetClickListener listener) {
        this.context = context;
        this.carpets = carpets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarpetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_carpet, parent, false);
        return new CarpetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarpetViewHolder holder, int position) {
        FirestoreDao.CarpetWithId carpetWithId = carpets.get(position);
        Carpet carpet = carpetWithId.carpet;
        holder.nameText.setText(carpet.getName());
        holder.priceText.setText(String.format("%.0f Ft", carpet.getPrice()));
        Glide.with(context)
                .load(carpet.getImageUrl())
                .placeholder(R.drawable.rug_background)
                .into(holder.imageView);
        holder.itemView.setOnClickListener(v -> listener.onCarpetClick(carpetWithId.docId));
    }

    @Override
    public int getItemCount() {
        return carpets.size();
    }

    static class CarpetViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText, priceText;
        CarpetViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carpetImageView);
            nameText = itemView.findViewById(R.id.carpetNameText);
            priceText = itemView.findViewById(R.id.carpetPriceText);
        }
    }
} 