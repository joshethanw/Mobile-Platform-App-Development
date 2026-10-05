package com.example.ch07.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ch07.data.NoteEntity
import com.example.ch07.data.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteListUiState(
    val isLoading: Boolean = false,
    val notes: List<NoteEntity> = emptyList(),
    val query: String = "",
    val recentlyDeleted: NoteEntity? = null,
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val errorMessage: String? = null
) {
    val isFiltering: Boolean
        get() = query.isNotBlank() || selectedTag != null
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val queryText = MutableStateFlow("")
    private val loadError = MutableStateFlow<String?>(null)
    private val recentlyDeleted = MutableStateFlow<NoteEntity?>(null)
    private val selectedTag = MutableStateFlow<String?>(null)

    private val searchResults: Flow<List<NoteEntity>> = queryText
        .debounce { query -> if (query.isBlank()) 0L else 300L }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllNotes()
            else repository.searchNotes(query)
        }
        .catch { e ->
            loadError.value = e.message ?: "Gagal memuat catatan"
            emit(emptyList())
        }

    private val allNotes: Flow<List<NoteEntity>> = repository.getAllNotes()
        .catch { emit(emptyList()) }

    private val notesAndAll = combine(searchResults, allNotes) { search, all -> search to all }

    val uiState: StateFlow<NoteListUiState> = combine(
        notesAndAll,
        queryText,
        loadError,
        recentlyDeleted,
        selectedTag
    ) { (results, all), query, error, deleted, activeTag ->
        val availableTags = all.flatMap { it.tags }.distinct().sorted()
        val effectiveTag = if (activeTag != null && activeTag in availableTags) activeTag else null
        val filteredNotes = results.filter { effectiveTag == null || it.tags.contains(effectiveTag) }

        NoteListUiState(
            notes = filteredNotes,
            query = query,
            recentlyDeleted = deleted,
            availableTags = availableTags,
            selectedTag = effectiveTag,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteListUiState(isLoading = true)
    )

    fun onQueryChange(newQuery: String) {
        queryText.value = newQuery
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            recentlyDeleted.value = note
        }
    }

    fun undoDelete() {
        val note = recentlyDeleted.value ?: return
        recentlyDeleted.value = null
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun onUndoDismissed() {
        recentlyDeleted.value = null
    }

    fun onTagSelected(tag: String?) {
        if (tag == null || selectedTag.value == tag) {
            selectedTag.value = null
        } else {
            selectedTag.value = tag
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }
}
