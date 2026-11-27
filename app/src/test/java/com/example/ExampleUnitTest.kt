package com.example

import com.example.data.HerbRemedyDataSource
import com.example.data.ThaiHerbalCatalog
import com.example.model.HerbRemedy
import com.example.model.HerbalCategory
import com.example.model.UiState
import com.example.ui.HerbRemedyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeHerbRemedyDataSource(
    initialRemedies: List<HerbRemedy>
) : HerbRemedyDataSource {
    val remediesFlow = MutableStateFlow(initialRemedies)

    override fun observeRemedies(userId: String): Flow<List<HerbRemedy>> = remediesFlow

    override suspend fun getAllRemedies(): Result<List<HerbRemedy>> =
        Result.success(remediesFlow.value)

    override suspend fun getRemedyById(remedyId: String): Result<HerbRemedy?> =
        Result.success(remediesFlow.value.firstOrNull { it.id == remedyId })

    override suspend fun createRemedy(remedy: HerbRemedy): Result<String> {
        val newId = "remedy_${remediesFlow.value.size + 1}"
        remediesFlow.value = remediesFlow.value + remedy.copy(id = newId)
        return Result.success(newId)
    }

    override suspend fun updateRemedy(remedyId: String, remedy: HerbRemedy): Result<Unit> {
        remediesFlow.value = remediesFlow.value.map {
            if (it.id == remedyId) remedy.copy(id = remedyId) else it
        }
        return Result.success(Unit)
    }

    override suspend fun deleteRemedy(remedyId: String): Result<Unit> {
        remediesFlow.value = remediesFlow.value.filterNot { it.id == remedyId }
        return Result.success(Unit)
    }
}

class ExampleUnitTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun herbRemedyViewModel_filtersInRealTimeAsUserTypes() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val sampleRemedies = listOf(
                    HerbRemedy(
                        id = "1",
                        name = "ฟ้าทะลายโจร",
                        properties = "แก้ไข้ เจ็บคอ ต้านการอักเสบ",
                        components = "ใบฟ้าทะลายโจร",
                        instructions = "รับประทานหลังอาหาร",
                        precautions = "ห้ามใช้ในหญิงตั้งครรภ์"
                    ),
                    HerbRemedy(
                        id = "2",
                        name = "ขิง (Ginger)",
                        properties = "ขับลม แก้คลื่นไส้ ท้องอืด",
                        components = "เหง้าขิงแก่",
                        instructions = "ชงน้ำร้อนดื่ม",
                        precautions = "ระวังในผู้ที่ใช้ยาละลายลิ่มเลือด"
                    ),
                    HerbRemedy(
                        id = "3",
                        name = "ขมิ้นชัน",
                        properties = "สมานแผลในกระเพาะ แก้ท้องอืด",
                        components = "เหง้าขมิ้นชันแห้งบดผง",
                        instructions = "รับประทานก่อนอาหาร",
                        precautions = "ผู้มีท่อน้ำดีอุดตันควรหลีกเลี่ยง"
                    )
                )
                val dataSource = FakeHerbRemedyDataSource(sampleRemedies)
                val viewModel = HerbRemedyViewModel(dataSource, "user_123")

                val collectJob = backgroundScope.launch(dispatcher) {
                    viewModel.remediesState.collect {}
                }

                // Initial unfiltered list
                val initial = viewModel.remediesState.value as UiState.Success
                assertEquals(3, initial.data.size)

                // Type query matching properties across multiple items ("ท้องอืด")
                viewModel.onSearchQueryChanged("ท้องอืด")
                val stomachMatches = viewModel.remediesState.value as UiState.Success
                assertEquals(listOf("ขิง (Ginger)", "ขมิ้นชัน"), stomachMatches.data.map { it.name })

                // Refine query across component/precaution ("เหง้าขิง")
                viewModel.onSearchQueryChanged("เหง้าขิง")
                val gingerMatch = viewModel.remediesState.value as UiState.Success
                assertEquals(listOf("ขิง (Ginger)"), gingerMatch.data.map { it.name })

                // Verify real-time upstream addition while search query is active
                dataSource.createRemedy(
                    HerbRemedy(
                        name = "ขิงดำ",
                        properties = "บำรุงกำลัง",
                        components = "เหง้าขิงดำ",
                        instructions = "ต้มดื่ม",
                        precautions = "ไม่ควรใช้เกินขนาด"
                    )
                )
                val updatedMatches = viewModel.remediesState.value as UiState.Success
                assertEquals(listOf("ขิง (Ginger)", "ขิงดำ"), updatedMatches.data.map { it.name })

                // Clear search restores full list
                viewModel.clearSearch()
                val restored = viewModel.remediesState.value as UiState.Success
                assertEquals(4, restored.data.size)

                collectJob.cancel()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun herbRemedy_hasExpectedFieldsAndDefaults() {
        val empty = HerbRemedy()
        assertEquals("", empty.id)
        assertEquals("", empty.userId)
        assertEquals("", empty.name)
        assertEquals("", empty.properties)
        assertEquals("", empty.components)
        assertEquals("", empty.instructions)
        assertEquals("", empty.precautions)
        assertNull(empty.createdAt)
        assertNull(empty.updatedAt)

        val remedy = HerbRemedy(
            name = "ฟ้าทะลายโจร",
            properties = "แก้ไข้ เจ็บคอ",
            components = "ใบฟ้าทะลายโจรบดผง",
            instructions = "รับประทานครั้งละ 1.5 กรัม วันละ 4 ครั้ง",
            precautions = "ห้ามใช้ในหญิงตั้งครรภ์"
        )
        assertEquals("ฟ้าทะลายโจร", remedy.name)
        assertEquals("แก้ไข้ เจ็บคอ", remedy.properties)
        assertEquals("ใบฟ้าทะลายโจรบดผง", remedy.components)
        assertEquals("รับประทานครั้งละ 1.5 กรัม วันละ 4 ครั้ง", remedy.instructions)
        assertEquals("ห้ามใช้ในหญิงตั้งครรภ์", remedy.precautions)
    }

    @Test
    fun standardCatalog_hasValidUniqueFormulas() {
        val formulas = ThaiHerbalCatalog.standardFormulas
        assertTrue(formulas.size >= 30)
        val ids = formulas.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        formulas.forEach { formula ->
            assertTrue(formula.name.isNotBlank())
            assertTrue(formula.indications.isNotBlank())
            assertTrue(formula.ingredientsSummary.isNotBlank())
            assertTrue(formula.preparationAndDosage.isNotBlank())
            assertTrue(formula.id.matches(Regex("^[a-zA-Z0-9_\\-]+$")))
        }
    }

    @Test
    fun ubuCrudeDrugs_haveBotanicalMetadata() {
        val ubuHerbs = ThaiHerbalCatalog.standardFormulas.filter { it.ubuCrudeDrug }
        assertTrue(ubuHerbs.size >= 18)
        ubuHerbs.forEach { herb ->
            assertTrue(herb.scientificName.isNotBlank())
            assertTrue(herb.botanicalFamily.isNotBlank())
            assertTrue(herb.partUsed.isNotBlank())
            assertTrue(herb.referenceSource.isNotBlank())
        }
    }

    @Test
    fun nineThaiTastes_isComplete() {
        assertEquals(9, ThaiHerbalCatalog.nineThaiTastes.size)
    }

    @Test
    fun herbalCategory_fromCodeFallsBackSafely() {
        assertEquals(HerbalCategory.DIGESTIVE, HerbalCategory.fromCode("DIGESTIVE"))
        assertEquals(HerbalCategory.CUSTOM, HerbalCategory.fromCode("UNKNOWN_CODE"))
    }
}
