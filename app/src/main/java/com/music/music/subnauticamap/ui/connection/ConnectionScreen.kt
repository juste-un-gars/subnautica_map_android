/**
 * @file ConnectionScreen.kt
 * @description Connection screen UI for configuring server connection
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.music.subnauticamap.R
import com.music.music.subnauticamap.ui.theme.SubnauticaColors

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel = viewModel(),
    onConnected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when connected
    if (uiState.connectionState is ConnectionState.Connected) {
        onConnected()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SubnauticaColors.OceanDeep,
                        SubnauticaColors.OceanMedium
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = stringResource(R.string.connection_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SubnauticaColors.BioluminescentBlue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Subnautica Map Companion",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Connection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SubnauticaColors.OceanLight.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // IP Address field
                    OutlinedTextField(
                        value = uiState.ipAddress,
                        onValueChange = viewModel::onIpAddressChange,
                        label = { Text(stringResource(R.string.ip_address_label)) },
                        placeholder = { Text(stringResource(R.string.ip_address_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SubnauticaColors.BioluminescentBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = SubnauticaColors.BioluminescentBlue,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = SubnauticaColors.BioluminescentBlue
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        enabled = uiState.connectionState !is ConnectionState.Connecting
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Port field
                    OutlinedTextField(
                        value = uiState.port,
                        onValueChange = viewModel::onPortChange,
                        label = { Text(stringResource(R.string.port_label)) },
                        placeholder = { Text(stringResource(R.string.port_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SubnauticaColors.BioluminescentBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = SubnauticaColors.BioluminescentBlue,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = SubnauticaColors.BioluminescentBlue
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = uiState.connectionState !is ConnectionState.Connecting
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Keep screen on switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Keep screen on",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                        Switch(
                            checked = uiState.keepScreenOn,
                            onCheckedChange = viewModel::onKeepScreenOnChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SubnauticaColors.BioluminescentBlue,
                                checkedTrackColor = SubnauticaColors.BioluminescentBlue.copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Connect button
                    Button(
                        onClick = viewModel::connect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = uiState.isInputValid &&
                                uiState.connectionState !is ConnectionState.Connecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SubnauticaColors.BioluminescentBlue,
                            disabledContainerColor = SubnauticaColors.BioluminescentBlue.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.connectionState is ConnectionState.Connecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.testing_connection),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.connect_button),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Status message
                    Spacer(modifier = Modifier.height(16.dp))

                    when (val state = uiState.connectionState) {
                        is ConnectionState.Error -> {
                            Text(
                                text = state.message,
                                color = SubnauticaColors.WarningRed,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        is ConnectionState.Connected -> {
                            Text(
                                text = "${stringResource(R.string.connection_success)} v${state.version}",
                                color = SubnauticaColors.BioluminescentGreen,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Help text
            Text(
                text = "Make sure Subnautica is running with the MapAPI mod\nand your device is on the same WiFi network.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
