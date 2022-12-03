package screens

import BackupService
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.ktor.network.selector.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

@Composable
fun AddFileScreen() {
    Row {
        val path = remember { mutableStateOf("") }
        val result = remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

        Column {

            TextField(value = path.value, onValueChange = {
                path.value = it
            }, placeholder = { Text("Введите путь до файла") })

            SelectionContainer {
                Text(result.value)
            }
        }

        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                result.value = "loading..."
                // TODO: 01.12.2022 Перехват ошибок
                try {
                    result.value = BackupService.addNewFile(path.value)
                } catch (e: FileNotFoundException) {
                    result.value = "Файл не найден"
                }

            }
        }) {
            Text("Post")
        }
    }
}