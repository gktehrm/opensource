package com.example.opensource.camera;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.opensource.R;
import org.opencv.android.OpenCVLoader;


public class CameraActivity extends AppCompatActivity {
    static {
        System.loadLibrary("opencv_java4");
        Log.d("OpenCV", "OpenCV native lib loaded");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.camera_fragment_container, new CameraXFragment())
                    .commit();
        }
    }
}
