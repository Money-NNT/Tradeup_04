package com.example.baicuoiky04;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class UriImageSliderAdapter extends RecyclerView.Adapter<UriImageSliderAdapter.SliderViewHolder> {

    private List<Uri> imageUris;
    private Context context;

    public UriImageSliderAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        Glide.with(context)
                .load(imageUri)
                .placeholder(R.color.light_gray)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUris != null ? imageUris.size() : 0;
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewSlider);
        }
    }
}