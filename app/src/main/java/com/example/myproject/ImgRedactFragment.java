package com.example.myproject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImgRedactFragment extends DialogFragment {

    public interface OnImageChangedListener {
        void onImageUpdated(String imageBase64);
        void onImageDeleted();
    }

    private OnImageChangedListener listener;
    private String currentImageBase64 = "";
    private ImageView ivPreview;
    private String tempImageBase64 = null;
    private Bitmap currentBitmap = null;

    public void setOnImageChangedListener(OnImageChangedListener l) {
        this.listener = l;
    }

    public void setCurrentImage(String imageBase64) {
        this.currentImageBase64 = imageBase64;
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
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        try {
                            currentBitmap = uriToBitmap(selectedUri);
                            tempImageBase64 = bitmapToBase64(currentBitmap);
                            ivPreview.setImageBitmap(currentBitmap);
                            ivPreview.setVisibility(View.VISIBLE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_img_redact, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivPreview = view.findViewById(R.id.ivPreview);
        Button btnPickImage = view.findViewById(R.id.btnPickImage);
        Button btnRotate = view.findViewById(R.id.btnRotate);
        Button btnDelete = view.findViewById(R.id.btnDelete);
        Button btnUpdate = view.findViewById(R.id.btnUpdate);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        // Загружаем текущее изображение с фиксированными размерами
        if (currentImageBase64 != null && !currentImageBase64.isEmpty()) {
            try {
                currentBitmap = base64ToBitmap(currentImageBase64);
                ivPreview.setImageBitmap(currentBitmap);
                ivPreview.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImage.launch(intent);
        });

        btnRotate.setOnClickListener(v -> {
            if (currentBitmap != null) {
                rotateBitmap();
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageDeleted();
            }
            dismiss();
        });

        btnUpdate.setOnClickListener(v -> {
            if (tempImageBase64 != null && !tempImageBase64.isEmpty()) {
                if (listener != null) {
                    listener.onImageUpdated(tempImageBase64);
                }
                dismiss();
            } else if (currentImageBase64 != null && !currentImageBase64.isEmpty()) {
                dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void rotateBitmap() {
        Matrix matrix = new Matrix();
        matrix.postRotate(90f);

        currentBitmap = Bitmap.createBitmap(currentBitmap, 0, 0,
                currentBitmap.getWidth(), currentBitmap.getHeight(),
                matrix, true);

        ivPreview.setImageBitmap(currentBitmap);
        tempImageBase64 = bitmapToBase64(currentBitmap);
    }

    private Bitmap uriToBitmap(Uri uri) throws IOException {
        ContentResolver contentResolver = requireContext().getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(uri);

        if (inputStream == null) {
            throw new IOException("Unable to open input stream for URI: " + uri);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        inputStream = contentResolver.openInputStream(uri);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int sampleSize = calculateSampleSize(options.outWidth, options.outHeight, screenWidth);

        options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        return bitmap;
    }

    private int calculateSampleSize(int width, int height, int targetWidth) {
        int sampleSize = 1;
        while (width / sampleSize > targetWidth) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}