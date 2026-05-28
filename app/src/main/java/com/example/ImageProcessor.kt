package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.OutputStream

object ImageProcessor {
    private const val TAG = "ImageProcessor"

    /**
     * Divides the image height into 10 equal parts.
     * Overlays thin divider lines and draws streak labels in the top section.
     */
    fun processStreakImage(sourceBitmap: Bitmap, streakCount: Int): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        // Avoid modifying original bitmap, create a high-quality mutable copy
        val processedBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)

        // 1. Divide image vertically into 10 equal parts using thin Sunrise Yellow lines
        val linePaint = Paint().apply {
            color = Color.parseColor("#FFFFD600") // Sunrise Yellow
            strokeWidth = (height * 0.003f).coerceAtLeast(3f) // Dynamic scale thickness
            style = Paint.Style.STROKE
            alpha = 140 // Semi-transparent overlay
        }

        val partHeight = height / 10.0f
        for (i in 1..9) {
            val y = i * partHeight
            canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
        }

        // 2. Clear out or semi-darken the top section (1st part) to make text stand out beautifully
        val topRect = RectF(0f, 0f, width.toFloat(), partHeight)
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 155 // 60% overlay darkened background
            style = Paint.Style.FILL
        }
        canvas.drawRect(topRect, bgPaint)

        // Draw a glowing border under the top section
        val borderPaint = Paint().apply {
            color = Color.parseColor("#FFFF9100") // Golden Orange
            strokeWidth = (height * 0.005f).coerceAtLeast(5f)
            style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, partHeight, width.toFloat(), partHeight, borderPaint)

        // 3. Draw modern typography labels "Day 01", "Day 02", etc. in the top section
        val textPaint = Paint().apply {
            color = Color.parseColor("#FFFFD600") // Sunrise Yellow
            isAntiAlias = true
            style = Paint.Style.FILL
            textSize = (partHeight * 0.22f).coerceAtLeast(24f) // Responsive text size
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, Color.BLACK) // Cool text glow/shadow
        }

        // Generate challenge list based on streak count
        val streakLabels = mutableListOf<String>()
        val maxStreakToShow = streakCount.coerceAtMost(75)
        for (day in 1..maxStreakToShow) {
            streakLabels.add(String.format("Day %02d", day))
        }

        // Beautiful grid or side-by-side spacing logic for streak labels in top section
        if (streakLabels.isNotEmpty()) {
            val totalTextWidth = width * 0.9f
            val labelCount = streakLabels.size

            // If we have many days, calculate reasonable layout
            // For a few days, draw side-by-side cleanly
            if (labelCount <= 5) {
                val step = totalTextWidth / (labelCount + 1)
                val startX = width * 0.05f
                val yOffset = partHeight * 0.55f // Centered in the top division

                for (idx in 0 until labelCount) {
                    val x = startX + (idx + 1) * step
                    canvas.drawText(streakLabels[idx], x, yOffset, textPaint)
                }
            } else {
                // For a larger number of days (e.g. 6 to 75), draw them in rows
                // to make sure it looks incredibly professional and structured!
                val columns = 5
                val rows = (labelCount + columns - 1) / columns
                val colStep = totalTextWidth / (columns + 1)
                val rowStep = partHeight * 0.75f / (rows + 1)
                val startX = width * 0.05f

                textPaint.textSize = (partHeight * 0.14f).coerceAtLeast(16f) // shrink font slightly

                for (idx in 0 until labelCount) {
                    val row = idx / columns
                    val col = idx % columns
                    val x = startX + (col + 1) * colStep
                    val y = rowStep * (row + 1) + partHeight * 0.12f
                    canvas.drawText(streakLabels[idx], x, y, textPaint)
                }
            }
        } else {
            // No streak days succeeded yet
            canvas.drawText("Challenge Started!", width / 2.0f, partHeight / 2.0f + 10f, textPaint)
        }

        return processedBitmap
    }

    /**
     * Saves processed bitmap to the external Pictures directory securely using MediaStore.
     * Fully compatible with modern scoped storage. No runtime storage permission required.
     */
    fun saveImageToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "Challenge_Streak_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        try {
            val contentResolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Challenge")
                }
                
                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = contentResolver.openOutputStream(imageUri)
                }
            } else {
                // Android 9 and lower backup
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val imageFile = java.io.File(imagesDir, filename)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                }
                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = java.io.FileOutputStream(imageFile)
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                Log.d(TAG, "Image saved successfully: $imageUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to gallery", e)
            imageUri = null
        } finally {
            fos?.close()
        }

        return imageUri
    }
}
