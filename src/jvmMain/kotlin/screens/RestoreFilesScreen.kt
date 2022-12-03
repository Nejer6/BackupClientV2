package screens

import BackupService
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RestoreFilesScreen() {
    Row {
        val path = remember { mutableStateOf("") }
        val result = remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

        Column {
            TextField(value = path.value, onValueChange = {
                path.value = it
            }, placeholder = { Text("Путь до папки сохранения (необязательно)") })

            Text(result.value)
        }

        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                result.value = "downloading..."

                BackupService.restoreFiles(path.value.ifBlank { null })

                result.value = "Done"
            }
        }) {
            Text("Download")
        }
    }
}