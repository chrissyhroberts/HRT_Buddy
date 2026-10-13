package com.hrtbuddy;
import android.appwidget.AppWidgetManager;
import android.content.Context;
public class HrtBuddyWidgetCompact extends HrtBuddyWidget {
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){scheduleBoundaryRefresh(c);for(int id:ids)update(c,manager,id,0);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,android.os.Bundle options){update(c,manager,id,0);}
}
