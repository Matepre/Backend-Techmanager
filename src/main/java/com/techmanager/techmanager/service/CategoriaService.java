package com.techmanager.techmanager.service;

import com.techmanager.techmanager.entity.Categoria;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de la lógica de negocio de categorías.
 * El controlador depende de esta interfaz y no de la implementación concreta
 * (CategoriaServiceImpl), lo que reduce el acoplamiento entre capas.
 */
public interface CategoriaService {

    /** Devuelve todas las categorías registradas. */
    List<Categoria> listarTodas();

    /** Busca una categoría por id; el Optional viene vacío si no existe. */
    Optional<Categoria> buscarPorId(Long id);

    /** Crea una categoría nueva y devuelve el registro guardado (con su id). */
    Categoria guardar(Categoria categoria);

    /** Modifica el nombre de la categoría indicada y devuelve el resultado. */
    Categoria actualizar(Long id, Categoria categoria);

    /** Elimina la categoría con el id indicado. */
    void eliminar(Long id);
}
