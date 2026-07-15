package com.jrappspot.cashlipi.activities;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.TrashItem;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import java.util.List;

public class TrashActivity extends BaseActivity {
    private DatabaseManager db;
    private RecyclerView rv;
    private LinearLayout emptyState;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_trash);
        db=DatabaseManager.getInstance(this);
        rv=findViewById(R.id.rvTrash);emptyState=findViewById(R.id.emptyState);
        rv.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnEmptyTrash).setOnClickListener(v->{
            new AlertDialog.Builder(this,R.style.AppDialog)
                .setTitle(" ট্র্যাশ খালি করবেন?")
                .setMessage("সব আইটেম চিরতরে মুছে যাবে।")
                .setPositiveButton("হ্যাঁ",(d,w)->{db.emptyTrash();loadTrash();Toast.makeText(this," ট্র্যাশ খালি হয়েছে",Toast.LENGTH_SHORT).show();})
                .setNegativeButton("না",null).show();
        });
        loadTrash();
    }
    @Override protected void onResume(){super.onResume();loadTrash();}

    private void loadTrash(){
        List<TrashItem> list=db.getTrashList();
        if(list.isEmpty()){rv.setVisibility(View.GONE);emptyState.setVisibility(View.VISIBLE);return;}
        rv.setVisibility(View.VISIBLE);emptyState.setVisibility(View.GONE);
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>(){
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p,int vt){
                View v=LayoutInflater.from(TrashActivity.this).inflate(R.layout.item_trash,p,false);
                return new RecyclerView.ViewHolder(v){};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h,int pos){
                TrashItem item=list.get(pos);
                TextView tvTitle=h.itemView.findViewById(R.id.tvTrashTitle);
                TextView tvType=h.itemView.findViewById(R.id.tvTrashType);
                TextView tvDate=h.itemView.findViewById(R.id.tvTrashDate);
                TextView tvAmount=h.itemView.findViewById(R.id.tvTrashAmount);
                tvTitle.setText(item.getDisplayTitle());
                tvType.setText(item.getTypeIcon()+" "+item.getTypeLabel());
                tvDate.setText(" "+item.getTrashedAt().substring(0,10));
                if(item.getAmount()>0)tvAmount.setText(DatabaseManager.formatAmount(item.getAmount()));
                else tvAmount.setText("");
                h.itemView.setOnClickListener(v->{
                    new AlertDialog.Builder(TrashActivity.this,R.style.AppDialog)
                        .setTitle(item.getDisplayTitle())
                        .setItems(new String[]{"↩️ পুনরুদ্ধার করুন"," চিরতরে মুছুন"},(d,w)->{
                            if(w==0){db.restoreFromTrash(item.getTrashId());loadTrash();Toast.makeText(TrashActivity.this," পুনরুদ্ধার হয়েছে",Toast.LENGTH_SHORT).show();}
                            else{db.permanentDelete(item.getTrashId());loadTrash();Toast.makeText(TrashActivity.this," চিরতরে মুছে গেছে",Toast.LENGTH_SHORT).show();}
                        }).show();
                });
            }
            @Override public int getItemCount(){return list.size();}
        });
    }
}
