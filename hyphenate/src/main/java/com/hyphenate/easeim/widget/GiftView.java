package com.hyphenate.easeim.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.adapter.GiftGridAdapter;
import com.hyphenate.easeim.domain.Gift;
import com.hyphenate.easeim.interfaces.GiftItemListener;
import com.hyphenate.easeim.interfaces.GiftViewListener;

import java.util.ArrayList;
import java.util.List;

public class GiftView extends LinearLayout implements GiftItemListener, View.OnClickListener {

    private ImageView closeView;
    private GridView gridView;
    private TextView scopeVeiw;
    private Context context;
    private GiftViewListener giftViewListener;
    private List<Gift> giftList = new ArrayList<>();

    public GiftView(Context context) {
        this(context, null);
    }

    public GiftView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GiftView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.gift_view, this);
    }

    public void init(List<Gift> gifts, String totalScore){
        closeView = findViewById(R.id.close_gift);
        gridView = findViewById(R.id.gift_grid);
        scopeVeiw = findViewById(R.id.scope);
        String scope = String.format("%s学分", totalScore);
        scopeVeiw.setText(scope);
        giftList = gifts;
        GiftGridAdapter gridAdapter = new GiftGridAdapter(context, 1,giftList);
        gridView.setAdapter(gridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                gridAdapter.setSeclection(i);
                gridAdapter.notifyDataSetChanged();
            }
        });

        gridAdapter.setGiftViewListener(this);
        closeView.setOnClickListener(this);
    }

    @Override
    public void onGiveGift(Gift gift) {
        giftViewListener.onGiftSend(gift);
    }

    public void setGiftViewListener(GiftViewListener giftViewListener){
        this.giftViewListener = giftViewListener;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.close_gift){
            giftViewListener.onCloseGiftView();
        }
    }
}
