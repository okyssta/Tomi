package com.tomi.app.ui.components

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics

@Composable
fun SettingsToggleRow(
    title: String,
    supportingText: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = modifier
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics(mergeDescendants = true) { },
    )
}
