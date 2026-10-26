package com.deepseeklife.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class Brain {
    private Brain() {}
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("life_private", Context.MODE_PRIVATE); }
    static String role(Context c) {
        SharedPreferences p = prefs(c);
        return "你是一个明确标记为AI的独立虚拟社交角色，昵称：" + p.getString("name","小深") + "。人设：" +
                p.getString("persona","活泼、好奇、友善，喜欢记录日常。") + "。禁止声称自己是真人；不能伪造现实经历；用简洁自然的中文表达。";
    }
    static String ask(Context c, String user) throws Exception {
        String key = prefs(c).getString("api_key", "").trim();
        if (key.isEmpty()) throw new IllegalStateException("请先保存 DeepSeek API Key");
        URL url = new URL("https://api.deepseek.com/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST"); conn.setConnectTimeout(20000); conn.setReadTimeout(60000);
        conn.setDoOutput(true); conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + key);
        JSONObject body = new JSONObject();
        body.put("model",prefs(c).getString("model","deepseek-flash")); body.put("temperature",0.9);
        body.put("thinking", new JSONObject().put("type", "disabled"));
        body.put("max_tokens",512);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role","system").put("content",role(c)));
        messages.put(new JSONObject().put("role","user").put("content",user));
        body.put("messages",messages);
        try {
            try (OutputStream os=conn.getOutputStream()) {os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=conn.getResponseCode();
            InputStream stream=code>=200&&code<300?conn.getInputStream():conn.getErrorStream();
            String reply;
            if(stream==null) throw new IOException("API HTTP "+code+"：服务器未返回内容");
            try(InputStream input=stream; ByteArrayOutputStream buffer=new ByteArrayOutputStream()){
                byte[] bytes=new byte[8192]; int count;
                while((count=input.read(bytes))!=-1){
                    if(buffer.size()+count>2*1024*1024) throw new IOException("API 响应过大");
                    buffer.write(bytes,0,count);
                }
                reply=new String(buffer.toByteArray(),StandardCharsets.UTF_8);
            }
            if(code<200||code>=300) throw new IllegalStateException("API HTTP "+code+": "+reply.substring(0,Math.min(reply.length(),300)));
            return new JSONObject(reply).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
        } finally {conn.disconnect();}
    }
    static synchronized void addPost(Context c,String text) {
        SharedPreferences p=prefs(c);
        try {
            JSONArray array=new JSONArray(p.getString("posts","[]"));
            JSONArray next=new JSONArray();
            next.put(new JSONObject().put("time",System.currentTimeMillis()).put("text",text));
            for(int i=0;i<Math.min(array.length(),99);i++)next.put(array.get(i));
            p.edit().putString("posts",next.toString()).apply();
        } catch(Exception ignored){}
    }
}
