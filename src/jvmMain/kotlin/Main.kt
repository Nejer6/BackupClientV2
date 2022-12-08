// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import screens.*
import java.util.StringJoiner

const val MILLISEC_IN_DAY = 86_400_000L

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val coroutineScope = rememberCoroutineScope()
        coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                BackupService.createBackup()
                delay(MILLISEC_IN_DAY)
            }
        }
        MainScreen()
    }
}

@Composable
fun MainScreen() {
    Row {
        val map = listOf<Pair<String, @Composable () -> Unit>>(
            "Добавить файл" to { AddFileScreen() },
            "Получить путь до файла" to { GetPathScreen() },
            "Удалить файл" to { DeleteFileScreen() },
            "Переименовать идентификатор" to { RenameFileScreen() },
            "Получить пути к файлам" to { GetPathsScreen() },
            "Восстановить файлы" to { RestoreFilesScreen() }
        )

        val (selectedOption, onOptionSelected) = remember { mutableStateOf(map[0]) }
        val page = remember { mutableStateOf<@Composable () -> Unit>( map.first().second ) }

        Column(modifier = Modifier.fillMaxHeight().selectableGroup()) {
            map.forEach { pair ->
                Row {
                    RadioButton(
                        selected = pair.first == selectedOption.first,
                        onClick = {
                            page.value = pair.second
                            onOptionSelected(pair)
                        }
                    )
                    Text(pair.first)
                }

            }
        }

        page.value()

    }
}
