package com.jrappspot.cashlipi.activities;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.models.Note;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import java.util.*;

public class NotesActivity extends BaseActivity {
    private DatabaseManager db;
    private RecyclerView rv;
    private LinearLayout emptyState;
    private EditText etSearch;
    private List<Note> allNotes=new ArrayList<>(), filteredNotes=new ArrayList<>();
    private String selectedColor="yellow";

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_notes);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db=DatabaseManager.getInstance(this);
        rv=findViewById(R.id.rvNotes);emptyState=findViewById(R.id.emptyState);
        etSearch=findViewById(R.id.etSearch);
        rv.setLayoutManager(new LinearLayoutManager(this));
        ImageView ivClearN = findViewById(R.id.ivClearSearch);
        etSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){if(ivClearN!=null)ivClearN.setVisibility(s.length()>0?View.VISIBLE:View.GONE);filterNotes(s.toString());}
            public void afterTextChanged(Editable s){}});
        if(ivClearN!=null)ivClearN.setOnClickListener(v->{etSearch.setText("");etSearch.requestFocus();});
        findViewById(R.id.btnAddNote).setOnClickListener(v->showAddNoteDialog(null,-1));
        loadNotes();
    }
    @Override protected void onResume(){super.onResume();loadNotes();}

    private void loadNotes(){
        allNotes=new ArrayList<>(db.getNotesList());
        filteredNotes=new ArrayList<>(allNotes);
        renderNotes();
    }

    private void filterNotes(String q){
        q=q.toLowerCase().trim();filteredNotes=new ArrayList<>();
        for(Note n:allNotes){if(q.isEmpty()||n.getTitle().toLowerCase().contains(q)||n.getContent().toLowerCase().contains(q))filteredNotes.add(n);}
        renderNotes();
    }

    private void renderNotes(){
        if(filteredNotes.isEmpty()){rv.setVisibility(View.GONE);emptyState.setVisibility(View.VISIBLE);return;}
        rv.setVisibility(View.VISIBLE);emptyState.setVisibility(View.GONE);
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>(){
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p,int vt){
                View v=LayoutInflater.from(NotesActivity.this).inflate(R.layout.item_note,p,false);
                return new RecyclerView.ViewHolder(v){};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h,int pos){
                Note n=filteredNotes.get(pos);
                TextView tvTitle=h.itemView.findViewById(R.id.tvNoteTitle);
                TextView tvContent=h.itemView.findViewById(R.id.tvNoteContent);
                TextView tvDate=h.itemView.findViewById(R.id.tvNoteDate);
                tvTitle.setText(n.getTitle().isEmpty()?"(শিরোনাম নেই)":n.getTitle());
                tvContent.setText(n.getContent());
                tvDate.setText(DatabaseManager.formatDateDisplay(n.getDate())+" "+DatabaseManager.formatTimeDisplay(n.getTime()));
                int bgRes;
                switch(n.getColor()){
                    case "blue": bgRes=R.drawable.bg_note_blue;break;
                    case "green":bgRes=R.drawable.bg_note_green;break;
                    case "pink": bgRes=R.drawable.bg_note_pink;break;
                    case "purple":bgRes=R.drawable.bg_note_purple;break;
                    default:bgRes=R.drawable.bg_note_yellow;break;
                }
                h.itemView.setBackground(NotesActivity.this.getResources().getDrawable(bgRes));
                h.itemView.setOnClickListener(v->showAddNoteDialog(n,allNotes.indexOf(n)));
                h.itemView.setOnLongClickListener(v->{
                    int ri=allNotes.indexOf(n);
                    new AlertDialog.Builder(NotesActivity.this,R.style.AppDialog)
                        .setTitle(n.getTitle()).setItems(new String[]{" মুছুন"},(d,w)->{
                            if(ri>=0){db.deleteNote(ri);loadNotes();}
                            Toast.makeText(NotesActivity.this," মুছে গেছে",Toast.LENGTH_SHORT).show();
                        }).show();return true;
                });
            }
            @Override public int getItemCount(){return filteredNotes.size();}
        });
    }

    private void showAddNoteDialog(Note existing, int editIdx){
        View dv=LayoutInflater.from(this).inflate(R.layout.dialog_add_note,null);
        TextInputEditText etTitle=dv.findViewById(R.id.etNoteTitle);
        TextInputEditText etContent=dv.findViewById(R.id.etNoteContent);
        RadioGroup colorGroup=dv.findViewById(R.id.colorGroup);
        if(existing!=null){etTitle.setText(existing.getTitle());etContent.setText(existing.getContent());}
        new AlertDialog.Builder(this,R.style.AppDialog)
            .setTitle(existing==null?" নতুন নোট":" নোট সম্পাদনা")
            .setView(dv)
            .setPositiveButton(existing==null?" সংরক্ষণ করুন":" আপডেট করুন",(d,w)->{
                String title=etTitle.getText()!=null?etTitle.getText().toString().trim():"";
                String content=etContent.getText()!=null?etContent.getText().toString().trim():"";
                if(content.isEmpty()){Toast.makeText(this,"বিষয়বস্তু লিখুন",Toast.LENGTH_SHORT).show();return;}
                int cId=colorGroup.getCheckedRadioButtonId();
                String color="yellow";
                if(cId==R.id.radioBlue)color="blue";
                else if(cId==R.id.radioGreen)color="green";
                else if(cId==R.id.radioPink)color="pink";
                else if(cId==R.id.radioPurple)color="purple";
                if(existing==null){Note n=new Note(title,content,color);db.addNote(n);}
                else{existing.setTitle(title);existing.setContent(content);existing.setColor(color);if(editIdx>=0)db.updateNote(editIdx,existing);}
                loadNotes();Toast.makeText(this," নোট সংরক্ষিত হয়েছে",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("বাতিল",null).show();
    }
}
