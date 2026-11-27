package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.HerbRemedy
import com.example.model.HerbalCatalogEntry
import java.text.DateFormat
import java.util.Locale

fun HerbalCatalogEntry.toHerbRemedy(): HerbRemedy = HerbRemedy(
    id = id,
    name = name,
    properties = indications,
    components = ingredientsSummary,
    instructions = preparationAndDosage,
    precautions = precautions,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HerbRemedyDetailView(
    remedy: HerbRemedy,
    showBackButton: Boolean = true,
    isMutating: Boolean = false,
    onBack: () -> Unit = {},
    onEdit: ((HerbRemedy) -> Unit)? = null,
    onDelete: ((HerbRemedy) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("herb_remedy_detail_view"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (showBackButton) WindowInsets.safeDrawing else WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = remedy.name.ifBlank { "รายละเอียดสมุนไพร" },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("remedy_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "ย้อนกลับ"
                            )
                        }
                    }
                },
                actions = {
                    if (onEdit != null) {
                        IconButton(
                            onClick = { onEdit(remedy) },
                            enabled = !isMutating,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("remedy_detail_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "แก้ไขข้อมูลสมุนไพร"
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !isMutating,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("remedy_detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "ลบข้อมูลสมุนไพร",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_herb_leaves),
                    contentDescription = remedy.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "ข้อมูลตำรับสมุนไพร (HerbRemedy)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = remedy.name.ifBlank { "ไม่ระบุชื่อสมุนไพร" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier.testTag("remedy_detail_name")
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                remedy.updatedAt?.toDate()?.let { updated ->
                    val formatted = remember(updated) {
                        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(updated)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "อัปเดตล่าสุด: $formatted",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            RemedyDetailSectionCard(
                icon = Icons.Rounded.LocalPharmacy,
                title = "สรรพคุณ (Properties)",
                body = remedy.properties.ifBlank { "ยังไม่ได้ระบุสรรพคุณ" },
                modifier = Modifier.testTag("remedy_detail_properties")
            )

            RemedyDetailSectionCard(
                icon = Icons.Rounded.Spa,
                title = "ส่วนประกอบ (Components)",
                body = remedy.components.ifBlank { "ยังไม่ได้ระบุส่วนประกอบ" },
                modifier = Modifier.testTag("remedy_detail_components")
            )

            RemedyDetailSectionCard(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                title = "วิธีใช้และขนาดรับประทาน (Instructions)",
                body = remedy.instructions.ifBlank { "ยังไม่ได้ระบุวิธีใช้" },
                modifier = Modifier.testTag("remedy_detail_instructions")
            )

            val hasPrecautions = remedy.precautions.isNotBlank()
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPrecautions) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("remedy_detail_precautions")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (hasPrecautions) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = "ข้อควรระวัง (Precautions)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (hasPrecautions) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (hasPrecautions) remedy.precautions else "ไม่มีข้อควรระวังเพิ่มเติมที่ระบุไว้",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hasPrecautions) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (onEdit != null || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onEdit != null) {
                        FilledTonalButton(
                            onClick = { onEdit(remedy) },
                            enabled = !isMutating,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("remedy_detail_edit_action")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("แก้ไขข้อมูล")
                        }
                    }
                    if (onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !isMutating,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("remedy_detail_delete_action")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("ลบข้อมูล", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Text(
                text = "คำแนะนำ: ข้อมูลนี้ใช้เพื่อการศึกษาและอ้างอิงเบื้องต้น หากมีโรคประจำตัว ตั้งครรภ์ หรือใช้ยาแผนปัจจุบันร่วมด้วย ควรปรึกษาแพทย์แผนไทยหรือเภสัชกร",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("ยืนยันการลบข้อมูลสมุนไพร") },
            text = { Text("คุณต้องการลบ \"${remedy.name}\" ออกจากคลังข้อมูลใช่หรือไม่?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(remedy)
                    }
                ) {
                    Text("ลบข้อมูล")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
fun HerbRemedyListCard(
    remedy: HerbRemedy,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("herb_remedy_card_${remedy.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFlorist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = remedy.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = remedy.properties,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ส่วนประกอบ: ${remedy.components}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "ดูรายละเอียด",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HerbRemedyEditorDialog(
    initialRemedy: HerbRemedy?,
    isSaving: Boolean,
    onSave: (HerbRemedy) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialRemedy) { mutableStateOf(initialRemedy?.name.orEmpty()) }
    var properties by remember(initialRemedy) { mutableStateOf(initialRemedy?.properties.orEmpty()) }
    var components by remember(initialRemedy) { mutableStateOf(initialRemedy?.components.orEmpty()) }
    var instructions by remember(initialRemedy) { mutableStateOf(initialRemedy?.instructions.orEmpty()) }
    var precautions by remember(initialRemedy) { mutableStateOf(initialRemedy?.precautions.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(if (initialRemedy == null) "เพิ่มข้อมูลสมุนไพร (HerbRemedy)" else "แก้ไขข้อมูลสมุนไพร")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                validationError?.let { errorMsg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 120) name = it },
                    label = { Text("ชื่อสมุนไพร / ตำรับยา *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remedy_editor_name")
                )
                OutlinedTextField(
                    value = properties,
                    onValueChange = { if (it.length <= 800) properties = it },
                    label = { Text("สรรพคุณ (Properties) *") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remedy_editor_properties")
                )
                OutlinedTextField(
                    value = components,
                    onValueChange = { if (it.length <= 1000) components = it },
                    label = { Text("ส่วนประกอบ (Components) *") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remedy_editor_components")
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { if (it.length <= 800) instructions = it },
                    label = { Text("วิธีใช้ (Instructions) *") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remedy_editor_instructions")
                )
                OutlinedTextField(
                    value = precautions,
                    onValueChange = { if (it.length <= 600) precautions = it },
                    label = { Text("ข้อควรระวัง (Precautions)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remedy_editor_precautions")
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    when {
                        name.trim().isEmpty() -> validationError = "กรุณาระบุชื่อสมุนไพร"
                        properties.trim().isEmpty() -> validationError = "กรุณาระบุสรรพคุณ"
                        components.trim().isEmpty() -> validationError = "กรุณาระบุส่วนประกอบ"
                        instructions.trim().isEmpty() -> validationError = "กรุณาระบุวิธีใช้"
                        else -> {
                            validationError = null
                            onSave(
                                HerbRemedy(
                                    id = initialRemedy?.id.orEmpty(),
                                    userId = initialRemedy?.userId.orEmpty(),
                                    name = name.trim(),
                                    properties = properties.trim(),
                                    components = components.trim(),
                                    instructions = instructions.trim(),
                                    precautions = precautions.trim()
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("remedy_editor_save")
            ) {
                Text(if (isSaving) "กำลังบันทึก..." else "บันทึก")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("ยกเลิก")
            }
        }
    )
}

@Composable
private fun RemedyDetailSectionCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
