package com.hyphenate.easeim.adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.domain.Gift;
import com.hyphenate.easeim.interfaces.GiftItemListener;
import com.hyphenate.util.EMLog;

import java.util.List;

public class GiftGridAdapter extends RecyclerView.Adapter<GiftGridAdapter.ViewHolder> {

    private  int clickTemp;
    private GiftItemListener giftItemListener;
    private List<Gift> gifts;
    private Context context;

    public GiftGridAdapter(List<Gift> gifts, Context context) {
        this.gifts = gifts;
        this.context = context;
    }

    public void setGiftViewListener(GiftItemListener giftItemListener){
        this.giftItemListener = giftItemListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gift_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.giftName.setVisibility(View.VISIBLE);
        holder.giftGive.setVisibility(View.GONE);
        holder.root.setBackground(context.getResources().getDrawable(R.color.white));
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickTemp = position;
                notifyDataSetChanged();
            }
        });
        if(clickTemp == position){
            holder.giftName.setVisibility(View.GONE);
            holder.giftGive.setVisibility(View.VISIBLE);
            holder.root.setBackground(context.getResources().getDrawable(R.drawable.gift_item_background));
        }

        Gift gift = gifts.get(position);

        Glide.with(context.getApplicationContext()).load(gift.getImg())
                .apply(RequestOptions.placeholderOf(R.mipmap.ee_33))
                .into(holder.giftImg);
        holder.scope.setText(gift.getScore()+"学分");
        holder.giftName.setText(gift.getName());
        holder.giftGive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EMLog.e("GridView", "give gift");
                giftItemListener.onGiveGift(gift);
            }
        });
    }

    @Override
    public int getItemCount() {
        return gifts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        LinearLayout root;
        ImageView giftImg;
        TextView scope;
        TextView giftName;
        TextView giftGive;

        public ViewHolder(View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root_gift_item);
            giftImg = itemView.findViewById(R.id.gift_img);
            scope = itemView.findViewById(R.id.gift_scope);
            giftName = itemView.findViewById(R.id.gift_name);
            giftGive = itemView.findViewById(R.id.gift_give);
        }
    }
}
