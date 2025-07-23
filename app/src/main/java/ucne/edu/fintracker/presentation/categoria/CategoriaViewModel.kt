package ucne.edu.fintracker.presentation.categoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ucne.edu.fintracker.data.local.repository.CategoriaRepository
import ucne.edu.fintracker.presentation.remote.dto.CategoriaDto
import ucne.edu.fintracker.presentation.remote.Resource
import javax.inject.Inject

@HiltViewModel
class CategoriaViewModel @Inject constructor(
    private val repository: CategoriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriaUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchCategorias()
    }

    // 🔹 Cargar categorías
    fun fetchCategorias() {
        viewModelScope.launch {
            repository.getCategorias().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { categorias ->
                            _uiState.update { it.copy(categorias = categorias, isLoading = false, error = null) }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message, isLoading = false) }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    // Cambiar pestaña
    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    // Filtrar categorías según pestaña
    fun categoriasFiltradas(): List<CategoriaDto> {
        val state = _uiState.value
        return state.categorias.filter { cat ->
            if (state.selectedTabIndex == 0) cat.tipo == "Gasto" else cat.tipo == "Ingreso"
        }
    }

    // Actualizar campos del formulario
    fun onNombreChange(value: String) {
        _uiState.update { it.copy(nombre = value) }
    }

    fun onTipoChange(value: String) {
        _uiState.update { it.copy(tipo = value) }
    }

    fun onIconoChange(value: String) {
        _uiState.update { it.copy(icono = value) }
    }

    fun onColorChange(value: String) {
        _uiState.update { it.copy(colorFondo = value) }
    }

    // 🔹 Guardar categoría y actualizar la lista local inmediatamente
    fun saveCategoria(onSuccess: () -> Unit) {
        val current = _uiState.value
        val nuevaCategoria = CategoriaDto(
            categoriaId = 0,
            nombre = current.nombre,
            tipo = current.tipo,
            icono = current.icono,
            colorFondo = current.colorFondo
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.createCategoria(nuevaCategoria).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        // ✅ Agregar directamente a la lista local
                        result.data?.let { categoriaCreada ->
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    categorias = state.categorias + categoriaCreada,
                                    nombre = "",
                                    tipo = "",
                                    icono = "",
                                    colorFondo = "",
                                    error = null
                                )
                            }
                        } ?: run {
                            _uiState.update { it.copy(isLoading = false) }
                        }

                        // ✅ Si quieres asegurar sincronización, puedes recargar desde backend
                        fetchCategorias()

                        onSuccess()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message, isLoading = false) }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
}
