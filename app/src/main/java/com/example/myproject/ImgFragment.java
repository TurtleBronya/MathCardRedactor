package com.example.myproject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;

public class ImgFragment extends DialogFragment {
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    public interface OnImageAddedListener {
        void onImageAdded(Uri imageUri);
    }

    private OnImageAddedListener listener;
    private Uri selectedUri = null;
    private ImageView ivPreview;

    public void setOnImageAddedListener(OnImageAddedListener l) {
        this.listener = l;
    }

    private final ActivityResultLauncher<Intent> pickImage =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedUri = result.getData().getData();
                    ivPreview.setImageURI(selectedUri);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_img, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivPreview        = view.findViewById(R.id.ivPreview);
        Button btnPick   = view.findViewById(R.id.btnPickImage);
        Button btnAdd    = view.findViewById(R.id.btnAdd);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnPick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImage.launch(intent);
        });

        btnAdd.setOnClickListener(v -> {
            if (selectedUri != null && listener != null) {
                listener.onImageAdded(selectedUri);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }
}