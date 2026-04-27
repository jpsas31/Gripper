package com.whc06.trainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CfTutorialDialog(onProceed: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Critical Force Test") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("All-out test per Giles et al. 2010.", fontWeight = FontWeight.SemiBold)
                Text("• 24 reps × 7s pull / 3s rest", fontSize = 13.sp)
                Text("• Pull max effort each rep", fontSize = 13.sp)
                Text("• Don't pace — go all-out from rep 1", fontSize = 13.sp)
                Text("• Force will plateau as you fatigue", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("Result", fontWeight = FontWeight.SemiBold)
                Text("CF = mean of last 6 reps (outliers >1 SD removed).", fontSize = 13.sp)
                Text("W' = total work above CF (kg·s).", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("Total ~4 minutes. No pacing. Brutal.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = { TextButton(onClick = onProceed) { Text("Start Test") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("Skip Tutorial") } }
    )
}
