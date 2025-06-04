package com.example.uhdlogger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class Setting(
    val key: String,
    val label: String,
    val defaultValue: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
) {
    val settingsDataStore = AppSettingsDataStore.instance
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            }
    ) {
        val coroutineScope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            settingsList.forEach { setting ->
                var text by remember(setting.key) {
                    mutableStateOf(setting.defaultValue)
                }

                var isFocused by remember { mutableStateOf(false) }

                LaunchedEffect(setting.key) {
                    settingsDataStore.getSetting(setting.key, setting.defaultValue)
                        .collect { storedValue ->
                            text = storedValue
                        }
                }

                Text(text = setting.label)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            coroutineScope.launch {
                                settingsDataStore.saveSetting(setting.key, text)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .onFocusChanged { focusState ->
                            if (isFocused && !focusState.isFocused) {
                                coroutineScope.launch {
                                    settingsDataStore.saveSetting(setting.key, text)
                                }
                            }
                            isFocused = focusState.isFocused
                        }
                )
            }
        }

    }
}


@Preview(showBackground = true)
@Composable
fun PreviewSettingsPage(){
    SettingsPage(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)
    )
}