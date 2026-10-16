package com.techmanager.techmanager.repository;

import com.techmanager.techmanager.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de la tabla "categorias".
 * Al extender JpaRepository<Categoria, Long>, Spring Data JPA genera automáticamente
 * las operaciones básicas (findAll, findById, save, deleteById, existsById, count...).
 * Long es el tipo de la clave primaria.
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
