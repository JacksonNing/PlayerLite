package com.wxy.playerlite.feature.search

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import com.wxy.playerlite.designsystem.theme.ThemeMode
import com.wxy.playerlite.designsystem.theme.ThemeSelection
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchHostDependenciesTest {
    @Test
    fun requireSearchHostDependencies_shouldPreserveThemeSelectionFlow() {
        val repository = HostDependenciesFakeSearchRepository()
        val themeSelectionFlow = MutableStateFlow(
            ThemeSelection(mode = ThemeMode.DARK)
        )
        val dependencies = SearchHostDependencies(
            repository = repository,
            themeSelectionFlow = themeSelectionFlow
        )
        val application = TestSearchApplication().apply {
            this.dependencies = dependencies
        }

        val resolved = TestSearchContext(application).requireSearchHostDependencies()

        assertSame(repository, resolved.repository)
        assertSame(themeSelectionFlow, resolved.themeSelectionFlow)
    }

    @Test(expected = IllegalStateException::class)
    fun requireSearchHostDependencies_shouldFailWhenProviderMissing() {
        TestSearchContext(Application())
            .requireSearchHostDependencies()
    }
}

private class TestSearchApplication : Application(), SearchHostDependenciesProvider {
    lateinit var dependencies: SearchHostDependencies

    override fun searchHostDependencies(): SearchHostDependencies = dependencies
}

private class TestSearchContext(
    private val appContext: Context
) : ContextWrapper(appContext) {
    override fun getApplicationContext(): Context = appContext
}

private class HostDependenciesFakeSearchRepository : SearchRepository {
    override suspend fun fetchHotKeywords(): List<SearchHotKeywordUiModel> = emptyList()

    override suspend fun fetchSuggestions(keyword: String): List<SearchSuggestionUiModel> = emptyList()

    override suspend fun search(
        keyword: String,
        type: SearchResultType
    ): List<SearchResultUiModel> = emptyList()

    override fun readSearchHistory(): List<String> = emptyList()

    override suspend fun recordSearchHistory(keyword: String) = Unit

    override suspend fun removeSearchHistory(keyword: String) = Unit

    override suspend fun clearSearchHistory() = Unit
}
