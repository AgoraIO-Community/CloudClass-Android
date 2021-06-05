package com.hyphenate.easeim.adapter;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.domain.Gift;
import com.hyphenate.easeim.interfaces.GiftItemListener;
import com.hyphenate.util.EMLog;

import java.util.List;

public class GiftGridAdapter extends ArrayAdapter<Gift> {

    private  int clickTemp;
    private GiftItemListener giftItemListener;

    public GiftGridAdapter(@NonNull Context context, int resource, @NonNull List<Gift> objects) {
        super(context, resource, objects);
    }

    public void setSeclection(int position) {
        clickTemp = position;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = View.inflate(getContext(), R.layout.gift_item, null);
        }

        LinearLayout root = convertView.findViewById(R.id.root_gift_item);
        ImageView giftImg = convertView.findViewById(R.id.gift_img);
        TextView scope= convertView.findViewById(R.id.gift_scope);
        TextView giftName = convertView.findViewById(R.id.gift_name);
        TextView giftGive = convertView.findViewById(R.id.gift_give);
        giftName.setVisibility(View.VISIBLE);
        giftGive.setVisibility(View.GONE);
        root.setBackground(getContext().getDrawable(R.color.white));
        if(clickTemp == position){
            giftName.setVisibility(View.GONE);
            giftGive.setVisibility(View.VISIBLE);
            root.setBackground(getContext().getDrawable(R.drawable.gift_item_background));
        }

        Gift gift = getItem(position);

        Glide.with(getContext().getApplicationContext()).load(gift.getImg())
                .apply(RequestOptions.placeholderOf(R.mipmap.ee_33))
                .into(giftImg);
        scope.setText(gift.getScore()+"学分");
        giftName.setText(gift.getName());
        giftGive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EMLog.e("GridView", "give gift");
                giftItemListener.onGiveGift(gift);
            }
        });
        return convertView;
    }

    public void setGiftViewListener(GiftItemListener giftItemListener){
        this.giftItemListener = giftItemListener;
    }
}
