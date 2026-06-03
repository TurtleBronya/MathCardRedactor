package com.example.myproject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImgFragment extends DialogFragment {

    public interface OnImageAddedListener {
        void onImageAdded(String imageBase64);
    }

    private OnImageAddedListener listener;
    private Uri selectedUri = null;
    private ImageView ivPreview;
    private String tempImageBase64 = null;

    public void setOnImageAddedListener(OnImageAddedListener l) {
        this.listener = l;
    }

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

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        try {
                            tempImageBase64 = convertUriToBase64(selectedUri);
                            // Настраиваем ImageView под размер
                            Bitmap bitmap = base64ToBitmap(tempImageBase64);
                            setupImageViewForBitmap(bitmap);
                            ivPreview.setImageBitmap(bitmap);
                            ivPreview.setVisibility(View.VISIBLE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    // Добавьте этот метод в ImgFragment
    private void setupImageViewForBitmap(Bitmap bitmap) {
        if (bitmap == null) return;

        int imageWidth = bitmap.getWidth();
        int imageHeight = bitmap.getHeight();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int dialogPadding = 40;
        int maxWidth = screenWidth - dialogPadding;
        int scaledHeight = (int) ((float) imageHeight * maxWidth / imageWidth);

        ViewGroup.LayoutParams params = ivPreview.getLayoutParams();
        params.width = maxWidth;
        params.height = scaledHeight;
        ivPreview.setLayoutParams(params);
        ivPreview.setScaleType(ImageView.ScaleType.FIT_XY);
        ivPreview.setAdjustViewBounds(true);
    }

    private Bitmap base64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_img, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivPreview = view.findViewById(R.id.ivPreview);
        Button btnPick = view.findViewById(R.id.btnPickImage);
        Button btnAdd = view.findViewById(R.id.btnAdd);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnPick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImage.launch(intent);
        });

        btnAdd.setOnClickListener(v -> {
            if (tempImageBase64 != null && !tempImageBase64.isEmpty() && listener != null) {
                listener.onImageAdded(tempImageBase64);
                dismiss();
            } else if (selectedUri == null) {
                Toast.makeText(getContext(), "Выберите изображение", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private String convertUriToBase64(Uri uri) throws IOException {
        ContentResolver contentResolver = requireContext().getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(uri);

        if (inputStream == null) {
            throw new IOException("Unable to open input stream for URI: " + uri);
        }

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}