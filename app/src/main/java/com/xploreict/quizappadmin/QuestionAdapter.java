package com.xploreict.quizappadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.viewholder> {

    private List<QuestionModel> list;
    private String category;
    private DeleteListiner listiner;

    public QuestionAdapter(List<QuestionModel> list,String category,DeleteListiner listiner) {
        this.list = list;
        this.category = category;
        this.listiner = listiner;
    }

    @NonNull
    @Override
    public viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.question_item,parent,false);
        return new viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewholder holder, int position) {
        String question = list.get(position).getQuestion();
        String answer = list.get(position).getAnswer();
        holder.setData(question,answer,position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class viewholder extends RecyclerView.ViewHolder{

        private TextView question,answer;

        public viewholder(@NonNull View itemView) {
            super(itemView);

            question = itemView.findViewById(R.id.question);
            answer = itemView.findViewById(R.id.answer);
        }

        private void setData(String question, String answer, final int position){
            this.question.setText(position+1+". "+question);
            this.answer.setText("Ans. "+answer);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent editIntent = new Intent(itemView.getContext(),AddQuestionActivity.class);
                    editIntent.putExtra("categoryName",category);
                    editIntent.putExtra("setId",list.get(position).getSet());
                    editIntent.putExtra("position",position);
                    itemView.getContext().startActivity(editIntent);
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                   listiner.onLongclick(position,list.get(position).getId());
                    return false;
                }
            });
        }
    }
    public interface DeleteListiner {
        void onLongclick(int position,String id);
    }
}
