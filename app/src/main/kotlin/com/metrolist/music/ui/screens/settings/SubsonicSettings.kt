package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.SubsonicEnabledKey
import com.metrolist.music.constants.SubsonicMaxBitRateKey
import com.metrolist.music.constants.SubsonicPasswordKey
import com.metrolist.music.constants.SubsonicServerUrlKey
import com.metrolist.music.constants.SubsonicUsernameKey
import com.metrolist.subsonic.Subsonic
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.PreferenceGroupTitle
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.ui.component.SwitchPreference
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubsonicSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (subsonicEnabled, onSubsonicEnabledChange) = rememberPreference(key = SubsonicEnabledKey, defaultValue = false)
    val (serverUrl, onServerUrlChange) = rememberPreference(key = SubsonicServerUrlKey, defaultValue = "")
    val (username, onUsernameChange) = rememberPreference(key = SubsonicUsernameKey, defaultValue = "")
    val (password, onPasswordChange) = rememberPreference(key = SubsonicPasswordKey, defaultValue = "")
    val (maxBitRate, onMaxBitRateChange) = rememberPreference(key = SubsonicMaxBitRateKey, defaultValue = 320)

    var showConfigDialog by rememberSaveable { mutableStateOf(false) }
    var testResult by rememberSaveable { mutableStateOf<String?>(null) }

    if (showConfigDialog) {
        var tempServerUrl by rememberSaveable { mutableStateOf(serverUrl) }
        var tempUsername by rememberSaveable { mutableStateOf(username) }
        var tempPassword by rememberSaveable { mutableStateOf(password) }
        var isTesting by remember { mutableStateOf(false) }
        var testStatus by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text(stringResource(R.string.subsonic_server_config)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        label = { Text(stringResource(R.string.subsonic_server_url)) },
                        placeholder = { Text("https://your-server.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it },
                        label = { Text(stringResource(R.string.username)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                testStatus = context.getString(R.string.testing_connection)
                                
                                // Update Subsonic client configuration
                                Subsonic.serverUrl = tempServerUrl.trimEnd('/')
                                Subsonic.username = tempUsername
                                Subsonic.password = tempPassword
                                
                                val result = Subsonic.ping()
                                testStatus = if (result.isSuccess) {
                                    context.getString(R.string.connection_successful)
                                } else {
                                    context.getString(R.string.connection_failed) + ": ${result.exceptionOrNull()?.message}"
                                }
                                isTesting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTesting && tempServerUrl.isNotBlank() && tempUsername.isNotBlank() && tempPassword.isNotBlank()
                    ) {
                        Text(stringResource(R.string.test_connection))
                    }

                    testStatus?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = status)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onServerUrlChange(tempServerUrl.trimEnd('/'))
                        onUsernameChange(tempUsername)
                        onPasswordChange(tempPassword)
                        
                        // Update Subsonic client
                        Subsonic.serverUrl = tempServerUrl.trimEnd('/')
                        Subsonic.username = tempUsername
                        Subsonic.password = tempPassword
                        
                        showConfigDialog = false
                    },
                    enabled = tempServerUrl.isNotBlank() && tempUsername.isNotBlank() && tempPassword.isNotBlank()
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceGroupTitle(title = stringResource(R.string.subsonic_settings))

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_subsonic)) },
            description = stringResource(R.string.enable_subsonic_description),
            icon = { painterResource(R.drawable.sync) },
            checked = subsonicEnabled,
            onCheckedChange = { 
                onSubsonicEnabledChange(it)
                if (it && (serverUrl.isBlank() || username.isBlank() || password.isBlank())) {
                    showConfigDialog = true
                }
            }
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.server_configuration)) },
            description = if (serverUrl.isNotBlank()) serverUrl else stringResource(R.string.not_configured),
            icon = { painterResource(R.drawable.settings) },
            onClick = { showConfigDialog = true }
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.max_bitrate)) },
            description = "$maxBitRate kbps",
            icon = { painterResource(R.drawable.speed) },
            onClick = { /* TODO: Show bitrate selector dialog */ }
        )

        if (serverUrl.isNotBlank() && username.isNotBlank()) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.test_connection)) },
                description = stringResource(R.string.verify_server_connection),
                icon = { painterResource(R.drawable.sync) },
                onClick = {
                    scope.launch {
                        Subsonic.serverUrl = serverUrl
                        Subsonic.username = username
                        Subsonic.password = password
                        
                        val result = Subsonic.ping()
                        testResult = if (result.isSuccess) {
                            context.getString(R.string.connection_successful)
                        } else {
                            context.getString(R.string.connection_failed) + ": ${result.exceptionOrNull()?.message}"
                        }
                    }
                }
            )
            
            testResult?.let { result ->
                Text(
                    text = result,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.subsonic_settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                painterResource(R.drawable.arrow_back)
            }
        },
        scrollBehavior = scrollBehavior
    )
}
