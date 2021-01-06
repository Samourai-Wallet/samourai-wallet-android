package com.samourai.wallet.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.imageview.ShapeableImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.client.android.Contents
import com.google.zxing.client.android.encode.QRCodeEncoder
import com.samourai.wallet.R
import java.io.File
import java.io.FileOutputStream

class QRBottomSheetDialog(val qrData: String, val title: String? = "", val clipboardLabel: String? = "") : BottomSheetDialogFragment() {


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_qr_code_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val qrDialogCopyToClipBoard = view.findViewById<TextView>(R.id.qrDialogCopyToClipBoard);
        val shareQrButton = view.findViewById<TextView>(R.id.shareQrButton);
        val qrDialogTitle = view.findViewById<TextView>(R.id.qrDialogTitle);
        val qrTextView = view.findViewById<TextView>(R.id.qrTextView);
        val qRImage = view.findViewById<ShapeableImageView>(R.id.imgQrCode);

        title?.let {
            qrDialogTitle.text = title
        }
        var bitmap: Bitmap? = null
        val qrCodeEncoder = QRCodeEncoder(qrData, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500)
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap()
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        qrTextView.setOnClickListener {
            TransitionManager.beginDelayedTransition(qrTextView.rootView as ViewGroup)
            if (qrTextView.maxLines == 2) {
                qrTextView.maxLines = 10
            } else {
                qrTextView.maxLines = 2
            }
        }
        val radius = resources.getDimension(R.dimen.qr_image_corner_radius)
        qrTextView.text = qrData
        qRImage.shapeAppearanceModel = qRImage.shapeAppearanceModel
                .toBuilder()
                .setAllCornerSizes(radius)
                .build()
        view.findViewById<ImageView>(R.id.imgQrCode).setImageBitmap(bitmap)

        qrDialogCopyToClipBoard.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(clipboardLabel ?: title, qrData)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            this.dismiss()
        }
        shareQrButton.setOnClickListener {
            val strFileName = AppUtil.getInstance(requireContext()).receiveQRFilename
            val file = File(strFileName)
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }

            try {
                val fos = FileOutputStream(file);
                file.setReadable(true, false)
                val bitmap = (qRImage.drawable as BitmapDrawable).bitmap
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos)
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.type = "image/png"
                if (Build.VERSION.SDK_INT >= 24) {
                    //From API 24 sending FIle on intent ,require custom file provider
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            requireContext(),
                            requireContext()
                                    .packageName + ".provider", file))
                } else {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                }
                startActivity(Intent.createChooser(intent, clipboardLabel));

            } catch (ex: Exception) {
                Toast.makeText(requireContext(), ex.message, Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }

    }


}