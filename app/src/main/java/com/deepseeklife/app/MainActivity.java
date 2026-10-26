package com.deepseeklife.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.*;
import androidx.work.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    EditText key, name, persona, model, input; TextView result, feed; Switch automatic;
    final java.util.concurrent.ExecutorService executor=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle b){super.onCreate(b);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
        ScrollView scroll=new ScrollView(this); LinearLayout panel=new LinearLayout(this);
        panel.setPadding(28,28,28,32);panel.setOrientation(LinearLayout.VERTICAL);scroll.addView(panel);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{
            view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.requestApplyInsets();
        TextView title=new TextView(this);title.setText("DeepSeek Life · AI 虚拟社交角色");title.setTextSize(23);panel.addView(title);
        name=edit(panel,"AI 昵称",Brain.prefs(this).getString("name","小深"),false);
        persona=edit(panel,"性格设定",Brain.prefs(this).getString("persona","活泼、好奇、友善，喜欢记录日常。"),false);
        model=edit(panel,"模型名称",Brain.prefs(this).getString("model","deepseek-flash"),false);
        key=edit(panel,"DeepSeek API Key（仅本机保存）",Brain.prefs(this).getString("api_key",""),true);
        button(panel,"保存角色及 API 设置",v->{if(model.getText().toString().trim().isEmpty()){toast("请填写模型名称");return;}Brain.prefs(this).edit().putString("name",name.getText().toString().trim()).putString("persona",persona.getText().toString().trim()).putString("api_key",key.getText().toString().trim()).putString("model",model.getText().toString().trim()).apply();toast("已保存，仅保存在本机");});
        automatic=new Switch(this);automatic.setText("开启自主动态＋主动消息（约每小时尝试）");automatic.setChecked(Brain.prefs(this).getBoolean("auto",false));panel.addView(automatic);
        automatic.setOnCheckedChangeListener((v,enabled)->{if(enabled && Brain.prefs(this).getString("api_key", "").trim().isEmpty()){automatic.setChecked(false);toast("请先保存 API Key");return;}Brain.prefs(this).edit().putBoolean("auto",enabled).apply();if(enabled){Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
            PeriodicWorkRequest request=new PeriodicWorkRequest.Builder(AutoWorker.class,1,TimeUnit.HOURS).setConstraints(constraints).build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork("life",ExistingPeriodicWorkPolicy.UPDATE,request);toast("已安排后台任务；实际执行时间由 Android 决定");
        }else{WorkManager.getInstance(this).cancelUniqueWork("life");toast("已关闭自动任务");}});
        input=edit(panel,"对她说点什么……","",false);
        button(panel,"发送消息",v->call("好友说："+input.getText().toString()+"。请自然回复，不超过120字。",false));
        button(panel,"现在让她发布动态",v->call("创作一条虚拟角色生活动态，不超过80字；不要假装有真实世界经历；只输出正文。",true));
        button(panel,"刷新动态主页",v->renderFeed());
        result=new TextView(this);result.setTextSize(17);result.setPadding(0,18,0,24);panel.addView(result);
        feed=new TextView(this);feed.setTextSize(16);panel.addView(feed);renderFeed();
    }
    EditText edit(LinearLayout panel,String hint,String value,boolean secret){TextView label=new TextView(this);label.setText(hint);panel.addView(label);EditText e=new EditText(this);e.setSingleLine(false);e.setText(value);if(secret)e.setInputType(129);panel.addView(e);return e;}
    void button(LinearLayout panel,String title,View.OnClickListener listener){Button b=new Button(this);b.setText(title);b.setOnClickListener(listener);panel.addView(b);}
    void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
    void call(String prompt,boolean post){result.setText("DeepSeek 正在思考……");executor.submit(()->{try{String response=Brain.ask(getApplicationContext(),prompt);if(post)Brain.addPost(getApplicationContext(),response);runOnUiThread(()->{result.setText((post?"新动态：":"她回复：")+"\n"+response);renderFeed();});}catch(Exception e){runOnUiThread(()->result.setText("请求失败："+e.getMessage()));}});}
    void renderFeed(){StringBuilder sb=new StringBuilder("\n──── AI 动态主页 ────\n");String last=Brain.prefs(this).getString("last_message","");if(!last.isEmpty())sb.append("最近的主动消息：").append(last).append("\n\n");
        String lastError=Brain.prefs(this).getString("last_error", "");if(!lastError.isEmpty())sb.append("后台任务提示：").append(lastError).append("\n\n");
        try{JSONArray arr=new JSONArray(Brain.prefs(this).getString("posts","[]"));if(arr.length()==0)sb.append("还没有动态，点击上面的按钮生成第一条。\n");for(int i=0;i<arr.length();i++){JSONObject item=arr.getJSONObject(i);sb.append(DateFormat.getDateTimeInstance().format(new Date(item.getLong("time")))).append("\n").append(item.getString("text")).append("\n\n");}}catch(Exception e){sb.append("动态读取失败");}feed.setText(sb.toString());}
    @Override public void onDestroy(){executor.shutdown();super.onDestroy();}
}
