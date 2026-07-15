package com.jrappspot.cashlipi.activities;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AiChatActivity extends BaseActivity {
    private RecyclerView rvChat;
    private TextInputEditText etInput;
    private List<String[]> messages=new ArrayList<>();// [0]=role, [1]=content
    private Executor executor=Executors.newSingleThreadExecutor();
    private DatabaseManager db;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_ai_chat);
        db=DatabaseManager.getInstance(this);
        rvChat=findViewById(R.id.rvChat);etInput=findViewById(R.id.etInput);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        ((LinearLayoutManager)rvChat.getLayoutManager()).setStackFromEnd(true);
        findViewById(R.id.btnSend).setOnClickListener(v->sendMessage());
        addBotMessage("আমি CashLipi ক্যাশলিপি AI সহায়তা। আপনার আর্থিক তথ্য বিশ্লেষণ করে সাহায্য করতে পারি।\n\nআপনার ব্যালেন্স: "+DatabaseManager.formatAmount(db.getBalance()));
    }

    private void sendMessage(){
        String text=etInput.getText()!=null?etInput.getText().toString().trim():"";
        if(text.isEmpty())return;
        addUserMessage(text);etInput.setText("");
        // Build context with financial data
        String context="CashLipi ক্যাশলিপি তথ্য:\n"
            +"মোট আয়: "+DatabaseManager.formatAmount(db.getTotalIncome())+"\n"
            +"মোট ব্যয়: "+DatabaseManager.formatAmount(db.getTotalExpense())+"\n"
            +"ব্যালেন্স: "+DatabaseManager.formatAmount(db.getBalance())+"\n"
            +"মোট সঞ্চয়: "+DatabaseManager.formatAmount(db.getTotalSavings())+"\n"
            +"দেনা: "+DatabaseManager.formatAmount(db.getTotalDena())+"\n"
            +"পাওনা: "+DatabaseManager.formatAmount(db.getTotalPabona())+"\n"
            +"আর্থিক স্বাস্থ্য স্কোর: "+db.calcHealthScore()+"/100\n\n";
        addBotMessage("ভাবছে...");
        final String prompt=context+"প্রশ্ন: "+text;
        executor.execute(()->{
            try{
                String resp=callClaudeApi(prompt);
                runOnUiThread(()->{
                    if(!messages.isEmpty()&&"bot".equals(messages.get(messages.size()-1)[0])&&"ভাবছে...".equals(messages.get(messages.size()-1)[1]))
                        messages.remove(messages.size()-1);
                    addBotMessage(resp);
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(!messages.isEmpty())"ভাবছে...".equals(messages.get(messages.size()-1)[1]);
                    {if(!messages.isEmpty())messages.remove(messages.size()-1);}
                    addBotMessage(" উত্তর পেতে ব্যর্থ হয়েছে। ইন্টারনেট সংযোগ পরীক্ষা করুন।");
                });
            }
        });
    }

    private String callClaudeApi(String userMsg) throws Exception{
        URL url=new URL("https://api.anthropic.com/v1/messages");
        HttpURLConnection conn=(HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/json");
        conn.setRequestProperty("x-api-key","YOUR_API_KEY_HERE");
        conn.setRequestProperty("anthropic-version","2023-06-01");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);conn.setReadTimeout(30000);
        JSONObject body=new JSONObject();
        body.put("model","claude-haiku-4-5-20251001");
        body.put("max_tokens",500);
        JSONArray msgs=new JSONArray();
        JSONObject msg=new JSONObject();msg.put("role","user");msg.put("content","আপনি CashLipi ক্যাশলিপি এর AI সহায়তা। সংক্ষিপ্ত বাংলায় উত্তর দিন। "+userMsg);
        msgs.put(msg);body.put("messages",msgs);
        try(OutputStream os=conn.getOutputStream()){os.write(body.toString().getBytes("UTF-8"));}
        int code=conn.getResponseCode();
        InputStream is=code==200?conn.getInputStream():conn.getErrorStream();
        BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8"));
        StringBuilder sb=new StringBuilder();String line;
        while((line=br.readLine())!=null)sb.append(line);
        JSONObject resp=new JSONObject(sb.toString());
        return resp.getJSONArray("content").getJSONObject(0).getString("text");
    }

    private void addUserMessage(String text){
        messages.add(new String[]{"user",text});
        renderChat();
    }
    private void addBotMessage(String text){
        messages.add(new String[]{"bot",text});
        renderChat();
    }

    private void renderChat(){
        rvChat.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>(){
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p,int vt){
                View v=LayoutInflater.from(AiChatActivity.this).inflate(
                    vt==0?R.layout.item_chat_user:R.layout.item_chat_bot,p,false);
                return new RecyclerView.ViewHolder(v){};
            }
            @Override public int getItemViewType(int pos){return "user".equals(messages.get(pos)[0])?0:1;}
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h,int pos){
                TextView tv=h.itemView.findViewById(R.id.tvMessage);
                if(tv!=null)tv.setText(messages.get(pos)[1]);
            }
            @Override public int getItemCount(){return messages.size();}
        });
        rvChat.scrollToPosition(messages.size()-1);
    }
}
