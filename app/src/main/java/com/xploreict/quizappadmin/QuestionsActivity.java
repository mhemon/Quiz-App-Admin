package com.xploreict.quizappadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.XMLFormatter;

public class QuestionsActivity extends AppCompatActivity {

    private Button add, excel;
    private RecyclerView recyclerView;
    private QuestionAdapter adapter;
    public static List<QuestionModel> list;
    private Dialog loadingDialog;
    private TextView loadingtxt;
    private DatabaseReference myRef;
    private String setId;
    private String categoryName;
    public static final int CELL_COUNT = 6;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myRef = FirebaseDatabase.getInstance().getReference();

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.btn_shape));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);
        loadingtxt = loadingDialog.findViewById(R.id.textView3);

        categoryName = getIntent().getStringExtra("category");
        setId = getIntent().getStringExtra("setId");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(categoryName);

        add = findViewById(R.id.add_btn);
        excel = findViewById(R.id.excel_btn);
        recyclerView = findViewById(R.id.recycle_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);


        list = new ArrayList<>();
        adapter = new QuestionAdapter(list, categoryName, new QuestionAdapter.DeleteListiner() {
            @Override
            public void onLongclick(final int position, final String id) {
                new AlertDialog.Builder(QuestionsActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                        .setTitle("Delete Question")
                        .setMessage("Are you sure delete this question?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                loadingDialog.show();

                                myRef.child("SETS").child(setId).child(id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            list.remove(position);
                                            adapter.notifyItemRemoved(position);
                                        } else {
                                            Toast.makeText(QuestionsActivity.this, "Delete Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                        loadingDialog.dismiss();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        recyclerView.setAdapter(adapter);


        getData(categoryName, setId);


        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addquestion = new Intent(QuestionsActivity.this, AddQuestionActivity.class);
                addquestion.putExtra("categoryName", categoryName);
                addquestion.putExtra("setId", setId);
                startActivity(addquestion);
            }
        });


        excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(QuestionsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    selectFile();
                } else {
                    ActivityCompat.requestPermissions(QuestionsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFile();
            } else {
                Toast.makeText(this, "Please Grant Permissions!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), 102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 102) {
            if (resultCode == RESULT_OK) {
                String filepath = data.getData().getPath();
                if (filepath.endsWith(".xlsx")) {
                    readFile(data.getData());
                } else {
                    Toast.makeText(this, "please choose an Excel file!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void readFile(Uri fileuri) {
        loadingtxt.setText("scannig Questions...");
        loadingDialog.show();
        final HashMap<String, Object> parentMap = new HashMap<>();
        final List<QuestionModel> tempList = new ArrayList<>();

        try {
            InputStream inputStream = getContentResolver().openInputStream(fileuri);
            try {
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                XSSFSheet sheet = workbook.getSheetAt(0);
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

                int rowscont = sheet.getPhysicalNumberOfRows();

                if (rowscont > 0) {

                    for (int r = 0; r < rowscont; r++) {
                        Row row = sheet.getRow(r);
                        if (row.getPhysicalNumberOfCells() == CELL_COUNT) {
                            String question = getcellData(row, 0, formulaEvaluator);
                            String a = getcellData(row, 1, formulaEvaluator);
                            String b = getcellData(row, 2, formulaEvaluator);
                            String c = getcellData(row, 3, formulaEvaluator);
                            String d = getcellData(row, 4, formulaEvaluator);
                            String correctAns = getcellData(row, 5, formulaEvaluator);

                            if (correctAns.equals(a) || correctAns.equals(b) || correctAns.equals(c) || correctAns.equals(d)) {

                                HashMap<String, Object> questionMap = new HashMap<>();
                                questionMap.put("question", question);
                                questionMap.put("optionA", a);
                                questionMap.put("optionB", b);
                                questionMap.put("optionC", c);
                                questionMap.put("optionD", d);
                                questionMap.put("correctANS", correctAns);
                                questionMap.put("setId", setId);

                                String id = UUID.randomUUID().toString();

                                parentMap.put(id, questionMap);

                                tempList.add(new QuestionModel(id, question, a, b, c, d, correctAns, setId));

                            } else {
                                loadingtxt.setText("Loading...");
                                loadingDialog.dismiss();
                                Toast.makeText(QuestionsActivity.this, "Row no. " + (r+1) + " has no correct option", Toast.LENGTH_SHORT).show();
                                return;
                            }

                        } else {
                            loadingtxt.setText("Loading...");
                            loadingDialog.dismiss();
                            Toast.makeText(QuestionsActivity.this, "Row no. " + (r+1) + " has incorrect data", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //previous return
                    }

                    loadingtxt.setText("Uploading...");
                    FirebaseDatabase.getInstance().getReference()
                            .child("SETS").child(setId).updateChildren(parentMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                list.addAll(tempList);
                                adapter.notifyDataSetChanged();
                            } else {
                                loadingtxt.setText("Loading...");
                                Toast.makeText(QuestionsActivity.this, "something went wrong!", Toast.LENGTH_SHORT).show();
                            }
                            loadingDialog.dismiss();
                        }
                    });

                } else {
                    loadingtxt.setText("Loading...");
                    loadingDialog.dismiss();
                    Toast.makeText(QuestionsActivity.this, "File is Empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

            } catch (final IOException e) {
                e.printStackTrace();

                loadingtxt.setText("Loading...");
                loadingDialog.dismiss();
                Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            loadingDialog.dismiss();
            loadingtxt.setText("Loading...");
            Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        }
    }

    private String getcellData(Row row, int cellposition, FormulaEvaluator formulaEvaluator) {
        String value = "";
        Cell cell = row.getCell(cellposition);
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN:
                return value + cell.getBooleanCellValue();

            case Cell.CELL_TYPE_NUMERIC:
                return value + cell.getNumericCellValue();

            case Cell.CELL_TYPE_STRING:
                return value + cell.getStringCellValue();

            default:
                return value;

        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getData(String categoryName, final String setId) {
        loadingDialog.show();
        myRef
                .child("SETS").child(setId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    String id = dataSnapshot1.getKey();
                    String question = dataSnapshot1.child("question").getValue().toString();
                    String a = dataSnapshot1.child("optionA").getValue().toString();
                    String b = dataSnapshot1.child("optionB").getValue().toString();
                    String c = dataSnapshot1.child("optionC").getValue().toString();
                    String d = dataSnapshot1.child("optionD").getValue().toString();
                    String correctAns = dataSnapshot1.child("correctANS").getValue().toString();

                    list.add(new QuestionModel(id, question, a, b, c, d, correctAns, setId));
                }
                loadingDialog.dismiss();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuestionsActivity.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        adapter.notifyDataSetChanged();
    }
}