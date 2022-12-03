package screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import exceptions.UniqueNameNotFound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GetPathScreen() {
    Row {
        val uniqueName = remember { mutableStateOf("") }
        val result = remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

        Column {
            TextField(value = uniqueName.value, onValueChange = {
                uniqueName.value = it
            }, placeholder = { Text("Введите идентификатор") })

            SelectionContainer {
                Text(result.value)
            }
        }

        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                result.value = "loading..."
                try {
                    result.value = BackupService.getPath(uniqueName.value)
                } catch (e: UniqueNameNotFound) {
                    result.value = "Файла с идентификатором \"${uniqueName.value}\" не существует"
                }
            }
        }) {
            Text("Get")
        }
    }
}