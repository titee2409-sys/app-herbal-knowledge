package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.HerbalCatalogEntry
import com.example.model.HerbalCategory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormulaEditorDialog(
    initialEntry: HerbalCatalogEntry?,
    isCloneMode: Boolean,
    isSaving: Boolean,
    onSave: (
        existingId: String?,
        name: String,
        englishName: String,
        category: HerbalCategory,
        tasteProfile: String,
        indications: String,
        ingredientsSummary: String,
        preparationAndDosage: String,
        precautions: String,
        referenceSource: String,
        externalUse: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val existingId = if (isCloneMode) null else initialEntry?.id
    var name by remember(initialEntry, isCloneMode) {
        mutableStateOf(
            when {
                initialEntry == null -> ""
                isCloneMode -> "${initialEntry.name} (ฉบับปรับปรุง)".take(100)
                else -> initialEntry.name
            }
        )
    }
    var englishName by remember(initialEntry) { mutableStateOf(initialEntry?.englishName.orEmpty()) }
    var category by remember(initialEntry) { mutableStateOf(initialEntry?.category ?: HerbalCategory.CUSTOM) }
    var tasteProfile by remember(initialEntry) { mutableStateOf(initialEntry?.tasteProfile.orEmpty()) }
    var indications by remember(initialEntry) { mutableStateOf(initialEntry?.indications.orEmpty()) }
    var ingredientsSummary by remember(initialEntry) { mutableStateOf(initialEntry?.ingredientsSummary.orEmpty()) }
    var preparationAndDosage by remember(initialEntry) { mutableStateOf(initialEntry?.preparationAndDosage.orEmpty()) }
    var precautions by remember(initialEntry) { mutableStateOf(initialEntry?.precautions.orEmpty()) }
    var referenceSource by remember(initialEntry) {
        mutableStateOf(
            initialEntry?.referenceSource?.ifBlank { "บันทึกตำรับส่วนตัว" } ?: "บันทึกตำรับส่วนตัว"
        )
    }
    var externalUse by remember(initialEntry) { mutableStateOf(initialEntry?.externalUse ?: false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val quickTastes = listOf("ขม", "ฝาด", "เผ็ดร้อน", "หอมเย็น", "เปรี้ยว", "หวาน", "เค็ม")

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(enabled = !isSaving, onBack = onDismiss)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                existingId != null -> "แก้ไขตำรับยา"
                                isCloneMode -> "คัดลอกและปรับปรุงตำรับ"
                                else -> "เพิ่มตำรับยาใหม่"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (!isSaving) onDismiss() },
                            modifier = Modifier.testTag("editor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "ยกเลิกและย้อนกลับ"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (validationError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 100) name = it },
                        label = { Text("ชื่อตำรับยา *") },
                        placeholder = { Text("เช่น ยาต้มแก้ไข้สูตรบ้าน") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("formula_name_input")
                    )

                    OutlinedTextField(
                        value = englishName,
                        onValueChange = { if (it.length <= 100) englishName = it },
                        label = { Text("ชื่อภาษาอังกฤษ (ถ้ามี)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "หมวดหมู่ตำรับยา",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HerbalCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.shortLabel) },
                                leadingIcon = if (category == cat) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tasteProfile,
                        onValueChange = { if (it.length <= 60) tasteProfile = it },
                        label = { Text("รสยา (เช่น รสขม หอมเย็น)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTastes.forEach { t ->
                            AssistChip(
                                onClick = {
                                    val next = if (tasteProfile.isBlank()) "รส$t" else "$tasteProfile $t"
                                    if (next.length <= 60) tasteProfile = next
                                },
                                label = { Text("+ $t") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = indications,
                        onValueChange = { if (it.length <= 600) indications = it },
                        label = { Text("สรรพคุณ *") },
                        placeholder = { Text("ระบุอาการหรือโรคที่ตำรับยานี้ใช้บรรเทา") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("formula_indications_input")
                    )

                    OutlinedTextField(
                        value = ingredientsSummary,
                        onValueChange = { if (it.length <= 1000) ingredientsSummary = it },
                        label = { Text("ส่วนประกอบและอัตราส่วน *") },
                        placeholder = { Text("ระบุชื่อสมุนไพรและน้ำหนักหรือส่วนที่ใช้") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("formula_ingredients_input")
                    )

                    OutlinedTextField(
                        value = preparationAndDosage,
                        onValueChange = { if (it.length <= 800) preparationAndDosage = it },
                        label = { Text("วิธีปรุงและขนาดรับประทาน/วิธีใช้ *") },
                        placeholder = { Text("ระบุวิธีต้ม ชง หรือทา และขนาดที่ใช้ต่อครั้ง") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("formula_preparation_input")
                    )

                    OutlinedTextField(
                        value = precautions,
                        onValueChange = { if (it.length <= 600) precautions = it },
                        label = { Text("ข้อควรระวัง / ข้อห้ามใช้") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = referenceSource,
                        onValueChange = { if (it.length <= 150) referenceSource = it },
                        label = { Text("ที่มา / ตำราอ้างอิง") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ยาใช้ภายนอกเท่านั้น",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "เปิดใช้งานหากเป็นยาพอก ยาหม่อง หรือน้ำมันนวด",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = externalUse,
                                onCheckedChange = { externalUse = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            when {
                                name.trim().isEmpty() -> validationError = "กรุณาระบุชื่อตำรับยา"
                                indications.trim().isEmpty() -> validationError = "กรุณาระบุสรรพคุณของตำรับยา"
                                ingredientsSummary.trim().isEmpty() -> validationError = "กรุณาระบุส่วนประกอบสมุนไพร"
                                preparationAndDosage.trim().isEmpty() -> validationError = "กรุณาระบุวิธีปรุงและวิธีใช้"
                                else -> {
                                    validationError = null
                                    onSave(
                                        existingId,
                                        name,
                                        englishName,
                                        category,
                                        tasteProfile,
                                        indications,
                                        ingredientsSummary,
                                        preparationAndDosage,
                                        precautions,
                                        referenceSource,
                                        externalUse
                                    )
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("save_formula_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("บันทึกตำรับยาลงคลาวด์")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
