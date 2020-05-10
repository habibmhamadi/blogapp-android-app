package com.habib.blogapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditUserInfoActivity extends AppCompatActivity {

    private TextInputLayout layoutName,layoutLastname;
    private TextInputEditText txtName,txtLastname;
    private TextView txtSelectPhoto;
    private Button btnSave;
    private CircleImageView circleImageView;
    private static final int GALLERY_CHANGE_PROFILE = 5;
    private Bitmap bitmap = null;
    private SharedPreferences userPref;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_info);
        init();
    }

    private void init() {
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        userPref = getApplicationContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        layoutLastname = findViewById(R.id.txtEditLayoutLastnameameUserInfo);
        layoutName = findViewById(R.id.txtEditLayoutNameUserInfo);
        txtName = findViewById(R.id.txtEditNameUserInfo);
        txtLastname = findViewById(R.id.txtEditLastnameUserInfo);
        txtSelectPhoto = findViewById(R.id.txtEditSelectPhoto);
        btnSave = findViewById(R.id.btnEditSave);
        circleImageView = findViewById(R.id.imgEditUserInfo);

        Picasso.get().load(getIntent().getStringExtra("imgUrl")).into(circleImageView);
        txtName.setText(userPref.getString("name",""));
        txtLastname.setText(userPref.getString("lastname",""));

        txtSelectPhoto.setOnClickListener(v->{
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            startActivityForResult(i,GALLERY_CHANGE_PROFILE);
        });

        btnSave.setOnClickListener(v->{
            if (validate()){
                updateProfile();
            }
        });
    }


    private void updateProfile(){
        dialog.setMessage("Updating");
        dialog.show();
        StringRequest request = new StringRequest(Request.Method.POST,Constant.SAVE_USER_INFO,res->{

            try {
                JSONObject object = new JSONObject(res);
                if (object.getBoolean("success")){
                    SharedPreferences.Editor editor = userPref.edit();
                    editor.putString("name",txtName.getText().toString().trim());
                    editor.putString("lastname",txtLastname.getText().toString().trim());
                    editor.apply();
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            dialog.dismiss();
        },err->{
            err.printStackTrace();
            dialog.dismiss();
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = userPref.getString("token","");
                HashMap<String,String> map = new HashMap<>();
                map.put("Authorization","Bearer "+token);
                return map;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> map = new HashMap<>();
                map.put("name",txtName.getText().toString().trim());
                map.put("lastname",txtLastname.getText().toString().trim());
                map.put("photo",bitmapToString(bitmap));
                return map;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(EditUserInfoActivity.this);
        queue.add(request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==GALLERY_CHANGE_PROFILE && resultCode==RESULT_OK){
           Uri uri = data.getData();

           circleImageView.setImageURI(uri);

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validate(){
        if (txtName.getText().toString().isEmpty()){
            layoutName.setErrorEnabled(true);
            layoutName.setError("Name is Required");
            return false;
        }
        if (txtLastname.getText().toString().isEmpty()){
            layoutLastname.setErrorEnabled(true);
            layoutLastname.setError("Lastname is required");
            return false;
        }

        return true;
    }

    private String bitmapToString(Bitmap bitmap) {
        if (bitmap!=null){
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
            byte [] array = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(array,Base64.DEFAULT);
        }

        return "";
    }
}
