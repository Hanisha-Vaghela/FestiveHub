package com.example.festivehub;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class Volunteers_QR extends AppCompatActivity {

    ImageView volunteersQRimg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteers_qr);

        volunteersQRimg = findViewById(R.id.volunteersQR);

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Different text per event = different QR per event
        String qrText = "festivehub://volunteer/event/" + eventId;

        Bitmap bitmap = generateQrcode(qrText);
        if (bitmap != null) {
            volunteersQRimg.setImageBitmap(bitmap);
        }
    }

    public Bitmap generateQrcode(String text) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
