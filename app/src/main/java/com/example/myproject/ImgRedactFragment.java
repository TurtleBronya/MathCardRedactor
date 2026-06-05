package com.example.myproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImgRedactFragment extends DialogFragment {

    public interface OnImageChangedListener {
        void onImageUpdated(String imageBase64);
        void onImageDeleted();
    }

    private OnImageChangedListener listener;
    private String currentImageBase64 = null;
    private ImageView ivPreview;
    private String tempImageBase64 = null;
    private Bitmap currentBitmap = null;
    private float currentRotation = 0f;
    private static final int PICK_IMAGE_REQUEST = 1;

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
            // Устанавливаем ширину на всю доступную ширину
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,  // Ширина на весь экран
                    ViewGroup.LayoutParams.WRAP_CONTENT   // Высота по содержимому
            );

            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

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


        // Загружаем текущее изображение
        if (currentImageBase64 != null && !currentImageBase64.isEmpty()) {
            try {
                currentBitmap = base64ToBitmap(currentImageBase64);
                currentRotation = 0f;

                // Масштабируем битмап только по высоте, если нужно
                currentBitmap = scaleBitmapIfNeeded(currentBitmap);

                ivPreview.setImageBitmap(currentBitmap);
                ivPreview.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnPickImage.setOnClickListener(v -> openGallery());

        btnRotate.setOnClickListener(v -> {
            if (currentBitmap != null) {
                rotateBitmap();
                Toast.makeText(getContext(), "Поворот на +90°", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Нет изображения", Toast.LENGTH_SHORT).show();
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
            } else {
                Toast.makeText(getContext(), "Нет изображения для сохранения", Toast.LENGTH_SHORT).show();
            }
        });



        // Ограничиваем только высоту ImageView
        ivPreview.setMaxHeight(getMaxImageHeight());
    }

    private int getMaxImageHeight() {
        android.graphics.Point size = new android.graphics.Point();
        requireActivity().getWindowManager().getDefaultDisplay().getSize(size);
        return (int) (size.y * 0.5); // Максимум 50% от высоты экрана
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                try {
                    currentBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), selectedUri);
                    currentRotation = 0f;

                    // Масштабируем битмап только по высоте, если нужно
                    currentBitmap = scaleBitmapIfNeeded(currentBitmap);

                    tempImageBase64 = bitmapToBase64(currentBitmap);
                    ivPreview.setImageBitmap(currentBitmap);
                    ivPreview.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Изображение загружено", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Bitmap scaleBitmapIfNeeded(Bitmap bitmap) {
        int maxHeight = getMaxImageHeight();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (height <= maxHeight) {
            return bitmap;
        }

        // Масштабируем только по высоте, ширина будет пропорциональной
        float ratio = (float) maxHeight / height;
        int newWidth = Math.round(width * ratio);
        int newHeight = maxHeight;

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void rotateBitmap() {
        Matrix matrix = new Matrix();
        matrix.postRotate(90f);

        currentBitmap = Bitmap.createBitmap(currentBitmap, 0, 0,
                currentBitmap.getWidth(), currentBitmap.getHeight(),
                matrix, true);

        currentRotation += 90f;

        // После поворота снова проверяем высоту
        currentBitmap = scaleBitmapIfNeeded(currentBitmap);
        ivPreview.setImageBitmap(currentBitmap);
        tempImageBase64 = bitmapToBase64(currentBitmap);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int quality = 70;
        if (bitmap.getWidth() > 2000 || bitmap.getHeight() > 2000) {
            quality = 50;
        }

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}