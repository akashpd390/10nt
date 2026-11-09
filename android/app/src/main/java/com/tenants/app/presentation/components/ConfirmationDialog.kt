package com.tenants.app.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tenants.app.R
import com.tenants.app.presentation.theme.Primary
import com.tenants.app.presentation.theme.TenantsTheme

/**
 * Reusable confirmation dialog for critical operations (e.g. deleting rooms/tenants).
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.confirm_button),
    cancelText: String = stringResource(R.string.cancel_button),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        shape = RoundedCornerShape(16.dp),
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(9999.dp)
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(9999.dp)
            ) {
                Text(text = cancelText)
            }
        }
    )
}

@Preview(name = "Confirmation Dialog Preview", showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
    TenantsTheme {
        ConfirmationDialog(
            title = "Delete Tenant",
            message = "Are you sure you want to remove Rahul Sharma? Room 101 will become available.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
