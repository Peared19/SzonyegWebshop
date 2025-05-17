package com.example.webshopfinal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    public interface OnCartItemListener {
        void onRemove(CartItem item);
        void onQuantityChanged(CartItem item, int newQuantity);
    }

    private final List<CartItem> cartItems;
    private final List<Carpet> carpets;
    private final Context context;
    private final OnCartItemListener listener;

    public CartAdapter(Context context, List<CartItem> cartItems, List<Carpet> carpets, OnCartItemListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.carpets = carpets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        Carpet carpet = null;
        for (Carpet c : carpets) {
            if (c != null && c.getDocId() != null && c.getDocId().equals(item.getCarpetDocId())) {
                carpet = c;
                break;
            }
        }
        if (carpet != null) {
            holder.nameText.setText(carpet.getName());
            holder.priceText.setText(String.format("%.0f Ft", carpet.getPrice()));
            Glide.with(context)
                    .load(carpet.getImageUrl())
                    .placeholder(R.drawable.rug_background)
                    .into(holder.imageView);
        } else {
            holder.nameText.setText("-");
            holder.priceText.setText("");
            holder.imageView.setImageResource(R.drawable.rug_background);
        }
        
        holder.quantityText.setText(String.valueOf(item.getQuantity()));
        
        holder.decreaseBtn.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() - 1;
            if (newQuantity > 0) {
                listener.onQuantityChanged(item, newQuantity);
            }
        });
        
        holder.increaseBtn.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;
            listener.onQuantityChanged(item, newQuantity);
        });
        
        holder.removeBtn.setOnClickListener(v -> listener.onRemove(item));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText, priceText, quantityText;
        ImageButton removeBtn, increaseBtn, decreaseBtn;
        
        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.cartItemImage);
            nameText = itemView.findViewById(R.id.cartItemName);
            priceText = itemView.findViewById(R.id.cartItemPrice);
            quantityText = itemView.findViewById(R.id.cartItemQuantity);
            removeBtn = itemView.findViewById(R.id.cartItemRemoveBtn);
            increaseBtn = itemView.findViewById(R.id.cartItemIncreaseBtn);
            decreaseBtn = itemView.findViewById(R.id.cartItemDecreaseBtn);
        }
    }
} 