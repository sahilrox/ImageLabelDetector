package com.example.imagelabeldetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button uploadBtn, detectBtn;
    private ImageButton cameraBtn;
    private static final int Image_Capture_Code = 1;
    private static final int RESULT_LOAD_IMAGE = 2;
    private static final int SELECT_PICTURE = 100;
    private ImageView image;
    private Bitmap bitmap;
    private Uri uri;
    ArrayList<String> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraBtn = findViewById(R.id.cameraBtn);
        uploadBtn = findViewById(R.id.uploadBtn);
        detectBtn = findViewById(R.id.detectBtn);
        image = findViewById(R.id.capturedImage);
        detectBtn.setEnabled(false);

        arrayList = new ArrayList<>();

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, Image_Capture_Code);
            }
        });

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),SELECT_PICTURE);
            }
        });

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ObjectsActivity.class);
                intent.putExtra("list",arrayList);
                startActivity(intent);
            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        arrayList = new ArrayList<>();
        boolean flag = false;
        if (resultCode == RESULT_OK) {
            if (requestCode == Image_Capture_Code) {
                bitmap = (Bitmap) data.getExtras().get("data");
                image.setImageBitmap(bitmap);
                flag = true;
                detectBtn.setEnabled(true);
            }
            else if (requestCode == SELECT_PICTURE) {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                    image.setImageBitmap(bitmap);
                    detectBtn.setEnabled(true);
                    flag = true;
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (requestCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
            }
        }
        if(flag) {
            FirebaseVisionLabelDetectorOptions options =
                    new FirebaseVisionLabelDetectorOptions.Builder()
                            .setConfidenceThreshold(0.7f)
                            .build();

            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

            FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
                    .getVisionLabelDetector(options);

            Task<List<FirebaseVisionLabel>> result =
                    detector.detectInImage(image)
                            .addOnSuccessListener(
                                    new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                        @Override
                                        public void onSuccess(List<FirebaseVisionLabel> labels) {
                                            for (FirebaseVisionLabel label: labels) {
                                                String text = label.getLabel();
                                                String entityId = label.getEntityId();
                                                float confidence = label.getConfidence();
                                                arrayList.add(text+": "+(int)(confidence*100)+"%");

                                            }
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(MainActivity.this, "Failed to detect", Toast.LENGTH_SHORT).show();
                                        }
                                    });
        }
    }

}