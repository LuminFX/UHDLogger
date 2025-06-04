package com.example.uhdlogger

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UHDLogPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsDataStore = AppSettingsDataStore.instance
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var logOutput by remember { mutableStateOf("") }
    var isLogging by remember { mutableStateOf(false) }

    fun appendLog(line: String) {
        logOutput += line + "\n"
        coroutineScope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun extractScriptFromAssets(context: Context, name: String): File {
        val outFile = File(context.filesDir, name)
        if (!outFile.exists()) {
            context.assets.open(name).use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            Runtime.getRuntime().exec(arrayOf("su", "-c", "chmod 755 ${outFile.absolutePath}"))
        }
        return outFile
    }

    fun runUhdCommand(args: List<String>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val startupScript = extractScriptFromAssets(context, "start_and_run_UHD.sh")
                val runnerScript = extractScriptFromAssets(context, "run_uhd.sh")

                val command = listOf("su", "-c", "$startupScript ${args.joinToString(" ")}")
                val process = ProcessBuilder(command).redirectErrorStream(true).start()

                val fullCommand = "$startupScript ${args.joinToString(" ")}"
                appendLog("[DEBUG] Running: $fullCommand")

                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { appendLog(it) }
                    }
                }

                val exitCode = process.waitFor()
                appendLog("\nExited with code $exitCode")
            } catch (e: Exception) {
                appendLog("Error: ${e.message}")
            } finally {
                isLogging = false
            }
        }
    }

    fun launchLogging() {
        coroutineScope.launch {
            isLogging = true
            logOutput = "[*] Starting UHD logging...\n"

            val settings = settingsList.map { setting ->
                settingsDataStore.getSetting(setting.key, setting.defaultValue).first()
            }

            runUhdCommand(settings)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Text("Live Log Output", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                Text(text = logOutput)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { launchLogging() },
                enabled = !isLogging,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(if (isLogging) "Logging..." else "Start Log")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UHDLogPagePreview(){
    UHDLogPage(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)
    )
}