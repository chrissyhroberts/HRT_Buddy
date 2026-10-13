package com.hrtbuddy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.util.Calendar;

public class HrtBuddyWidget extends AppWidgetProvider {
    static final String ACTION_CARE_DAY_REFRESH="com.hrtbuddy.ACTION_CARE_DAY_REFRESH";
    public void onReceive(Context c, Intent i) { super.onReceive(c,i); if(ACTION_CARE_DAY_REFRESH.equals(i.getAction())) refresh(c); }
    public void onUpdate(Context c, AppWidgetManager manager, int[] ids) { scheduleBoundaryRefresh(c); for (int id : ids) update(c, manager, id); }
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager manager, int id, android.os.Bundle options) { update(c, manager, id); }

    static int scoreColor(String type,int score) {
        int[] pain={Color.rgb(255,225,225),Color.rgb(255,210,210),Color.rgb(255,195,195),Color.rgb(250,177,177),Color.rgb(246,158,158),Color.rgb(239,139,139),Color.rgb(232,119,119),Color.rgb(224,98,98),Color.rgb(214,75,80),Color.rgb(195,45,58)};
        int[] wellbeing={Color.rgb(217,79,92),Color.rgb(230,103,106),Color.rgb(240,128,128),Color.rgb(245,154,133),Color.rgb(246,179,106),Color.rgb(233,201,74),Color.rgb(183,201,74),Color.rgb(127,196,107),Color.rgb(85,184,117),Color.rgb(58,170,106)};
        int[] palette=type.equals("pain")?pain:wellbeing;
        return palette[Math.max(1,Math.min(10,score))-1];
    }

    static int color(Context c, LocalDate date, String type) {
        LocalDate today=State.careDate();
        if(type.equals("patch")||type.equals("pill")) {
            if(!State.scheduled(c,date,type)) return Color.rgb(246,244,249);
            if(State.confirmedOn(c,date,type)) return Color.rgb(126,205,145);
            if(State.skippedOn(c,date,type)||date.isBefore(today)) return Color.rgb(239,145,148);
            return Color.rgb(255,211,102);
        }
        if(type.equals("bleeding")) {
            String value=State.dailyText(c,date,"bleeding");
            if(value.equals("None")) return Color.rgb(246,244,249);
            if(value.equals("Heavy")) return Color.rgb(195,45,58);
            if(value.equals("Medium")) return Color.rgb(226,108,112);
            if(value.equals("Light")) return Color.rgb(249,178,178);
            if(value.equals("Spotting")) return Color.rgb(255,215,215);
            return Color.rgb(246,244,249);
        }
        if(State.skippedOn(c,date,"checkin")) return Color.rgb(239,145,148);
        int value=State.dailyInt(c,date,type);
        return value<0?Color.rgb(246,244,249):scoreColor(type,value);
    }

    static void update(Context c, AppWidgetManager manager, int id) {
        try {
            RemoteViews view=new RemoteViews(c.getPackageName(),R.layout.widget);
            Intent open=new Intent(c,MainActivity.class);
            view.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,40,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            int cycle=Math.max(1,Math.min(60,State.cycle(c)));
            android.os.Bundle options=manager.getAppWidgetOptions(id);
            int width=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250));
            float mapSize=Math.max(9f,Math.min(15f,10f+(width-250)/90f));
            for(int mapId:new int[]{R.id.widget_map_patch_text,R.id.widget_map_prog_text,R.id.widget_map_bleeding_text,R.id.widget_map_pain_text,R.id.widget_map_mood_text,R.id.widget_map_overall_text}) view.setTextViewTextSize(mapId,android.util.TypedValue.COMPLEX_UNIT_SP,mapSize);
            view.setTextViewText(R.id.widget_title,"HRT BUDDY  ·  FULL CYCLE");
            view.setTextViewText(R.id.widget_day,"Day "+State.day(c)+" / "+cycle);
            view.setTextViewText(R.id.widget_status,State.status(c));
            String patchDays=State.p(c).getString("patchDays","TUE,SAT").replace("TUE","Tue").replace("WED","Wed").replace("THU","Thu").replace("FRI","Fri").replace("SAT","Sat").replace("SUN","Sun").replace("MON","Mon");
            LocalDate today=State.careDate();
            LocalDate first=State.cycleStart(c);
            view.setTextViewText(R.id.widget_map_patch_text,mapTextPlain(c,first,"Patch  ","patch",cycle));
            view.setTextViewText(R.id.widget_map_prog_text,mapTextPlain(c,first,"Prog.  ","pill",cycle));
            view.setTextViewText(R.id.widget_map_bleeding_text,mapText(c,first,"Blood  ","bleeding",cycle));
            view.setTextViewText(R.id.widget_map_pain_text,mapText(c,first,"Pain   ","pain",cycle));
            view.setTextViewText(R.id.widget_map_mood_text,mapText(c,first,"Mood   ","mood",cycle));
            view.setTextViewText(R.id.widget_map_overall_text,mapText(c,first,"Feel   ","overall",cycle));
            view.setViewVisibility(R.id.widget_map_bleeding_text,State.showMap(c,"bleeding")?android.view.View.VISIBLE:android.view.View.GONE);
            view.setViewVisibility(R.id.widget_map_pain_text,State.showMap(c,"pain")?android.view.View.VISIBLE:android.view.View.GONE);
            view.setViewVisibility(R.id.widget_map_mood_text,State.showMap(c,"mood")?android.view.View.VISIBLE:android.view.View.GONE);
            view.setViewVisibility(R.id.widget_map_overall_text,State.showMap(c,"overall")?android.view.View.VISIBLE:android.view.View.GONE);
            boolean patchDue=State.scheduled(c,today,"patch");
            boolean pillDue=State.scheduled(c,today,"pill");
            boolean checkinDue=State.scheduled(c,today,"checkin");
            boolean healthDone=State.confirmed(c,"checkin")||State.dailyInt(c,today,"pain")>=0||State.dailyInt(c,today,"mood")>=0||State.dailyInt(c,today,"overall")>=0||!State.dailyText(c,today,"bleeding").isEmpty();
            view.setViewVisibility(R.id.widget_actions,(patchDue||pillDue||checkinDue)?android.view.View.VISIBLE:android.view.View.GONE);
            view.setViewVisibility(R.id.widget_action_patch,patchDue?android.view.View.VISIBLE:android.view.View.GONE);
            view.setViewVisibility(R.id.widget_action_pill,pillDue?android.view.View.VISIBLE:android.view.View.GONE);
            view.setViewVisibility(R.id.widget_action_checkin,checkinDue?android.view.View.VISIBLE:android.view.View.GONE);
            if(patchDue){boolean done=State.confirmed(c,"patch");view.setTextViewText(R.id.widget_action_patch,done?"● Patch changed today":"● Change patch");view.setTextColor(R.id.widget_action_patch,done?Color.rgb(42,150,92):Color.rgb(205,65,76));view.setBoolean(R.id.widget_action_patch,"setEnabled",!done);view.setOnClickPendingIntent(R.id.widget_action_patch,ReminderReceiver.action(c,"patch","done",30));}
            if(pillDue){boolean done=State.confirmed(c,"pill");view.setTextViewText(R.id.widget_action_pill,done?"● Progesterone taken today":"● Take progesterone");view.setTextColor(R.id.widget_action_pill,done?Color.rgb(42,150,92):Color.rgb(205,65,76));view.setBoolean(R.id.widget_action_pill,"setEnabled",!done);view.setOnClickPendingIntent(R.id.widget_action_pill,ReminderReceiver.action(c,"pill","done",31));}
            if(checkinDue){view.setTextViewText(R.id.widget_action_checkin,healthDone?"● Health check complete":"● Health check");view.setTextColor(R.id.widget_action_checkin,healthDone?Color.rgb(42,150,92):Color.rgb(205,65,76));view.setBoolean(R.id.widget_action_checkin,"setEnabled",!healthDone);view.setOnClickPendingIntent(R.id.widget_action_checkin,ReminderReceiver.openHealth(c));}
            manager.updateAppWidget(id,view);
        } catch (RuntimeException brokenWidget) {
            RemoteViews fallback=new RemoteViews(c.getPackageName(),R.layout.widget);
            fallback.setTextViewText(R.id.widget_title,"HRT BUDDY");
            fallback.setTextViewText(R.id.widget_day,"Day "+State.day(c)+" / "+State.cycle(c));
            fallback.setTextViewText(R.id.widget_status,"Open HRT Buddy to view your cycle");
            manager.updateAppWidget(id,fallback);
        }
    }

    static CharSequence mapText(Context c, LocalDate first, String label, String type, int cycle) {
        StringBuilder symbols=new StringBuilder(label);
        int labelLength=symbols.length();
        int shown=Math.min(cycle,35);
        for(int i=0;i<shown;i++) {
            LocalDate date=first.plusDays(i);
            symbols.append(State.careDate().equals(date)?'┃':(color(c,date,type)==Color.rgb(246,244,249)?'·':'■'));
        }
        SpannableString result=new SpannableString(symbols.toString());
        int cursor=labelLength;
        for(int i=0;i<shown;i++) {
            LocalDate date=first.plusDays(i);
            int cellColor=color(c,date,type);
            if(cellColor==Color.rgb(246,244,249)) cellColor=Color.rgb(150,145,160);
            result.setSpan(new ForegroundColorSpan(State.careDate().equals(date)?Color.rgb(205,65,76):cellColor),cursor,cursor+1,SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            cursor++;
        }
        return result;
    }

    static CharSequence mapTextPlain(Context c, LocalDate first, String label, String type, int cycle) {
        StringBuilder out=new StringBuilder(label);
        int labelLength=out.length();
        int shown=Math.min(cycle,35);
        for(int i=0;i<shown;i++) {
            LocalDate date=first.plusDays(i);
            out.append(State.careDate().equals(date)?'┃':(color(c,date,type)==Color.rgb(246,244,249)?'·':'■'));
        }
        SpannableString result=new SpannableString(out.toString());
        int cursor=labelLength;
        for(int i=0;i<shown;i++) {
            LocalDate date=first.plusDays(i);
            int cellColor=color(c,date,type);
            if(cellColor==Color.rgb(246,244,249)) cellColor=Color.rgb(80,76,92);
            result.setSpan(new ForegroundColorSpan(State.careDate().equals(date)?Color.rgb(205,65,76):cellColor),cursor,cursor+1,SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            cursor++;
        }
        return result;
    }

    public void onEnabled(Context c) { scheduleBoundaryRefresh(c); refresh(c); }
    static void scheduleBoundaryRefresh(Context c) {
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Calendar next=Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY,3); next.set(Calendar.MINUTE,0); next.set(Calendar.SECOND,0); next.set(Calendar.MILLISECOND,0);
        if(next.getTimeInMillis()<=System.currentTimeMillis()) next.add(Calendar.DATE,1);
        Intent intent=new Intent(c,HrtBuddyWidget.class).setAction(ACTION_CARE_DAY_REFRESH);
        PendingIntent pending=PendingIntent.getBroadcast(c,90,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(android.os.Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pending);
        else if(android.os.Build.VERSION.SDK_INT>=23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pending);
        else am.set(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pending);
    }
    static void refresh(Context c) { scheduleBoundaryRefresh(c); AppWidgetManager manager=AppWidgetManager.getInstance(c);for(int id:manager.getAppWidgetIds(new ComponentName(c,HrtBuddyWidget.class)))update(c,manager,id); }
}
