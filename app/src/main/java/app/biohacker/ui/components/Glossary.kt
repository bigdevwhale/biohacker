package app.biohacker.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.biohacker.ui.Words
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T

/** The «?» in a top bar: opens the glossary. */
@Composable
fun HelpButton(onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clickable(onClick = onClick).padding(9.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(22.dp).border(1.dp, C.muted), contentAlignment = Alignment.Center) { Text("?", style = T.Code.copy(color = C.muted)) }
    }
}

/** Every term the app uses, in a sheet; [section] is the screen it was opened from and goes first. */
@Composable
fun GlossarySheet(w: Words, section: String?, onClose: () -> Unit) {
    BottomSheet(visible = section != null, onClose = onClose) {
        Text(w.t("СПРАВОЧНИК", "GLOSSARY"), style = T.Label.copy(color = C.amber))
        for ((title, terms) in w.glossary(section)) {
            Text(title, style = T.Title, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            for ((term, text) in terms) Column(Modifier.padding(vertical = 5.dp)) {
                Text(term, style = T.BodyM.copy(color = C.ink))
                Text(text, style = T.Small)
            }
        }
        Btn(w.t("Понятно", "Got it"), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = onClose)
        Gap(6)
    }
}
