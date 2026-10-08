package com.cephcoding.tyde.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cephcoding.tyde.core.designsystem.theme.TydeSpacing

@Composable
fun TydeCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(TydeSpacing.medium)) {
            content()
        }
    }
}
