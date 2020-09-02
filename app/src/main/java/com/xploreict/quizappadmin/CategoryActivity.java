package com.xploreict.quizappadmin;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class CategoryActivity extends AppCompatActivity {

    // Write a message to the database
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private Dialog loadingDialog,categoryDialog;
    private CircleImageView addImage;
    private EditText categoryname;
    private Button addbtn;
    private Uri image;
    private String downloadUri;


    private RecyclerView recyclerView;
    public static List<CategoriesModel> list;

    private CategoryAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        Toolbar toolbar = findViewById(R.id.quistion_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Categories");

        //checking internet connection........

        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (isConnected == false){
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
            alertDialog.setTitle(R.string.no_internet_title);
            alertDialog.setMessage(R.string.no_internet_msg);
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });
            alertDialog.show();
            return;
        }

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.btn_shape));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);


        setCategoryDialog();

        recyclerView = findViewById(R.id.rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        list = new ArrayList<>();

//delete in the category activity start here..................................
        adapter = new CategoryAdapter(list, new CategoryAdapter.DeleteLisitner() {
            @Override
            public void ondelete(final String key, final int position) {

                new AlertDialog.Builder(CategoryActivity.this,R.style.Theme_AppCompat_Light_Dialog)
                        .setTitle("Delete Category")
                        .setMessage("Are you sure delete this category?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                loadingDialog.show();

                                myRef.child("Categories").child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()){

                                            for (String setIds : list.get(position).getSets()){
                                                myRef.child("SETS").child(setIds).removeValue();
                                            }

                                            list.remove(position);
                                            adapter.notifyDataSetChanged();
                                            loadingDialog.dismiss();

                                        }else {
                                            Toast.makeText(CategoryActivity.this, "Delete Failed!", Toast.LENGTH_SHORT).show();
                                            loadingDialog.dismiss();
                                        }

                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel",null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });


        recyclerView.setAdapter(adapter);
        loadingDialog.show();
        myRef.child("Categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    List<String> sets = new ArrayList<>();
                    for (DataSnapshot dataSnapshot2 : dataSnapshot1.child("sets").getChildren()){
                        sets.add(dataSnapshot2.getKey());
                    }
                    list.add(new CategoriesModel(dataSnapshot1.child("name").getValue().toString(),
                            sets,
                            dataSnapshot1.child("url").getValue().toString(),
                            dataSnapshot1.getKey()
                    ));
                }
                adapter.notifyDataSetChanged();
                loadingDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CategoryActivity.this,error.getMessage(),Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add){
            categoryDialog.show();
        }
        if (item.getItemId() == R.id.logout){
            new AlertDialog.Builder(CategoryActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                    .setTitle("Logout")
                    .setMessage("Are you sure want to logout?")
                    .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            loadingDialog.show();
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(CategoryActivity.this,MainActivity.class);
                            startActivity(intent);
                            finish();
                                }
                            })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }
    private void setCategoryDialog(){
        categoryDialog = new Dialog(this);
        categoryDialog.setContentView(R.layout.add_category_dialog);
        categoryDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_box));
        categoryDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        categoryDialog.setCancelable(true);

        addImage = categoryDialog.findViewById(R.id.image_add);
        categoryname = categoryDialog.findViewById(R.id.category_name);
        addbtn = categoryDialog.findViewById(R.id.add_btn);

        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent,101);
            }
        });

        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (categoryname.getText() == null || categoryname.getText().toString().isEmpty()){
                    categoryname.setError("Required");
                    return;
                }
                for (CategoriesModel model : list){
                    if(categoryname.getText().toString().equals(model.getName())){
                        categoryname.setError("Category Name already Present");
                        return;
                    }
                }
                if (image == null){
                    Toast.makeText(CategoryActivity.this, "Please Select your Image.", Toast.LENGTH_SHORT).show();
                    return;
                }
                categoryDialog.dismiss();
                //uploading data
                uploadData();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101){
            if (resultCode == RESULT_OK){
                image = data.getData();
                addImage.setImageURI(image);
            }
        }
    }
    private void uploadData(){
        loadingDialog.show();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        final StorageReference ImageReference = storageReference.child("categories").child(image.getLastPathSegment());
        UploadTask uploadTask = ImageReference.putFile(image);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return ImageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()){
                            downloadUri = task.getResult().toString();
                            uploadCategoryName();
                        }else {
                            loadingDialog.dismiss();
                            Toast.makeText(CategoryActivity.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                } else {
                    loadingDialog.dismiss();
                    Toast.makeText(CategoryActivity.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void uploadCategoryName(){
        Map<String,Object> map = new HashMap<>();
        map.put("name",categoryname.getText().toString());
        map.put("sets",0);
        map.put("url",downloadUri);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        final String id = UUID.randomUUID().toString();

        database.getReference().child("Categories").child(id).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    list.add(new CategoriesModel(categoryname.getText().toString(),new ArrayList<String>(),downloadUri,id));
                    adapter.notifyDataSetChanged();

                }else {
                    Toast.makeText(CategoryActivity.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();
            }
        });
    }
}