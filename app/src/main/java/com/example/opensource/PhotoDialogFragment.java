package com.example.opensource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;

import java.io.InputStream;

public class PhotoDialogFragment extends DialogFragment {
    private static final String ARG_IMAGE_URI = "image_uri";

    public static PhotoDialogFragment newInstance(String uriString) {
        PhotoDialogFragment fragment = new PhotoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URI, uriString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_photo, container, false);
        ImageView imageView = view.findViewById(R.id.dialog_image);
        Button retryButton = view.findViewById(R.id.retry_button);
        Button confirmButton = view.findViewById(R.id.confirm_button);

        String uriString = getArguments().getString(ARG_IMAGE_URI);
        if (uriString != null) {
            try {
                Uri imageUri = Uri.parse(uriString);

                InputStream stream = requireContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(stream);

                // EXIF에서 회전 정보 읽기
                InputStream exifStream = requireContext().getContentResolver().openInputStream(imageUri);
                ExifInterface exif = new ExifInterface(exifStream);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                int rotation = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:  rotation = 90; break;
                    case ExifInterface.ORIENTATION_ROTATE_180: rotation = 180; break;
                    case ExifInterface.ORIENTATION_ROTATE_270: rotation = 270; break;
                }

                // 회전 보정 적용
                if (rotation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);
                    bitmap = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }

                imageView.setImageBitmap(bitmap);

            } catch (Exception e) {
                Log.e("PhotoDialogFragment", "❌ 미리보기 이미지 로딩 실패", e);
            }
        }

        retryButton.setOnClickListener(v -> {
            dismiss();
            CameraListener listener = (CameraListener) getParentFragment();
            if (listener != null) {
                listener.onRetryCapture();
            }
        });

        confirmButton.setOnClickListener(v -> {
            dismiss();
            CameraListener listener = (CameraListener) getParentFragment();
            if (listener != null) {
                listener.onConfirmCapture(Uri.parse(uriString));
            }
        });

        return view;
    }
}