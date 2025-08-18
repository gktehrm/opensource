package com.example.opensource.camera;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;

import com.example.opensource.R;

public class PhotoPreviewDialogFragment extends DialogFragment {
    private Bitmap previewBitmap;

    public static PhotoPreviewDialogFragment newInstance(Bitmap bitmap) {
        PhotoPreviewDialogFragment fragment = new PhotoPreviewDialogFragment();
        fragment.previewBitmap = bitmap;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_photo, container, false);
        ImageView imageView = view.findViewById(R.id.dialog_image);
        Button retryButton = view.findViewById(R.id.retry_button);
        Button confirmButton = view.findViewById(R.id.confirm_button);

        imageView.setImageBitmap(previewBitmap);

        retryButton.setOnClickListener(v -> {
            dismiss();
            CameraListener listener = (CameraListener) getTargetFragment();
            if (listener != null) listener.onRetryCapture();
        });

        confirmButton.setOnClickListener(v -> {
            if (getTargetFragment() instanceof CameraListener) {
                ((CameraListener) getTargetFragment()).onConfirmCapture(previewBitmap);
            }
            dismiss();
        });

        return view;
    }
}
