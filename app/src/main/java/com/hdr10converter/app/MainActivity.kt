package com.hdr10converter.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HDR10ConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0F)
                ) {
                    HDRConverterScreen()
                }
            }
        }
    }
}

@Composable
fun HDR10ConverterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4F9BFF),
            secondary = Color(0xFF7C3FFF),
            surface = Color(0xFF151520),
            background = Color(0xFF0A0A0F)
        ),
        content = content
    )
}

@Composable
fun HDRConverterScreen() {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("") }
    var expansion by remember { mutableStateOf(100f) } // npl value
    var isConverting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var outputPath by remember { mutableStateOf("") }
    
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms.values.all { it }
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedVideoUri = it
            videoName = it.lastPathSegment ?: "video.mp4"
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Header
        Text(
            text = "HDR10+ Converter",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "SDR → HDR10 • No Resolution Loss",
            fontSize = 14.sp,
            color = Color(0xFF8B8B9A),
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Video Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!hasPermission && Build.VERSION.SDK_INT < 33) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                    } else if (!hasPermission && Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
                    } else {
                        videoPickerLauncher.launch("video/*")
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151520))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF4F9BFF), Color(0xFF7C3FFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (selectedVideoUri == null) "Pilih Video" else videoName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = if (selectedVideoUri == null) "Tap untuk memilih file video" else "Tap untuk ganti video",
                    fontSize = 13.sp,
                    color = Color(0xFF8B8B9A),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Expansion Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151520))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expansion (npl)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "${expansion.toInt()} nit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4F9BFF)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Atur kekuatan highlight expansion",
                    fontSize = 12.sp,
                    color = Color(0xFF8B8B9A)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = expansion,
                    onValueChange = { expansion = it },
                    valueRange = 50f..150f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4F9BFF),
                        activeTrackColor = Color(0xFF4F9BFF),
                        inactiveTrackColor = Color(0xFF2A2A3A)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("50", fontSize = 11.sp, color = Color(0xFF8B8B9A))
                    Text("100", fontSize = 11.sp, color = Color(0xFF8B8B9A))
                    Text("150", fontSize = 11.sp, color = Color(0xFF8B8B9A))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Convert Button
        Button(
            onClick = {
                if (selectedVideoUri == null) {
                    Toast.makeText(context, "Pilih video terlebih dahulu", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (isConverting) return@Button
                convertVideo(context, selectedVideoUri!!, expansion.toInt(), { isConverting = it }, { progress = it }, { outputPath = it })
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4F9BFF)
            ),
            enabled = selectedVideoUri != null && !isConverting
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConverting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mengkonversi...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(imageVector = Icons.Default.Transform, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konversi ke HDR10", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        
        if (isConverting && progress.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = progress,
                fontSize = 12.sp,
                color = Color(0xFF8B8B9A),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        if (outputPath.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1F0F))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Berhasil!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4ADE80)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tersimpan di: $outputPath",
                        fontSize = 12.sp,
                        color = Color(0xFF8B8B9A)
                    )
                }
            }
        }
    }
}

fun convertVideo(
    context: android.content.Context,
    uri: Uri,
    npl: Int,
    onConvertingChange: (Boolean) -> Unit,
    onProgress: (String) -> Unit,
    onOutput: (String) -> Unit
) {
    onConvertingChange(true)
    onProgress("Mempersiapkan file...")
    
    try {
        val inputFile = File(context.cacheDir, "input_temp.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            inputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(context.getExternalFilesDir(null), "HDR10_${timeStamp}.mp4")
        
        val command = "-i \"${inputFile.absolutePath}\" -vf \"zscale=t=linear:npl=$npl,format=gbrpf32le,zscale=p=bt2020:t=smpte2084:m=bt2020nc:r=tv,format=yuv420p10le\" -c:v libx265 -preset slow -crf 16 -x265-params \"hdr-opt=1:repeat-headers=1:colorprim=bt2020:transfer=smpte2084:colormatrix=bt2020nc:master-display=G(13250,34500)B(7500,3000)R(34000,16000)WP(15635,16450)L(10000000,1):max-cll=1000,400\" -c:a copy \"${outputFile.absolutePath}\""
        
        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                onProgress("")
                onOutput(outputFile.absolutePath)
                Toast.makeText(context, "Konversi berhasil!", Toast.LENGTH_LONG).show()
            } else {
                onProgress("Gagal: ${session.failStackTrace}")
                Toast.makeText(context, "Gagal dikonversi", Toast.LENGTH_SHORT).show()
            }
            onConvertingChange(false)
            inputFile.delete()
        }
    } catch (e: Exception) {
        onProgress("Error: ${e.message}")
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        onConvertingChange(false)
    }
}
