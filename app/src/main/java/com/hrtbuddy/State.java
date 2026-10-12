package com.hrtbuddy;

import android.content.*;
import java.time.*;

final class State {
    static final String PREF = "hrtbuddy";
    static LocalDate careDate() { LocalDate calendarDate=LocalDate.now(); return java.time.LocalTime.now().isBefore(java.time.LocalTime.of(3,0))?calendarDate.minusDays(1):calendarDate; }
    static SharedPreferences p(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
    static void migrate(Context c) { SharedPreferences x=p(c);int version=x.getInt("schemaVersion",0);if(version<1){SharedPreferences.Editor e=x.edit();if(!x.contains("cycle"))e.putInt("cycle",28);if(!x.contains("anchorDay"))e.putInt("anchorDay",1);if(!x.contains("progStart"))e.putInt("progStart",15);if(!x.contains("progEnd"))e.putInt("progEnd",26);if(!x.contains("patchDays"))e.putString("patchDays","TUE,SAT");if(!x.contains("patchTime"))e.putString("patchTime","10:00");if(!x.contains("progTime"))e.putString("progTime","22:00");if(!x.contains("checkinTime"))e.putString("checkinTime","20:00");e.putInt("schemaVersion",1).apply();} }
    static int cycle(Context c) { return p(c).getInt("cycle", 28); }
    static int day(Context c) { return day(c,careDate()); }
    static LocalDate cycleStart(Context c) { SharedPreferences x=p(c); long anchor=x.getLong("anchor",System.currentTimeMillis()); LocalDate anchorDate=Instant.ofEpochMilli(anchor).atZone(ZoneId.systemDefault()).toLocalDate(); return anchorDate.minusDays(x.getInt("anchorDay",1)-1L); }
    static int historyCycles(Context c) { LocalDate start=cycleStart(c), oldest=null; int records=0; for(String key:p(c).getAll().keySet()){if(key.startsWith("cycle_len_")){try{LocalDate recorded=LocalDate.parse(key.substring("cycle_len_".length()));if(recorded.isBefore(start))records++;}catch(Exception ignored){}}String raw=null;if(key.startsWith("log_")&&key.length()>14){int end=key.indexOf('_',4);if(end>4)raw=key.substring(4,end);}else if((key.startsWith("done_")||key.startsWith("skip_"))&&key.length()>5)raw=key.substring(5);if(raw!=null)try{LocalDate d=LocalDate.parse(raw);if(d.isBefore(start)&&(oldest==null||d.isBefore(oldest)))oldest=d;}catch(Exception ignored){}} if(oldest==null)return Math.max(1,records+1);long days=start.toEpochDay()-oldest.toEpochDay();return Math.max(1,Math.min(120,Math.max(records+1,(int)(days/cycle(c))+1))); }
    static int day(Context c, LocalDate date) { SharedPreferences x=p(c); long anchor=x.getLong("anchor", System.currentTimeMillis()); int d=x.getInt("anchorDay",1); long n=date.toEpochDay()-Instant.ofEpochMilli(anchor).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay(); return (int)Math.floorMod(d-1+n, cycle(c))+1; }
    static String status(Context c) { LocalDate today=careDate();boolean any=false,outstanding=false;if(scheduled(c,today,"patch")){any=true;outstanding|=!confirmed(c,"patch");}int d=day(c),ps=p(c).getInt("progStart",15),pe=p(c).getInt("progEnd",26);if(d>=ps&&d<=pe){any=true;outstanding|=!confirmed(c,"pill");}if(trackBleeding(c)||trackPain(c)||trackMood(c)||trackOverall(c)){any=true;boolean logged=dailyInt(c,today,"pain")>=0||dailyInt(c,today,"mood")>=0||dailyInt(c,today,"overall")>=0||!dailyText(c,today,"bleeding").isEmpty()||confirmed(c,"checkin");outstanding|=!logged;}return outstanding?"Tasks due today":any?"All tasks complete today":"No tasks due today"; }
    static boolean trackPain(Context c) { return p(c).getBoolean("trackPain",false); }
    static boolean trackMood(Context c) { return p(c).getBoolean("trackMood",false); }
    static boolean trackOverall(Context c) { return p(c).getBoolean("trackOverall",false); }
    static boolean trackBleeding(Context c) { return p(c).getBoolean("trackBleeding",true); }
    static boolean showMap(Context c,String type) { return p(c).getBoolean("show_"+type,true); }
    static int dailyInt(Context c,LocalDate date,String key) { return p(c).getInt("log_"+date+"_"+key,-1); }
    static String dailyText(Context c,LocalDate date,String key) { return p(c).getString("log_"+date+"_"+key,""); }
    static boolean hasDailyData(Context c,String type) { for(String key:p(c).getAll().keySet()) if(key.startsWith("log_")&&key.endsWith("_"+type)) return true; return false; }
    static void saveDaily(Context c,LocalDate date,int pain,int mood,int overall,String bleeding) { SharedPreferences.Editor e=p(c).edit(); if(pain>=0)e.putInt("log_"+date+"_pain",pain);if(mood>=0)e.putInt("log_"+date+"_mood",mood);if(overall>=0)e.putInt("log_"+date+"_overall",overall);if(bleeding!=null&&!bleeding.isEmpty())e.putString("log_"+date+"_bleeding",bleeding);else e.remove("log_"+date+"_bleeding");e.apply(); HrtBuddyWidget.refresh(c); }
    static boolean confirmed(Context c,String key) { return confirmedOn(c,careDate(),key); }
    static boolean confirmedOn(Context c,LocalDate date,String key) { return p(c).getStringSet("done_"+date,java.util.Collections.emptySet()).contains(key); }
    static boolean skippedOn(Context c,LocalDate date,String key) { return p(c).getStringSet("skip_"+date,java.util.Collections.emptySet()).contains(key); }
    static void finish(Context c,String key,boolean skipped) { String k=(skipped?"skip_":"done_")+careDate(); java.util.Set<String> s=new java.util.HashSet<>(p(c).getStringSet(k,java.util.Collections.emptySet())); s.add(key); p(c).edit().putStringSet(k,s).apply(); HrtBuddyWidget.refresh(c); }
    static void confirm(Context c,String key) { finish(c,key,false); }
    static void unfinish(Context c,String key) { LocalDate today=careDate();String k="done_"+today; java.util.Set<String> s=new java.util.HashSet<>(p(c).getStringSet(k,java.util.Collections.emptySet())); s.remove(key); java.util.Set<String> skipped=new java.util.HashSet<>(p(c).getStringSet("skip_"+today,java.util.Collections.emptySet()));skipped.remove(key);p(c).edit().putStringSet(k,s).putStringSet("skip_"+today,skipped).apply(); HrtBuddyWidget.refresh(c); }
    static void resetAll(Context c) { p(c).edit().clear().apply(); ((android.app.NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll(); ReminderReceiver.schedule(c); HrtBuddyWidget.refresh(c); }
    static boolean scheduled(Context c,LocalDate date,String key) { if(key.equals("checkin")) return trackBleeding(c)||trackPain(c)||trackMood(c)||trackOverall(c); int d=day(c,date); if(key.equals("pill")) return d>=p(c).getInt("progStart",15)&&d<=p(c).getInt("progEnd",26); String names=p(c).getString("patchDays","TUE,SAT"); return names.contains(date.getDayOfWeek().toString().substring(0,3)); }
    static void save(Context c,int cycle,int today,int ps,int pe,String patchDays,String patchTime) { save(c,cycle,today,ps,pe,patchDays,patchTime,p(c).getString("progTime","22:00"),p(c).getString("checkinTime","20:00")); }
    static void save(Context c,int cycle,int today,int ps,int pe,String patchDays,String patchTime,String progTime) { save(c,cycle,today,ps,pe,patchDays,patchTime,progTime,p(c).getString("checkinTime","20:00")); }
    static void save(Context c,int cycle,int today,int ps,int pe,String patchDays,String patchTime,String progTime,String checkinTime) { SharedPreferences old=p(c); int previous=old.getInt("cycle",cycle); android.content.SharedPreferences.Editor e=p(c).edit(); int effectiveToday=today; if(previous!=cycle&&old.contains("anchor")){LocalDate oldStart=cycleStart(c),newStart=careDate();e.putInt("cycle_len_"+oldStart,previous).putString("cycle_prev_"+newStart,oldStart.toString());effectiveToday=1;} LocalDate newStart=careDate().minusDays(effectiveToday-1L); e.putInt("cycle_len_"+newStart,cycle).putInt("cycle",cycle).putInt("anchorDay",effectiveToday).putLong("anchor",System.currentTimeMillis()).putInt("progStart",ps).putInt("progEnd",pe).putString("patchDays",patchDays).putString("patchTime",patchTime).putString("progTime",progTime).putString("checkinTime",checkinTime).apply(); ReminderReceiver.schedule(c); HrtBuddyWidget.refresh(c); }
    static int cycleLengthAt(Context c,LocalDate start){return p(c).getInt("cycle_len_"+start,cycle(c));}
    static void clearDateRange(Context c,LocalDate from,LocalDate through){android.content.SharedPreferences.Editor e=p(c).edit();for(String key:p(c).getAll().keySet()){String raw=null;if(key.startsWith("log_")&&key.length()>14){int end=key.indexOf('_',4);if(end>4)raw=key.substring(4,end);}else if((key.startsWith("done_")||key.startsWith("skip_"))&&key.length()>5)raw=key.substring(5);if(raw!=null)try{LocalDate d=LocalDate.parse(raw);if(!d.isBefore(from)&&!d.isAfter(through))e.remove(key);}catch(Exception ignored){}}e.apply();}
    static void markSkippedCycleDays(Context c,int fromDay,int throughDay){android.content.SharedPreferences.Editor e=p(c).edit();LocalDate today=careDate();for(int d=fromDay;d<=throughDay;d++){LocalDate date=today.minusDays(State.day(c)-d);java.util.Set<String> set=new java.util.HashSet<>(p(c).getStringSet("skip_"+date,java.util.Collections.emptySet()));if(scheduled(c,date,"patch"))set.add("patch");if(scheduled(c,date,"pill"))set.add("pill");if(scheduled(c,date,"checkin"))set.add("checkin");if(!set.isEmpty())e.putStringSet("skip_"+date,set);}e.apply();}
    static void restoreDemoIfNeeded(Context c){String backup=p(c).getString("demoBackup","");if(backup.isEmpty())return;try{BackupManager.importCsv(c,new java.io.ByteArrayInputStream(backup.getBytes("UTF-8")));p(c).edit().remove("demoBackup").remove("demoData").apply();}catch(Exception ex){android.widget.Toast.makeText(c,"Could not restore real data; demo data is still active",android.widget.Toast.LENGTH_LONG).show();}}
    static void loadDemoData(Context c) {
        String backup=BackupManager.csv(c); if(!p(c).contains("demoBackup"))p(c).edit().putString("demoBackup",backup).apply(); BackupManager.clearLogs(c);
        int cycle=28, todayDay=14;
        LocalDate today=careDate(), currentStart=today.minusDays(todayDay-1L);
        SharedPreferences.Editor e=p(c).edit().putInt("cycle",cycle).putInt("anchorDay",todayDay).putLong("anchor",System.currentTimeMillis()).putInt("progStart",15).putInt("progEnd",26).putString("patchDays","TUE,SAT").putString("patchTime","10:00").putString("progTime","22:00").putBoolean("trackPain",true).putBoolean("trackMood",true).putBoolean("trackOverall",true).putBoolean("demoData",true);
        String[] bleeding={"", "Light", "Medium", "Heavy", "Light", "Spotting", ""};
        for(int cycleIndex=9;cycleIndex>=0;cycleIndex--){
            LocalDate start=currentStart.minusDays((long)cycleIndex*cycle);
            for(int day=1;day<=cycle;day++){
                LocalDate date=start.plusDays(day-1L);
                int pain=2+((day*3+cycleIndex)%7), mood=4+((day+cycleIndex*2)%6), overall=3+((day*2+cycleIndex)%7);
                if(day<=5){ pain=Math.min(10,pain+2); mood=Math.max(1,mood-1); }
                e.putInt("log_"+date+"_pain",pain).putInt("log_"+date+"_mood",mood).putInt("log_"+date+"_overall",overall);
                String bleed=day<=6?bleeding[(day+cycleIndex)%bleeding.length]:""; if(!bleed.isEmpty())e.putString("log_"+date+"_bleeding",bleed);
                java.util.Set<String> done=new java.util.HashSet<>(); java.util.Set<String> skipped=new java.util.HashSet<>();
                boolean patch=date.getDayOfWeek()==DayOfWeek.TUESDAY||date.getDayOfWeek()==DayOfWeek.SATURDAY;
                if(patch&&!date.isAfter(today)){if(cycleIndex==3&&day==4)skipped.add("patch");else done.add("patch");}
                if(day>=15&&day<=26&&!date.isAfter(today)){if((cycleIndex==6&&day==19)||(cycleIndex==2&&day==23))skipped.add("pill");else done.add("pill");}
                if(!done.isEmpty())e.putStringSet("done_"+date,done); if(!skipped.isEmpty())e.putStringSet("skip_"+date,skipped);
            }
        }
        e.apply(); ReminderReceiver.schedule(c); HrtBuddyWidget.refresh(c);
    }
}
