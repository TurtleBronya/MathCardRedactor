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

public class ImgFragment extends DialogFragment {

    public interface OnImageAddedListener {
        void onImageAdded(String imageBase64);
    }

    private OnImageAddedListener listener;
    private ImageView ivPreview;
    private Bitmap currentBitmap = null;
    private float currentRotation = 0f;
    private static final int PICK_IMAGE_REQUEST = 1;

    public void setOnImageAddedListener(OnImageAddedListener l) {
        this.listener = l;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Получаем размер экрана
            WindowManager windowManager = (WindowManager) requireContext().getSystemService(requireContext().WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            android.graphics.Point size = new android.graphics.Point();
            display.getSize(size);

            int screenHeight = size.y;

            // Устанавливаем ширину на всю доступную ширину, высоту ограничиваем
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,  // Ширина на весь экран
                    ViewGroup.LayoutParams.WRAP_CONTENT   // Высота по содержимому
            );

            // Ограничиваем только максимальную высоту (80% от экрана)
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
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
        Button btnPickImage = view.findViewById(R.id.btnPickImage);
        Button btnRotate = view.findViewById(R.id.btnRotate);
        Button btnAdd = view.findViewById(R.id.btnAdd);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnPickImage.setOnClickListener(v -> openGallery());

        btnRotate.setOnClickListener(v -> {
            if (currentBitmap != null) {
                rotateBitmap(90);
                Toast.makeText(getContext(), "Поворот на +90°", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Сначала выберите изображение", Toast.LENGTH_SHORT).show();
            }
        });

        btnAdd.setOnClickListener(v -> {
            if (currentBitmap != null) {
                String imageBase64 = bitmapToBase64(currentBitmap);
                if (listener != null) {
                    listener.onImageAdded(imageBase64);
                }
                dismiss();
            } else {
                Toast.makeText(getContext(), "Выберите изображение", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        // Ограничиваем только высоту ImageView, ширину не трогаем
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

    private void rotateBitmap(float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        currentBitmap = Bitmap.createBitmap(currentBitmap, 0, 0,
                currentBitmap.getWidth(), currentBitmap.getHeight(),
                matrix, true);

        currentRotation += degrees;

        // После поворота снова проверяем высоту
        currentBitmap = scaleBitmapIfNeeded(currentBitmap);
        ivPreview.setImageBitmap(currentBitmap);
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
}