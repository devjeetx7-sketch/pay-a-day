package com.dailywork.attedance.ui

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import com.dailywork.attedance.R
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CustomScannerActivity : Activity() {
    private lateinit var capture: CaptureManager
    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private lateinit var switchFlashlightButton: Button
    private var isFlashlightOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_scanner_layout)

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner)
        switchFlashlightButton = findViewById(R.id.btn_flashlight)

        capture = CaptureManager(this, barcodeScannerView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.setShowMissingCameraPermissionDialog(false)
        capture.decode()

        switchFlashlightButton.setOnClickListener {
            if (isFlashlightOn) {
                barcodeScannerView.setTorchOff()
            } else {
                barcodeScannerView.setTorchOn()
            }
        }

        barcodeScannerView.setTorchListener(object : DecoratedBarcodeView.TorchListener {
            override fun onTorchOn() {
                isFlashlightOn = true
            }

            override fun onTorchOff() {
                isFlashlightOn = false
            }
        })
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onResume()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}
