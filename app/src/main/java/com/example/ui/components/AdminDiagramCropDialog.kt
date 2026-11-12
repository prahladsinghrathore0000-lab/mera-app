package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.OfflineTestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Editor mode for diagram manipulation:
 * - CROP: Trim whitespace, crop unneeded questions/text, rotate
 * - DRAW: Pen & Whiteout markup tool
 * - PREVIEW: Live look at final rendered diagram
 */
enum class DiagramEditorMode {
    CROP,
    DRAW,
    PREVIEW
}

/**
 * Represents a single drawn stroke on top of the diagram.
 * Points are stored in normalized coordinates (0.0 to 1.0) relative to the diagram bitmap dimensions.
 */
data class DiagramStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidthDp: Float
)

/**
 * High-craft, responsive interactive dialog for editing and cropping exam diagrams.
 * Maximizes diagram canvas viewport space while keeping controls sleek and compact.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDiagramCropDialog(
    imageSource: String,
    questionNumber: Int = 1,
    onDismiss: () -> Unit,
    onDeleteDiagram: (() -> Unit)? = null,
    onCropped: (newImagePath: String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Normalized crop bounds (0.0 to 1.0)
    var cropLeft by remember { mutableFloatStateOf(0.0f) }
    var cropTop by remember { mutableFloatStateOf(0.0f) }
    var cropRight by remember { mutableFloatStateOf(1.0f) }
    var cropBottom by remember { mutableFloatStateOf(1.0f) }

    var currentMode by remember { mutableStateOf(DiagramEditorMode.CROP) }
    var showFineMargins by remember { mutableStateOf(false) }

    // Pen & Drawing State
    val strokeList = remember { mutableStateListOf<DiagramStroke>() }
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var selectedStrokeWidthDp by remember { mutableFloatStateOf(16f) }

    // Color Palette
    val colorPalette = listOf(
        Color.White to "सफ़ेद (इरेज़र)",
        Color.Black to "काला",
        Color(0xFFDC2626) to "लाल",
        Color(0xFF2563EB) to "नीला",
        Color(0xFF16A34A) to "हरा",
        Color(0xFFEAB308) to "पीला",
        Color(0xFFF97316) to "नारंगी",
        Color(0xFF9333EA) to "बैंगनी"
    )

    // Load bitmap asynchronously
    LaunchedEffect(imageSource) {
        isLoading = true
        errorMessage = null
        try {
            val bmp = withContext(Dispatchers.IO) {
                decodeBitmapFromSource(context, imageSource)
            }
            if (bmp != null) {
                originalBitmap = bmp
                currentBitmap = bmp
                cropLeft = 0f
                cropTop = 0f
                cropRight = 1f
                cropBottom = 1f
            } else {
                errorMessage = "इमेज लोड नहीं हो सकी। कृपया पुनः प्रयास करें।"
            }
        } catch (e: Exception) {
            errorMessage = "इमेज लोड करने में त्रुटि: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Auto-trim whitespace helper
    fun autoTrimWhitespace() {
        val bmp = currentBitmap ?: return
        try {
            val w = bmp.width
            val h = bmp.height
            var minX = w
            var minY = h
            var maxX = 0
            var maxY = 0
            var hasNonWhite = false

            val step = max(1, min(w, h) / 300)
            for (y in 0 until h step step) {
                for (x in 0 until w step step) {
                    val p = bmp.getPixel(x, y)
                    val r = (p shr 16) and 0xff
                    val g = (p shr 8) and 0xff
                    val b = p and 0xff
                    if (r < 240 || g < 240 || b < 240) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                        hasNonWhite = true
                    }
                }
            }

            if (hasNonWhite && maxX > minX && maxY > minY) {
                val padX = (w * 0.02f).toInt()
                val padY = (h * 0.02f).toInt()
                val safeMinX = max(0, minX - padX)
                val safeMaxX = min(w, maxX + padX)
                val safeMinY = max(0, minY - padY)
                val safeMaxY = min(h, maxY + padY)

                cropLeft = safeMinX.toFloat() / w
                cropRight = safeMaxX.toFloat() / w
                cropTop = safeMinY.toFloat() / h
                cropBottom = safeMaxY.toFloat() / h
                Toast.makeText(context, "⚡ खाली जगह हटा दी गई!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "पूरी आकृति पहले से ही केंद्रित है", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ऑटो-ट्रिम में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Rotate 90 degrees
    fun rotate90() {
        val bmp = currentBitmap ?: return
        try {
            val matrix = Matrix().apply { postRotate(90f) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            currentBitmap = rotated
            cropLeft = 0f
            cropTop = 0f
            cropRight = 1f
            cropBottom = 1f
            strokeList.clear()
            Toast.makeText(context, "↷ आकृति 90° घुमाई गई", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "घुमाने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Burn all strokes and crop bitmap, then save locally
    fun saveCropped() {
        val bmp = currentBitmap ?: return
        try {
            val w = bmp.width
            val h = bmp.height

            // 1. Create a mutable copy to render pen strokes
            val mutableBmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
            val androidCanvas = android.graphics.Canvas(mutableBmp)

            // Render all drawn strokes onto bitmap
            for (stroke in strokeList) {
                if (stroke.points.isEmpty()) continue

                val strokeWidthPx = (stroke.strokeWidthDp * (w / 360f)).coerceAtLeast(2f)
                val paint = Paint().apply {
                    color = stroke.color.toArgb()
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                if (stroke.points.size == 1) {
                    paint.style = Paint.Style.FILL
                    val pt = stroke.points[0]
                    androidCanvas.drawCircle(pt.x * w, pt.y * h, strokeWidthPx / 2f, paint)
                } else {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = strokeWidthPx
                    val path = android.graphics.Path()
                    val first = stroke.points[0]
                    path.moveTo(first.x * w, first.y * h)
                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        path.lineTo(pt.x * w, pt.y * h)
                    }
                    androidCanvas.drawPath(path, paint)
                }
            }

            // 2. Perform cropping on the painted bitmap
            val safeX = (cropLeft * w).toInt().coerceIn(0, w - 1)
            val safeY = (cropTop * h).toInt().coerceIn(0, h - 1)
            val safeW = ((cropRight - cropLeft) * w).toInt().coerceIn(1, w - safeX)
            val safeH = ((cropBottom - cropTop) * h).toInt().coerceIn(1, h - safeY)

            val croppedBmp = Bitmap.createBitmap(mutableBmp, safeX, safeY, safeW, safeH)

            val offlineDir = File(context.filesDir, "offline_images")
            if (!offlineDir.exists()) offlineDir.mkdirs()

            val fileName = "edited_diagram_${System.currentTimeMillis() % 1000000}_q$questionNumber.jpg"
            val targetFile = File(offlineDir, fileName)

            FileOutputStream(targetFile).use { out ->
                croppedBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            Toast.makeText(context, "✅ डायग्राम सफलतापूर्वक सेव हो गया!", Toast.LENGTH_SHORT).show()
            onCropped(targetFile.absolutePath)
            onDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "डायग्राम सेव करने में त्रुटि: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.96f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A), // Dark slate theme for clear diagram contrast
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // ================= 1. COMPACT TOP HEADER =================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "बंद करें",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "प्रश्न $questionNumber डायग्राम",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Mode Switcher Tabs in Top Bar (Saves Vertical Space)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactModeTab(
                                icon = Icons.Default.Crop,
                                label = "क्रॉप",
                                isSelected = currentMode == DiagramEditorMode.CROP,
                                activeColor = Color(0xFF3B82F6),
                                onClick = { currentMode = DiagramEditorMode.CROP }
                            )

                            CompactModeTab(
                                icon = Icons.Default.Brush,
                                label = "पेन",
                                isSelected = currentMode == DiagramEditorMode.DRAW,
                                activeColor = Color(0xFF8B5CF6),
                                onClick = { currentMode = DiagramEditorMode.DRAW }
                            )

                            CompactModeTab(
                                icon = Icons.Default.Visibility,
                                label = "प्रीव्यू",
                                isSelected = currentMode == DiagramEditorMode.PREVIEW,
                                activeColor = Color(0xFF10B981),
                                onClick = { currentMode = DiagramEditorMode.PREVIEW }
                            )
                        }
                    }

                    // Quick Save Button in Header
                    Button(
                        onClick = { saveCropped() },
                        enabled = !isLoading && currentBitmap != null,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("सेव", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // ================= 2. MAIN DIAGRAM CANVAS (MAX SCREEN ESTATE) =================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF030712)), // High contrast deep black
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(32.dp))
                            Text("डायग्राम लोड हो रहा है...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    } else if (errorMessage != null || currentBitmap == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "इमेज उपलब्ध नहीं है",
                                color = Color(0xFFF87171),
                                fontSize = 12.5.sp
                            )
                        }
                    } else {
                        val bmp = currentBitmap!!

                        if (currentMode == DiagramEditorMode.PREVIEW) {
                            // Live Cropped & Drawn Preview Mode
                            val cropW = ((cropRight - cropLeft) * bmp.width).toInt().coerceIn(1, bmp.width)
                            val cropH = ((cropBottom - cropTop) * bmp.height).toInt().coerceIn(1, bmp.height)
                            val cropX = (cropLeft * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                            val cropY = (cropTop * bmp.height).toInt().coerceIn(0, bmp.height - 1)

                            val previewCropped = remember(cropLeft, cropTop, cropRight, cropBottom, bmp, strokeList.size) {
                                try {
                                    val mutableBmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
                                    val androidCanvas = android.graphics.Canvas(mutableBmp)
                                    val w = bmp.width
                                    val h = bmp.height

                                    for (stroke in strokeList) {
                                        if (stroke.points.isEmpty()) continue
                                        val strokeWidthPx = (stroke.strokeWidthDp * (w / 360f)).coerceAtLeast(2f)
                                        val paint = Paint().apply {
                                            color = stroke.color.toArgb()
                                            isAntiAlias = true
                                            strokeCap = Paint.Cap.ROUND
                                            strokeJoin = Paint.Join.ROUND
                                        }
                                        if (stroke.points.size == 1) {
                                            paint.style = Paint.Style.FILL
                                            val pt = stroke.points[0]
                                            androidCanvas.drawCircle(pt.x * w, pt.y * h, strokeWidthPx / 2f, paint)
                                        } else {
                                            paint.style = Paint.Style.STROKE
                                            paint.strokeWidth = strokeWidthPx
                                            val path = android.graphics.Path()
                                            val first = stroke.points[0]
                                            path.moveTo(first.x * w, first.y * h)
                                            for (i in 1 until stroke.points.size) {
                                                val pt = stroke.points[i]
                                                path.lineTo(pt.x * w, pt.y * h)
                                            }
                                            androidCanvas.drawPath(path, paint)
                                        }
                                    }

                                    Bitmap.createBitmap(
                                        mutableBmp,
                                        cropX, cropY,
                                        cropW.coerceAtMost(bmp.width - cropX),
                                        cropH.coerceAtMost(bmp.height - cropY)
                                    )
                                } catch (_: Throwable) { null }
                            }

                            if (previewCropped != null) {
                                Image(
                                    bitmap = previewCropped.asImageBitmap(),
                                    contentDescription = "Cropped Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            // Interactive Working Canvas (Draw or Crop)
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val containerW = constraints.maxWidth.toFloat()
                                val containerH = constraints.maxHeight.toFloat()
                                val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat()
                                val containerAspect = containerW / containerH

                                val (drawW, drawH) = if (bmpAspect > containerAspect) {
                                    containerW to (containerW / bmpAspect)
                                } else {
                                    (containerH * bmpAspect) to containerH
                                }

                                val offsetX = (containerW - drawW) / 2f
                                val offsetY = (containerH - drawH) / 2f

                                // 1. Base Diagram Bitmap
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Diagram",
                                    modifier = Modifier
                                        .size(
                                            width = (drawW / density.density).dp,
                                            height = (drawH / density.density).dp
                                        ),
                                    contentScale = ContentScale.FillBounds
                                )

                                // 2. Gesture Handling & Drawing Canvas
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(currentMode, selectedColor, selectedStrokeWidthDp, bmp) {
                                            if (currentMode == DiagramEditorMode.DRAW) {
                                                // Pen Drawing Gestures
                                                detectDragGestures(
                                                    onDragStart = { startOffset ->
                                                        val normX = ((startOffset.x - offsetX) / drawW).coerceIn(0f, 1f)
                                                        val normY = ((startOffset.y - offsetY) / drawH).coerceIn(0f, 1f)
                                                        currentStrokePoints = listOf(Offset(normX, normY))
                                                    },
                                                    onDrag = { change, _ ->
                                                        change.consume()
                                                        val currX = ((change.position.x - offsetX) / drawW).coerceIn(0f, 1f)
                                                        val currY = ((change.position.y - offsetY) / drawH).coerceIn(0f, 1f)
                                                        currentStrokePoints = currentStrokePoints + Offset(currX, currY)
                                                    },
                                                    onDragEnd = {
                                                        if (currentStrokePoints.isNotEmpty()) {
                                                            strokeList.add(
                                                                DiagramStroke(
                                                                    points = currentStrokePoints,
                                                                    color = selectedColor,
                                                                    strokeWidthDp = selectedStrokeWidthDp
                                                                )
                                                            )
                                                            currentStrokePoints = emptyList()
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        currentStrokePoints = emptyList()
                                                    }
                                                )
                                            } else {
                                                // Crop Pan Gestures
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val normDx = dragAmount.x / drawW
                                                    val normDy = dragAmount.y / drawH

                                                    val boxW = cropRight - cropLeft
                                                    val boxH = cropBottom - cropTop

                                                    val newL = (cropLeft + normDx).coerceIn(0f, 1f - boxW)
                                                    val newT = (cropTop + normDy).coerceIn(0f, 1f - boxH)

                                                    cropLeft = newL
                                                    cropRight = newL + boxW
                                                    cropTop = newT
                                                    cropBottom = newT + boxH
                                                }
                                            }
                                        }
                                ) {
                                    // Draw all confirmed pen strokes
                                    for (stroke in strokeList) {
                                        drawNormalizedStroke(
                                            stroke = stroke,
                                            offsetX = offsetX,
                                            offsetY = offsetY,
                                            drawW = drawW,
                                            drawH = drawH,
                                            density = density.density
                                        )
                                    }

                                    // Draw active stroke in progress
                                    if (currentStrokePoints.isNotEmpty()) {
                                        drawNormalizedStroke(
                                            stroke = DiagramStroke(currentStrokePoints, selectedColor, selectedStrokeWidthDp),
                                            offsetX = offsetX,
                                            offsetY = offsetY,
                                            drawW = drawW,
                                            drawH = drawH,
                                            density = density.density
                                        )
                                    }

                                    // If in CROP mode, draw crop overlay box and boundary lines
                                    if (currentMode == DiagramEditorMode.CROP) {
                                        val rectL = offsetX + cropLeft * drawW
                                        val rectT = offsetY + cropTop * drawH
                                        val rectR = offsetX + cropRight * drawW
                                        val rectB = offsetY + cropBottom * drawH

                                        val darkDimColor = Color(0x99000000)

                                        // Darken outside crop margins
                                        drawRect(darkDimColor, topLeft = Offset(0f, 0f), size = Size(size.width, rectT))
                                        drawRect(darkDimColor, topLeft = Offset(0f, rectB), size = Size(size.width, size.height - rectB))
                                        drawRect(darkDimColor, topLeft = Offset(0f, rectT), size = Size(rectL, rectB - rectT))
                                        drawRect(darkDimColor, topLeft = Offset(rectR, rectT), size = Size(size.width - rectR, rectB - rectT))

                                        // Crop boundary rectangle (Glowing Cyan)
                                        drawRect(
                                            color = Color(0xFF38BDF8),
                                            topLeft = Offset(rectL, rectT),
                                            size = Size(rectR - rectL, rectB - rectT),
                                            style = Stroke(width = 2.5f)
                                        )

                                        // Rule-of-thirds grid lines
                                        val thirdW = (rectR - rectL) / 3f
                                        val thirdH = (rectB - rectT) / 3f
                                        val gridColor = Color(0x66FFFFFF)
                                        drawLine(gridColor, Offset(rectL + thirdW, rectT), Offset(rectL + thirdW, rectB), strokeWidth = 1f)
                                        drawLine(gridColor, Offset(rectL + 2 * thirdW, rectT), Offset(rectL + 2 * thirdW, rectB), strokeWidth = 1f)
                                        drawLine(gridColor, Offset(rectL, rectT + thirdH), Offset(rectR, rectT + thirdH), strokeWidth = 1f)
                                        drawLine(gridColor, Offset(rectL, rectT + 2 * thirdH), Offset(rectR, rectT + 2 * thirdH), strokeWidth = 1f)
                                    }
                                }
                            }
                        }
                    }

                    // Floating Status Badge on Top-Left
                    if (!isLoading && currentBitmap != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.70f)
                        ) {
                            Text(
                                text = when (currentMode) {
                                    DiagramEditorMode.PREVIEW -> "👀 फाइनल प्रीव्यू"
                                    DiagramEditorMode.DRAW -> "✏️ पेन (${strokeList.size} स्ट्रोक)"
                                    DiagramEditorMode.CROP -> "✂️ क्रॉप मोड"
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ================= 3. COMPACT BOTTOM CONTROL PANEL =================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (currentMode == DiagramEditorMode.CROP) {
                            // CROP CONTROLS (Compact single-line horizontal scrollable actions)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactActionPill(
                                    icon = Icons.Default.AutoAwesome,
                                    label = "⚡ ऑटो-ट्रिम",
                                    containerColor = Color(0xFF1E3A8A),
                                    textColor = Color(0xFF93C5FD),
                                    onClick = { autoTrimWhitespace() }
                                )

                                CompactActionPill(
                                    icon = Icons.Default.Crop,
                                    label = "✂️ नीचे हटाएं (-25%)",
                                    onClick = {
                                        cropBottom = max(0.40f, cropBottom - 0.25f)
                                        Toast.makeText(context, "नीचे का हिस्सा काटा गया", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                CompactActionPill(
                                    icon = Icons.Default.Crop,
                                    label = "✂️ ऊपर हटाएं (-15%)",
                                    onClick = {
                                        cropTop = min(0.45f, cropTop + 0.15f)
                                        Toast.makeText(context, "ऊपर का हिस्सा काटा गया", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                CompactActionPill(
                                    icon = Icons.AutoMirrored.Filled.RotateRight,
                                    label = "↷ 90° घुमाएं",
                                    onClick = { rotate90() }
                                )

                                CompactActionPill(
                                    icon = Icons.Default.Refresh,
                                    label = "↺ रीसेट",
                                    onClick = {
                                        cropLeft = 0f
                                        cropTop = 0f
                                        cropRight = 1f
                                        cropBottom = 1f
                                        Toast.makeText(context, "क्रॉप रीसेट किया गया", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                CompactActionPill(
                                    icon = Icons.Default.Tune,
                                    label = if (showFineMargins) "📐 मार्जिन छुपाएं" else "📐 सटीक मार्जिन",
                                    containerColor = if (showFineMargins) Color(0xFF374151) else Color(0xFF0F172A),
                                    textColor = Color(0xFFF1F5F9),
                                    onClick = { showFineMargins = !showFineMargins }
                                )
                            }

                            // Optional Collapsible Fine Margin Sliders
                            AnimatedVisibility(
                                visible = showFineMargins,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    CompactTrimSliderRow(
                                        label = "⬆️ ऊपर:",
                                        value = (cropTop * 100).toInt(),
                                        onValueChange = { percent ->
                                            cropTop = (percent / 100f).coerceIn(0f, cropBottom - 0.1f)
                                        }
                                    )

                                    CompactTrimSliderRow(
                                        label = "⬇️ नीचे:",
                                        value = ((1f - cropBottom) * 100).toInt(),
                                        onValueChange = { percent ->
                                            cropBottom = (1f - (percent / 100f)).coerceIn(cropTop + 0.1f, 1f)
                                        }
                                    )

                                    CompactTrimSliderRow(
                                        label = "⬅️ बायां:",
                                        value = (cropLeft * 100).toInt(),
                                        onValueChange = { percent ->
                                            cropLeft = (percent / 100f).coerceIn(0f, cropRight - 0.1f)
                                        }
                                    )

                                    CompactTrimSliderRow(
                                        label = "➡️ दायां:",
                                        value = ((1f - cropRight) * 100).toInt(),
                                        onValueChange = { percent ->
                                            cropRight = (1f - (percent / 100f)).coerceIn(cropLeft + 0.1f, 1f)
                                        }
                                    )
                                }
                            }
                        } else if (currentMode == DiagramEditorMode.DRAW) {
                            // PEN & WHITEOUT DRAWING CONTROLS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color swatches
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    colorPalette.forEach { (c, _) ->
                                        val isSelected = selectedColor == c
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B),
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedColor = c },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (c == Color.White) Color.Black else Color.White)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Undo and Clear Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (strokeList.isNotEmpty()) {
                                                strokeList.removeAt(strokeList.lastIndex)
                                                Toast.makeText(context, "↩️ अनडू", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = strokeList.isNotEmpty(),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo",
                                            tint = if (strokeList.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFF475569),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (strokeList.isNotEmpty()) {
                                                strokeList.clear()
                                                Toast.makeText(context, "🗑️ ड्रॉइंग साफ", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = strokeList.isNotEmpty(),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear All",
                                            tint = if (strokeList.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF475569),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Thickness selector row (Compact Chips + Slider)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "मोटाई: ${selectedStrokeWidthDp.toInt()}px",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFCBD5E1),
                                    modifier = Modifier.width(62.dp)
                                )

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CompactThicknessDot(label = "बारीक", px = 4f, isSelected = selectedStrokeWidthDp == 4f, onClick = { selectedStrokeWidthDp = 4f })
                                    CompactThicknessDot(label = "मध्यम", px = 14f, isSelected = selectedStrokeWidthDp == 14f, onClick = { selectedStrokeWidthDp = 14f })
                                    CompactThicknessDot(label = "मोटा", px = 28f, isSelected = selectedStrokeWidthDp == 28f, onClick = { selectedStrokeWidthDp = 28f })
                                    CompactThicknessDot(label = "व्हाइट-आउट", px = 44f, isSelected = selectedStrokeWidthDp == 44f, onClick = {
                                        selectedStrokeWidthDp = 44f
                                        selectedColor = Color.White
                                    })
                                }

                                // Live Brush Preview Dot
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color(0xFF475569), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((selectedStrokeWidthDp * 0.35f).coerceIn(2f, 16f).dp)
                                            .clip(CircleShape)
                                            .background(selectedColor)
                                    )
                                }
                            }
                        } else {
                            // PREVIEW MODE INFO
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✨ यह फाइनल आकृति परीक्षा में दिखाई देगी",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF86EFAC)
                                )

                                OutlinedButton(
                                    onClick = { currentMode = DiagramEditorMode.CROP },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD)),
                                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("एडिट जारी रखें", fontSize = 10.5.sp)
                                }
                            }
                        }

                        // Bottom Actions Row: Delete Diagram & Cancel/Save
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onDeleteDiagram != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF450A0A),
                                    border = BorderStroke(1.dp, Color(0xFF991B1B)),
                                    modifier = Modifier.clickable { showDeleteConfirmDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(13.dp))
                                        Text("डायग्राम हटाएं", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                    border = BorderStroke(1.dp, Color(0xFF475569))
                                ) {
                                    Text("रद्द करें", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { saveCropped() },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("💾 सेव करें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Diagram Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(26.dp)
                    )
                },
                title = {
                    Text(
                        text = "डायग्राम हटाएं?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Text(
                        text = "क्या आप प्रश्न $questionNumber से यह डायग्राम हटाना चाहते हैं?",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteDiagram?.invoke()
                            Toast.makeText(context, "🗑️ डायग्राम हटा दिया गया", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("हाँ, हटाएं", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("रद्द करें", fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

@Composable
private fun CompactModeTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) activeColor else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun CompactActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color = Color(0xFF0F172A),
    textColor: Color = Color(0xFFE2E8F0),
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun CompactThicknessDot(
    label: String,
    px: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFF334155)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CompactTrimSliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.width(48.dp)
        )

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..45f,
            modifier = Modifier
                .weight(1f)
                .height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF0284C7),
                inactiveTrackColor = Color(0xFF334155)
            )
        )

        Text(
            text = "$value%",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            modifier = Modifier.width(28.dp)
        )
    }
}

/**
 * Draws a normalized DiagramStroke onto a Jetpack Compose DrawScope canvas.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNormalizedStroke(
    stroke: DiagramStroke,
    offsetX: Float,
    offsetY: Float,
    drawW: Float,
    drawH: Float,
    density: Float
) {
    if (stroke.points.isEmpty()) return

    val strokePx = (stroke.strokeWidthDp * density * (drawW / 360f)).coerceAtLeast(2f)

    if (stroke.points.size == 1) {
        val pt = stroke.points[0]
        drawCircle(
            color = stroke.color,
            radius = strokePx / 2f,
            center = Offset(offsetX + pt.x * drawW, offsetY + pt.y * drawH)
        )
    } else {
        val composePath = androidx.compose.ui.graphics.Path()
        val first = stroke.points[0]
        composePath.moveTo(offsetX + first.x * drawW, offsetY + first.y * drawH)
        for (i in 1 until stroke.points.size) {
            val pt = stroke.points[i]
            composePath.lineTo(offsetX + pt.x * drawW, offsetY + pt.y * drawH)
        }
        drawPath(
            path = composePath,
            color = stroke.color,
            style = Stroke(
                width = strokePx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

/**
 * Decodes Bitmap safely from base64, local file path, offline cache, or content Uri.
 */
private fun decodeBitmapFromSource(context: Context, source: String): Bitmap? {
    if (source.isBlank()) return null
    return try {
        when {
            source.startsWith("data:image", true) || (!source.startsWith("http") && !source.startsWith("/") && !source.startsWith("content://") && source.length > 50) -> {
                val clean = if (source.contains(",")) source.substringAfter(",") else source
                val bytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            source.startsWith("content://") -> {
                val uri = Uri.parse(source)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            source.startsWith("/") && File(source).exists() -> {
                BitmapFactory.decodeFile(source)
            }
            else -> {
                val cached = OfflineTestRepository.getOfflineImageFile(context, source)
                if (cached != null && cached.exists()) {
                    BitmapFactory.decodeFile(cached.absolutePath)
                } else if (source.startsWith("http", ignoreCase = true)) {
                    val url = java.net.URL(source)
                    BitmapFactory.decodeStream(url.openStream())
                } else {
                    null
                }
            }
        }
    } catch (_: Throwable) {
        null
    }
}
