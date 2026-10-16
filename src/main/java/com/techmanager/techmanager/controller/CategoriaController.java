package com.techmanager.techmanager.controller;

import com.techmanager.techmanager.entity.Categoria;
import com.techmanager.techmanager.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de categorías. Expone los endpoints bajo la ruta /categorias.
 * Requiere usuario autenticado (ADMIN o EMPLEADO). No contiene lógica de negocio:
 * recibe la petición HTTP, delega en el servicio y devuelve el resultado como JSON.
 */
@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    /** Spring inyecta el servicio por constructor. */
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /** GET /categorias: devuelve la lista completa de categorías. */
    @GetMapping
    public List<Categoria> listarCategorias() {
        return categoriaService.listarTodas();
    }

    /** GET /categorias/{id}: devuelve una categoría o lanza error si no existe. */
    @GetMapping("/{id}")
    public Categoria buscarCategoria(@PathVariable Long id) {
        return categoriaService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
    }

    /** POST /categorias: crea una categoría. @Valid aplica las validaciones de la entidad. */
    @PostMapping
    public Categoria guardarCategoria(@Valid @RequestBody Categoria categoria) {
        return categoriaService.guardar(categoria);
    }

    /** PUT /categorias/{id}: modifica el nombre de la categoría indicada. */
    @PutMapping("/{id}")
    public Categoria actualizarCategoria(@PathVariable Long id,
                                         @Valid @RequestBody Categoria categoria) {
        return categoriaService.actualizar(id, categoria);
    }

    /** DELETE /categorias/{id}: elimina la categoría (la base la rechaza si está en uso). */
    @DeleteMapping("/{id}")
    public void eliminarCategoria(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}
