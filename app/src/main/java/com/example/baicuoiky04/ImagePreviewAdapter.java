package com.example.baicuoiky04;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {

    public interface OnImageRemoveListener {
        void onImageRemoved(int position);
    }

    private Context context;
    private List<Uri> imageUris;
    private OnImageRemoveListener removeListener;

    public ImagePreviewAdapter(Context context, List<Uri> imageUris, OnImageRemoveListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        Glide.with(context).load(imageUri).into(holder.imageViewSelected);

        holder.buttonRemoveImage.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onImageRemoved(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewSelected;
        ImageButton buttonRemoveImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewSelected = itemView.findViewById(R.id.imageViewSelected);
            buttonRemoveImage = itemView.findViewById(R.id.buttonRemoveImage);
        }
    }
}