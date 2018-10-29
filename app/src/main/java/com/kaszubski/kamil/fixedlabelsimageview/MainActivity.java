package com.kaszubski.kamil.fixedlabelsimageview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
    private EditText editText;
    private Button button;
    private TouchImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        button = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mgram_pict);
        imageView.setBitmap(bitmap);
        imageView.setMaxScale(5);
        imageView.setAlwaysShowLeftLabel(true);
        imageView.setLeftLabelWidth(206);
        imageView.setAlwaysShowTopLabel(true);
        imageView.setTopLabelHeight(172);

        button.setOnClickListener(view -> {
            imageView.setBitmap(bitmap);
            double scale = Double.parseDouble(editText.getText().toString().trim());
            imageView.setScale(scale);

//            int value = Integer.parseInt(editText.getText().toString().trim());
//            imageView.setTopLabelHeight(value);
        });
    }
}
