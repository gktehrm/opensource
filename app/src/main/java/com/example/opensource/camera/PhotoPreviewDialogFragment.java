package com.example.opensource.camera;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;

import com.example.opensource.R;

public class PhotoPreviewDialogFragment extends DialogFragment {
    private Bitmap originalBitmap; // 원본
    private Bitmap warpedBitmap;   // 보정본(없을 수 있음)

    public static PhotoPreviewDialogFragment newInstance(Bitmap original, Bitmap warped) {
        PhotoPreviewDialogFragment fragment = new PhotoPreviewDialogFragment();
        fragment.originalBitmap = original;
        fragment.warpedBitmap = warped;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_photo, container, false);

        ImageView imageView = view.findViewById(R.id.dialog_image);
        CheckBox warpToggle = view.findViewById(R.id.toggle_warp); // ✅ 추가
        Button retryButton = view.findViewById(R.id.retry_button);
        Button confirmButton = view.findViewById(R.id.confirm_button);

        // 보정본이 없으면 토글 숨기고 원본만 표시
        if (warpedBitmap == null) {
            warpToggle.setVisibility(View.GONE);
            imageView.setImageBitmap(originalBitmap);
        } else {
            // 기본값: 체크(보정본 표시)
            warpToggle.setChecked(true);
            imageView.setImageBitmap(warpedBitmap);

            warpToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    imageView.setImageBitmap(warpedBitmap);
                } else {
                    imageView.setImageBitmap(originalBitmap);
                }
            });
        }

        retryButton.setOnClickListener(v -> {
            dismiss();
            if (getTargetFragment() instanceof CameraListener) {
                ((CameraListener) getTargetFragment()).onRetryCapture();
            }
        });

        confirmButton.setOnClickListener(v -> {
            if (getTargetFragment() instanceof CameraListener) {
                // 토글 상태에 따라 선택된 이미지를 콜백으로 전달
                Bitmap selected =
                        (warpToggle.getVisibility() == View.VISIBLE && warpToggle.isChecked() && warpedBitmap != null)
                                ? warpedBitmap
                                : originalBitmap;
                ((CameraListener) getTargetFragment()).onConfirmCapture(selected);
            }
            dismiss();
        });

        return view;
    }
}
