package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.signOut
import com.example.model.HerbRemedy
import com.example.model.HerbalCatalogEntry
import com.example.model.HerbalCategory
import com.example.model.UiState
import com.example.ui.CatalogSourceFilter
import com.example.ui.DatabaseTab
import com.example.ui.HerbRemedyViewModel
import com.example.ui.HerbalViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerbalDatabaseApp(
    viewModel: HerbalViewModel,
    userEmail: String?,
    remedyViewModel: HerbRemedyViewModel? = null,
    modifier: Modifier = Modifier
) {
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val fallbackRemediesState = remember { MutableStateFlow<UiState<List<HerbRemedy>>>(UiState.Success(emptyList())) }
    val fallbackMutatingState = remember { MutableStateFlow(false) }
    val fallbackFeedbackState = remember { MutableStateFlow<String?>(null) }

    val remediesState by (remedyViewModel?.remediesState ?: fallbackRemediesState).collectAsStateWithLifecycle()
    val isRemedyMutating by (remedyViewModel?.isMutating ?: fallbackMutatingState).collectAsStateWithLifecycle()
    val remedyFeedback by (remedyViewModel?.feedbackMessage ?: fallbackFeedbackState).collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedFormulaId by remember { mutableStateOf<String?>(null) }
    var selectedRemedyId by remember { mutableStateOf<String?>(null) }
    var editorTarget by remember { mutableStateOf<Pair<HerbalCatalogEntry?, Boolean>?>(null) }
    var remedyEditorTarget by remember { mutableStateOf<Pair<Boolean, HerbRemedy?>?>(null) }
    var showTasteGuide by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var categoriesExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearFeedback()
    }

    LaunchedEffect(remedyFeedback) {
        val msg = remedyFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        remedyViewModel?.clearFeedback()
    }

    val allEntries = (catalogState as? UiState.Success)?.data.orEmpty()
    val selectedEntry = remember(allEntries, selectedFormulaId) {
        allEntries.firstOrNull { it.id == selectedFormulaId }
    }
    val filteredRemediesList = (remediesState as? UiState.Success)?.data.orEmpty()
    val selectedRemedy = remember(filteredRemediesList, selectedRemedyId) {
        filteredRemediesList.firstOrNull { it.id == selectedRemedyId }
    }

    val categoryCounts = remember(allEntries) {
        HerbalCategory.entries.associateWith { cat ->
            allEntries.count { it.category == cat }
        }
    }

    val searchMatchingRemedies = remember(allEntries, filterState.searchQuery) {
        val query = filterState.searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            allEntries.take(6)
        } else {
            allEntries.filter { item ->
                item.name.lowercase().contains(query) ||
                    item.englishName.lowercase().contains(query) ||
                    item.scientificName.lowercase().contains(query) ||
                    item.botanicalFamily.lowercase().contains(query) ||
                    item.partUsed.lowercase().contains(query) ||
                    item.indications.lowercase().contains(query) ||
                    item.ingredientsSummary.lowercase().contains(query) ||
                    item.tasteProfile.lowercase().contains(query) ||
                    item.category.thaiLabel.lowercase().contains(query)
            }
        }
    }

    val filteredEntries = remember(allEntries, filterState) {
        val query = filterState.searchQuery.trim().lowercase()
        allEntries.filter { item ->
            val tabMatches = when (filterState.activeTab) {
                DatabaseTab.CATALOG -> true
                DatabaseTab.SAVED -> item.favorite || item.personalNote.isNotBlank()
                DatabaseTab.MY_FORMULAS -> item.customOwned
                DatabaseTab.REMEDIES -> false
            }
            val sourceMatches = if (filterState.activeTab != DatabaseTab.CATALOG) {
                true
            } else {
                when (filterState.selectedSource) {
                    CatalogSourceFilter.ALL -> true
                    CatalogSourceFilter.FORMULAS -> !item.ubuCrudeDrug && !item.customOwned
                    CatalogSourceFilter.UBU_HERBS -> item.ubuCrudeDrug
                }
            }
            val categoryMatches = filterState.selectedCategory == null || item.category == filterState.selectedCategory
            val tasteMatches = filterState.selectedTaste == null || item.tasteProfile.contains(filterState.selectedTaste!!, ignoreCase = true)
            val queryMatches = query.isEmpty() ||
                item.name.lowercase().contains(query) ||
                item.englishName.lowercase().contains(query) ||
                item.scientificName.lowercase().contains(query) ||
                item.botanicalFamily.lowercase().contains(query) ||
                item.partUsed.lowercase().contains(query) ||
                item.indications.lowercase().contains(query) ||
                item.ingredientsSummary.lowercase().contains(query) ||
                item.tasteProfile.lowercase().contains(query)
            tabMatches && sourceMatches && categoryMatches && tasteMatches && queryMatches
        }
    }

    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val useTwoPane = maxWidth >= 840.dp

        if (!useTwoPane && selectedRemedy != null && filterState.activeTab == DatabaseTab.REMEDIES) {
            HerbRemedyDetailView(
                remedy = selectedRemedy,
                showBackButton = true,
                isMutating = isRemedyMutating,
                onBack = { selectedRemedyId = null },
                onEdit = if (remedyViewModel != null) {
                    { item -> remedyEditorTarget = true to item }
                } else null,
                onDelete = if (remedyViewModel != null) {
                    { item ->
                        remedyViewModel.deleteRemedy(item.id, item.name) {
                            selectedRemedyId = null
                        }
                    }
                } else null
            )
        } else if (!useTwoPane && selectedEntry != null) {
            FormulaDetailContent(
                entry = selectedEntry,
                isMutating = isMutating,
                showBackButton = true,
                onBack = { selectedFormulaId = null },
                onToggleFavorite = viewModel::toggleFavorite,
                onSaveNote = viewModel::savePersonalNote,
                onCloneToCustom = { entry -> editorTarget = entry to true },
                onEditCustom = { entry -> editorTarget = entry to false },
                onDeleteCustom = { entry ->
                    viewModel.deleteCustomFormula(entry) { selectedFormulaId = null }
                }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (!searchExpanded) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "คลังสมุนไพรไทย",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    if (!userEmail.isNullOrBlank()) {
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showSignOutConfirm = true },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = "ออกจากระบบ"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!useRail && !searchExpanded) {
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            DatabaseTab.entries.forEach { tab ->
                                val selected = filterState.activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!searchExpanded) {
                        val isRemediesTab = filterState.activeTab == DatabaseTab.REMEDIES && remedyViewModel != null
                        ExtendedFloatingActionButton(
                            onClick = {
                                if (isRemediesTab) {
                                    remedyEditorTarget = true to null
                                } else {
                                    editorTarget = null to false
                                }
                            },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text(if (isRemediesTab) "เพิ่มสมุนไพร" else "เพิ่มตำรับยา") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_formula_fab")
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && !searchExpanded) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DatabaseTab.entries.forEach { tab ->
                                val selected = filterState.activeTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                        VerticalDivider()
                    }

                    val listWeight = if (useTwoPane) 0.46f else 1f
                    Column(
                        modifier = Modifier
                            .weight(listWeight)
                            .fillMaxHeight()
                    ) {
                        HerbalSearchBar(
                            query = filterState.searchQuery,
                            expanded = searchExpanded,
                            onQueryChange = { newQuery ->
                                viewModel.updateSearchQuery(newQuery)
                                remedyViewModel?.onSearchQueryChanged(newQuery)
                            },
                            onExpandedChange = { searchExpanded = it },
                            matchingRemedies = searchMatchingRemedies,
                            onSelectRemedy = { remedy ->
                                selectedRemedyId = null
                                selectedFormulaId = remedy.id
                            },
                            modifier = if (searchExpanded) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            }
                        )

                        if (!searchExpanded) {
                            val activeContentState = if (filterState.activeTab == DatabaseTab.REMEDIES) {
                                remediesState
                            } else {
                                catalogState
                            }
                            when (val state = activeContentState) {
                                is UiState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                is UiState.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Text(
                                                text = state.message,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 6.dp,
                                            bottom = 96.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (filterState.activeTab == DatabaseTab.CATALOG) {
                                            item {
                                                ApothecaryHeroBanner(
                                                    totalCount = allEntries.size,
                                                    savedCount = allEntries.count { it.favorite },
                                                    customCount = allEntries.count { it.customOwned },
                                                    onOpenTasteGuide = { showTasteGuide = true }
                                                )
                                            }

                                            item {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "หมวดหมู่ตำรับยาสมุนไพร",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "เลือกหมวดหมู่เพื่อดูตำรับยาตามกลุ่มอาการ",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (filterState.selectedCategory != null) {
                                                            TextButton(
                                                                onClick = { viewModel.selectCategory(null) },
                                                                modifier = Modifier.testTag("clear_category_filter")
                                                            ) {
                                                                Text("ดูทั้งหมด")
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = { categoriesExpanded = !categoriesExpanded },
                                                            modifier = Modifier.testTag("toggle_categories_list")
                                                        ) {
                                                            Icon(
                                                                imageVector = if (categoriesExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                                contentDescription = if (categoriesExpanded) "ย่อรายการหมวดหมู่" else "แสดงรายการหมวดหมู่"
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            item {
                                                AnimatedVisibility(visible = categoriesExpanded) {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.testTag("categories_list_container")
                                                    ) {
                                                        HerbalCategory.entries.forEach { category ->
                                                            val count = categoryCounts[category] ?: 0
                                                            CategoryListItemCard(
                                                                category = category,
                                                                formulaCount = count,
                                                                selected = filterState.selectedCategory == category,
                                                                onClick = {
                                                                    viewModel.selectCategory(
                                                                        if (filterState.selectedCategory == category) null else category
                                                                    )
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            item {
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 4.dp)
                                                ) {
                                                    items(CatalogSourceFilter.entries) { source ->
                                                        val count = when (source) {
                                                            CatalogSourceFilter.ALL -> allEntries.size
                                                            CatalogSourceFilter.FORMULAS -> allEntries.count { !it.ubuCrudeDrug && !it.customOwned }
                                                            CatalogSourceFilter.UBU_HERBS -> allEntries.count { it.ubuCrudeDrug }
                                                        }
                                                        FilterChip(
                                                            selected = filterState.selectedSource == source,
                                                            onClick = { viewModel.selectSource(source) },
                                                            label = { Text("${source.label} ($count)") },
                                                            modifier = Modifier.testTag("source_filter_${source.name.lowercase()}")
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (filterState.selectedCategory != null || filterState.selectedTaste != null ||
                                            (filterState.activeTab == DatabaseTab.CATALOG && filterState.selectedSource != CatalogSourceFilter.ALL)
                                        ) {
                                            item {
                                                LazyRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (filterState.activeTab == DatabaseTab.CATALOG && filterState.selectedSource != CatalogSourceFilter.ALL) {
                                                        item {
                                                            InputChip(
                                                                selected = true,
                                                                onClick = { viewModel.selectSource(CatalogSourceFilter.ALL) },
                                                                label = { Text("แหล่งข้อมูล: ${filterState.selectedSource.label}") },
                                                                trailingIcon = {
                                                                    Icon(Icons.Rounded.Close, contentDescription = "ล้างตัวกรองแหล่งข้อมูล")
                                                                }
                                                            )
                                                        }
                                                    }
                                                    filterState.selectedCategory?.let { cat ->
                                                        item {
                                                            InputChip(
                                                                selected = true,
                                                                onClick = { viewModel.selectCategory(null) },
                                                                label = { Text("หมวด: ${cat.shortLabel}") },
                                                                trailingIcon = {
                                                                    Icon(Icons.Rounded.Close, contentDescription = "ล้างตัวกรองหมวดหมู่")
                                                                }
                                                            )
                                                        }
                                                    }
                                                    filterState.selectedTaste?.let { taste ->
                                                        item {
                                                            InputChip(
                                                                selected = true,
                                                                onClick = { viewModel.selectTaste(null) },
                                                                label = { Text("รสยา: $taste") },
                                                                trailingIcon = {
                                                                    Icon(Icons.Rounded.Close, contentDescription = "ล้างตัวกรองรสยา")
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = when (filterState.activeTab) {
                                                        DatabaseTab.CATALOG -> filterState.selectedCategory?.thaiLabel ?: "ตำรับยาทั้งหมด"
                                                        DatabaseTab.SAVED -> "ตำรับยาที่บันทึกไว้"
                                                        DatabaseTab.MY_FORMULAS -> "ตำรับยาส่วนตัวของฉัน"
                                                        DatabaseTab.REMEDIES -> "คลังสมุนไพร (HerbRemedy)"
                                                    },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (filterState.activeTab == DatabaseTab.REMEDIES) {
                                                        "${filteredRemediesList.size} รายการ"
                                                    } else {
                                                        "${filteredEntries.size} ตำรับ"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        if (filterState.activeTab == DatabaseTab.REMEDIES) {
                                            if (filteredRemediesList.isEmpty()) {
                                                item {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        EmptyHerbalState(
                                                            title = if (filterState.searchQuery.isBlank()) {
                                                                "ยังไม่มีข้อมูล HerbRemedy"
                                                            } else {
                                                                "ไม่พบสมุนไพรที่ตรงกับ \"${filterState.searchQuery}\""
                                                            },
                                                            subtitle = if (filterState.searchQuery.isBlank()) {
                                                                "เพิ่มข้อมูลสมุนไพรเพื่อบันทึกสรรพคุณ ส่วนประกอบ วิธีใช้ และข้อควรระวัง"
                                                            } else {
                                                                "ลองเปลี่ยนคำค้นหาในช่องค้นหาด้านบน"
                                                            }
                                                        )
                                                        if (remedyViewModel != null && filterState.searchQuery.isBlank()) {
                                                            Button(
                                                                onClick = { remedyEditorTarget = true to null },
                                                                modifier = Modifier.testTag("empty_add_remedy_button")
                                                            ) {
                                                                Text("เพิ่มข้อมูล HerbRemedy")
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                items(
                                                    items = filteredRemediesList,
                                                    key = { "remedy_${it.id}" }
                                                ) { remedy ->
                                                    HerbRemedyListCard(
                                                        remedy = remedy,
                                                        selected = useTwoPane && selectedRemedyId == remedy.id,
                                                        onClick = {
                                                            selectedFormulaId = null
                                                            selectedRemedyId = remedy.id
                                                        }
                                                    )
                                                }
                                            }
                                        } else if (filteredEntries.isEmpty()) {
                                            item {
                                                val (title, subtitle) = when (filterState.activeTab) {
                                                    DatabaseTab.SAVED -> "ยังไม่มีรายการที่บันทึกไว้" to "แตะไอคอนที่คั่นหนังสือบนตำรับยาเพื่อเก็บไว้ดูภายหลัง"
                                                    DatabaseTab.MY_FORMULAS -> "ยังไม่มีตำรับยาส่วนตัว" to "แตะปุ่ม \"เพิ่มตำรับยา\" เพื่อบันทึกสูตรยาของคุณลงคลาวด์"
                                                    DatabaseTab.CATALOG, DatabaseTab.REMEDIES -> "ไม่พบตำรับยาที่ตรงกับการค้นหา" to "ลองเปลี่ยนคำค้นหา หมวดหมู่ หรือล้างตัวกรองรสยา"
                                                }
                                                EmptyHerbalState(title = title, subtitle = subtitle)
                                            }
                                        } else {
                                            items(
                                                items = filteredEntries,
                                                key = { it.id }
                                            ) { entry ->
                                                FormulaCatalogCard(
                                                    entry = entry,
                                                    selected = useTwoPane && selectedFormulaId == entry.id,
                                                    onClick = {
                                                        selectedRemedyId = null
                                                        selectedFormulaId = entry.id
                                                    },
                                                    onToggleFavorite = { viewModel.toggleFavorite(entry) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (useTwoPane && !searchExpanded) {
                        VerticalDivider()
                        Box(
                            modifier = Modifier
                                .weight(0.54f)
                                .fillMaxHeight()
                        ) {
                            if (filterState.activeTab == DatabaseTab.REMEDIES && selectedRemedy != null) {
                                HerbRemedyDetailView(
                                    remedy = selectedRemedy,
                                    showBackButton = false,
                                    isMutating = isRemedyMutating,
                                    onBack = { selectedRemedyId = null },
                                    onEdit = if (remedyViewModel != null) {
                                        { item -> remedyEditorTarget = true to item }
                                    } else null,
                                    onDelete = if (remedyViewModel != null) {
                                        { item ->
                                            remedyViewModel.deleteRemedy(item.id, item.name) {
                                                selectedRemedyId = null
                                            }
                                        }
                                    } else null
                                )
                            } else if (selectedEntry != null) {
                                FormulaDetailContent(
                                    entry = selectedEntry,
                                    isMutating = isMutating,
                                    showBackButton = false,
                                    onBack = { selectedFormulaId = null },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onSaveNote = viewModel::savePersonalNote,
                                    onCloneToCustom = { entry -> editorTarget = entry to true },
                                    onEditCustom = { entry -> editorTarget = entry to false },
                                    onDeleteCustom = { entry ->
                                        viewModel.deleteCustomFormula(entry) { selectedFormulaId = null }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyHerbalState(
                                        title = "เลือกตำรับยาเพื่อดูรายละเอียด",
                                        subtitle = "ดูส่วนประกอบ สรรพคุณ วิธีใช้ ข้อควรระวัง และบันทึกส่วนตัว"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editorTarget?.let { (entry, isClone) ->
        FormulaEditorDialog(
            initialEntry = entry,
            isCloneMode = isClone,
            isSaving = isMutating,
            onSave = { existingId, name, englishName, cat, taste, indications, ingredients, prep, precautions, ref, extUse ->
                viewModel.saveCustomFormula(
                    existingId = existingId,
                    name = name,
                    englishName = englishName,
                    category = cat,
                    tasteProfile = taste,
                    indications = indications,
                    ingredientsSummary = ingredients,
                    preparationAndDosage = prep,
                    precautions = precautions,
                    referenceSource = ref,
                    externalUse = extUse,
                    onSaved = { editorTarget = null }
                )
            },
            onDismiss = { editorTarget = null }
        )
    }

    remedyEditorTarget?.let { (isVisible, initialRemedy) ->
        if (isVisible && remedyViewModel != null) {
            HerbRemedyEditorDialog(
                initialRemedy = initialRemedy,
                isSaving = isRemedyMutating,
                onSave = { updated ->
                    if (initialRemedy == null || initialRemedy.id.isBlank()) {
                        remedyViewModel.createRemedy(updated) { createdId ->
                            remedyEditorTarget = null
                            selectedRemedyId = createdId
                        }
                    } else {
                        remedyViewModel.updateRemedy(initialRemedy.id, updated) {
                            remedyEditorTarget = null
                        }
                    }
                },
                onDismiss = { remedyEditorTarget = null }
            )
        }
    }

    if (showTasteGuide) {
        ThaiTasteGuideDialog(
            onSelectTasteFilter = { taste ->
                viewModel.selectTab(DatabaseTab.CATALOG)
                viewModel.selectTaste(taste)
            },
            onDismiss = { showTasteGuide = false }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("ออกจากระบบ") },
            text = { Text("ต้องการออกจากระบบบัญชี Google ใช่หรือไม่?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        selectedFormulaId = null
                        selectedRemedyId = null
                        signOut(credentialManager, scope)
                    }
                ) {
                    Text("ออกจากระบบ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

private fun tabIcon(tab: DatabaseTab, selected: Boolean) = when (tab) {
    DatabaseTab.CATALOG -> if (selected) Icons.Rounded.LocalFlorist else Icons.Outlined.LocalFlorist
    DatabaseTab.SAVED -> if (selected) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder
    DatabaseTab.MY_FORMULAS -> if (selected) Icons.Rounded.CreateNewFolder else Icons.Outlined.CreateNewFolder
    DatabaseTab.REMEDIES -> if (selected) Icons.Rounded.Medication else Icons.Outlined.Medication
}
