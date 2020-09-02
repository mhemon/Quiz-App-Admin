package com.xploreict.quizappadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.viewholder> {

    private List<CategoriesModel> categoriesModelList;
    private DeleteLisitner deleteLisitner;

    public CategoryAdapter(List<CategoriesModel> categoriesModelList,DeleteLisitner deleteLisitner) {
        this.categoriesModelList = categoriesModelList;
        this.deleteLisitner = deleteLisitner;
    }

    @NonNull
    @Override
    public viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.categories_item,parent,false);
        return new viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewholder holder, int position) {
        holder.setData(categoriesModelList.get(position).getUrl(),categoriesModelList.get(position).getName(),categoriesModelList.get(position).getKey(),position);
    }

    @Override
    public int getItemCount() {
        return categoriesModelList.size();
    }

    class viewholder extends RecyclerView.ViewHolder{

        private CircleImageView imageView;
        private TextView title;
        private ImageButton delete;

        public viewholder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            title = itemView.findViewById(R.id.title_text);
            delete = itemView.findViewById(R.id.delete);
        }
        private void setData(String url, final String title,final String key, final int position){
            Glide.with(itemView.getContext()).load(url).into(imageView);
            this.title.setText(title);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent setIntent = new Intent(itemView.getContext(),SetsActivity.class);
                    setIntent.putExtra("title",title);
                    setIntent.putExtra("position",position);
                    setIntent.putExtra("key",key);
                    itemView.getContext().startActivity(setIntent);
                }
            });

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteLisitner.ondelete(key,position);
                }
            });
        }
    }
    public interface DeleteLisitner{
        public void ondelete(String key,int position);
    }
}
