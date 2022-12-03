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
import exceptions.NameAlreadyTaken
import exceptions.UniqueNameNotFound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RenameFileScreen() {
    Row {
        val oldName = remember { mutableStateOf("") }
        val newName = remember { mutableStateOf("") }
        val result = remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

        Column {
            TextField(value = oldName.value, onValueChange = {
                oldName.value = it
            }, placeholder = { Text("Старый идентификатор") })

            TextField(value = newName.value, onValueChange = {
                newName.value = it
            }, placeholder = { Text("Новый идентификатор") })

            SelectionContainer {
                Text(result.value)
            }
        }

        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                result.value = "loading..."
                try {
                    BackupService.renameFile(oldName.value, newName.value)
                    result.value = "OK"
                } catch (e: UniqueNameNotFound) {
                    result.value = "Файла с идентификатором \"${oldName.value}\" не существует"
                } catch (e: NameAlreadyTaken) {
                    result.value = "\"${newName.value}\" уже существует"
                }
            }
        }) {
            Text("Rename")
        }
    }
}