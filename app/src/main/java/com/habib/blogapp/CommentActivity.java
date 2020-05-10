package com.habib.blogapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.habib.blogapp.Adapters.CommentsAdapter;
import com.habib.blogapp.Fragments.HomeFragment;
import com.habib.blogapp.Models.Comment;
import com.habib.blogapp.Models.Post;
import com.habib.blogapp.Models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CommentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<Comment> list;
    private CommentsAdapter adapter;
    private int postId = 0;
    public  static  int postPosition = 0;
    private SharedPreferences preferences;
    private EditText txtAddComment;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        init();
    }

    private void init() {
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        postPosition = getIntent().getIntExtra("postPosition",-1);
        preferences = getApplicationContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        recyclerView = findViewById(R.id.recyclerComments);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        txtAddComment = findViewById(R.id.txtAddComment);
        postId = getIntent().getIntExtra("postId",0);
        getComments();
    }

    private void getComments() {
        list = new ArrayList<>();
        StringRequest request = new StringRequest(Request.Method.POST,Constant.COMMENTS,res->{

            try {
                JSONObject object = new JSONObject(res);
                if (object.getBoolean("success")){
                    JSONArray comments = new JSONArray(object.getString("comments"));
                    for (int i = 0; i < comments.length(); i++) {
                        JSONObject comment = comments.getJSONObject(i);
                        JSONObject user = comment.getJSONObject("user");

                        User mUser = new User();
                        mUser.setId(user.getInt("id"));
                        mUser.setPhoto(Constant.URL+"storage/profiles/"+user.getString("photo"));
                        mUser.setUserName(user.getString("name")+" "+user.getString("lastname"));

                        Comment mComment = new Comment();
                        mComment.setId(comment.getInt("id"));
                        mComment.setUser(mUser);
                        mComment.setDate(comment.getString("created_at"));
                        mComment.setComment(comment.getString("comment"));
                        list.add(mComment);
                    }
                }

                adapter = new CommentsAdapter(this,list);
                recyclerView.setAdapter(adapter);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        },error -> {
            error.printStackTrace();
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = preferences.getString("token","");
                HashMap<String,String> map = new HashMap<>();
                map.put("Authorization","Bearer "+token);
                return map;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> map = new HashMap<>();
                map.put("id",postId+"");
                return map;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(CommentActivity.this);
        queue.add(request);
    }

    public void goBack(View view) {
        super.onBackPressed();
    }

    public void addComment(View view) {
        String commentText = txtAddComment.getText().toString();
        dialog.setMessage("Adding comment");
        dialog.show();
        if (commentText.length()>0){
            StringRequest request = new StringRequest(Request.Method.POST,Constant.CREATE_COMMENT,res->{

                try {
                    JSONObject object = new JSONObject(res);
                    if (object.getBoolean("success")){
                        JSONObject comment = object.getJSONObject("comment");
                        JSONObject user = comment.getJSONObject("user");

                        Comment c = new Comment();
                        User u = new User();
                        u.setId(user.getInt("id"));
                        u.setUserName(user.getString("name")+" "+user.getString("lastname"));
                        u.setPhoto(Constant.URL+"storage/profiles/"+user.getString("photo"));
                        c.setUser(u);
                        c.setId(comment.getInt("id"));
                        c.setDate(comment.getString("created_at"));
                        c.setComment(comment.getString("comment"));

                        Post post = HomeFragment.arrayList.get(postPosition);
                        post.setComments(post.getComments()+1);
                        HomeFragment.arrayList.set(postPosition,post);
                        HomeFragment.recyclerView.getAdapter().notifyDataSetChanged();

                        list.add(c);
                        recyclerView.getAdapter().notifyDataSetChanged();
                        txtAddComment.setText("");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();

            },err->{
                err.printStackTrace();
                dialog.dismiss();
            }){
                //add token to header

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    String token = preferences.getString("token","");
                    HashMap<String,String> map = new HashMap<>();
                    map.put("Authorization","Bearer "+token);
                    return map;
                }

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String,String> map = new HashMap<>();
                    map.put("id",postId+"");
                    map.put("comment",commentText);
                    return map;
                }


            };
            RequestQueue queue = Volley.newRequestQueue(CommentActivity.this);
            queue.add(request);
        }
    }
}
