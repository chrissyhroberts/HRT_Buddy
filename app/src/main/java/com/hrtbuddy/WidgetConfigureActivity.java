package com.hrtbuddy;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WidgetConfigureActivity extends Activity {
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24),dp(24),dp(24),dp(24));
        TextView title=new TextView(this); title.setText("Widget appearance"); title.setTextSize(24); title.setTextColor(Color.rgb(39,35,50)); title.setTypeface(null,1); root.addView(title);
        TextView hint=new TextView(this); hint.setText("Choose the size of the cycle map."); hint.setTextSize(15); hint.setTextColor(Color.rgb(91,85,105)); root.addView(hint);
        String[] sizes={"Compact","Standard","Large"};
        int selected=Math.max(0,Math.min(2,State.p(this).getInt("widgetMapSize",1)));
        for(int i=0;i<sizes.length;i++){final int choice=i;Button b=new Button(this);b.setText(sizes[i]);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(Color.rgb(105,82,210));b.setOnClickListener(v->{State.p(this).edit().putInt("widgetMapSize",choice).apply();HrtBuddyWidget.refresh(this);setResult(RESULT_OK);finish();});root.addView(b,new LinearLayout.LayoutParams(-1,dp(52)));b.setSelected(i==selected);}
        setContentView(root);
    }
}
