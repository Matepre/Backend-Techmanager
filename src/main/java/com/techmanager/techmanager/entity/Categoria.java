package com.techmanager.techmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad que representa una categoría del catálogo (por ejemplo "Cocina" o "IPhone").
 * Se mapea a la tabla "categorias". Hereda de BaseEntity el campo id (clave primaria
 * autoincremental). Los productos la referencian con una relación muchos-a-uno.
 */
@Getter // Lombok: genera getNombre()
@Setter // Lombok: genera setNombre(String)
@Entity
@Table(name = "categorias")
public class Categoria extends BaseEntity {

    /**
     * Nombre de la categoría.
     * - @NotBlank: se valida al recibir la petición (ni nulo ni vacío).
     * - nullable = false: la base también rechaza nulos.
     * - unique = true: la base impide dos categorías con el mismo nombre.
     * - length = 100: largo máximo de la columna.
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}
