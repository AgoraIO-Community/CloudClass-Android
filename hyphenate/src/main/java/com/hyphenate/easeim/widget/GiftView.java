package com.hyphenate.easeim.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.adapter.GiftGridAdapter;
import com.hyphenate.easeim.domain.Gift;
import com.hyphenate.easeim.interfaces.GiftItemListener;
import com.hyphenate.easeim.interfaces.GiftViewListener;

import java.util.ArrayList;
import java.util.List;

public class GiftView extends LinearLayout implements GiftItemListener, View.OnClickListener {

    private ImageView closeView;
    private RecyclerView gridView;
    private TextView scopeView;
    private ImageView doubtView;
    private RelativeLayout promptRoot;
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

    public void init(List<Gift> gifts, String totalScore) {
        closeView = findViewById(R.id.close_gift);
        gridView = findViewById(R.id.gift_grid);
        scopeView = findViewById(R.id.scope);
        doubtView = findViewById(R.id.iv_doubt);
        promptRoot = findViewById(R.id.prompt_root);
        String scope = String.format("%s学分", totalScore);
        scopeView.setText(scope);
        giftList = gifts;
        GiftGridAdapter gridAdapter = new GiftGridAdapter(giftList, context);
        gridView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        gridView.setAdapter(gridAdapter);
//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                gridAdapter.setSeclection(i);
//                gridAdapter.notifyDataSetChanged();
//            }
//        });

        gridAdapter.setGiftViewListener(this);
        closeView.setOnClickListener(this);
        doubtView.setOnClickListener(this);
        promptRoot.setOnClickListener(this);
    }

    @Override
    public void onGiveGift(Gift gift) {
        giftViewListener.onGiftSend(gift);
    }

    public void setGiftViewListener(GiftViewListener giftViewListener) {
        this.giftViewListener = giftViewListener;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.close_gift) {
            promptRoot.setVisibility(GONE);
            giftViewListener.onCloseGiftView();
        } else if (id == R.id.iv_doubt) {
            if (promptRoot.getVisibility() == VISIBLE) {
                promptRoot.setVisibility(GONE);
            } else {
                promptRoot.setVisibility(VISIBLE);
            }
        } else if (id == R.id.prompt_root) {
            promptRoot.setVisibility(GONE);
        }
    }
}
