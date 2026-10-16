package com.techmanager.techmanager.service.impl;

import com.techmanager.techmanager.entity.Categoria;
import com.techmanager.techmanager.repository.CategoriaRepository;
import com.techmanager.techmanager.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementación de la lógica de negocio de categorías.
 * {@code @Service} registra la clase como componente de Spring para que pueda
 * inyectarse en los controladores.
 */
@Service
public class CategoriaServiceImpl implements CategoriaService {

    /** Repositorio inyectado por Spring (inyección por campo con @Autowired). */
    @Autowired
    private CategoriaRepository categoriaRepository;

    /** Devuelve todas las categorías (equivale a SELECT * FROM categorias). */
    @Override
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    /** Busca una categoría por su id; devuelve un Optional vacío si no existe. */
    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    /**
     * Guarda una categoría nueva. Si el nombre ya existe, la base rechaza el INSERT
     * por la restricción UNIQUE y Spring lanza DataIntegrityViolationException.
     */
    @Override
    public Categoria guardar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    /**
     * Actualiza el nombre de una categoría existente.
     * Primero la busca (si no existe lanza una excepción) y luego copia el nuevo
     * nombre sobre el registro ya cargado, conservando su id.
     */
    @Override
    public Categoria actualizar(Long id, Categoria categoria) {

        Categoria categoriaExistente = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        categoriaExistente.setNombre(categoria.getNombre());

        return categoriaRepository.save(categoriaExistente);
    }

    /**
     * Elimina la categoría por id. Si algún producto la usa, la clave foránea
     * impide el borrado y Spring lanza DataIntegrityViolationException.
     */
    @Override
    public void eliminar(Long id) {
        categoriaRepository.deleteById(id);
    }
}
