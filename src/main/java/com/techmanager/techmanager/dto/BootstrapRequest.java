package com.techmanager.techmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear el primer administrador (POST /auth/bootstrap).
 * Transporta solo los tres datos necesarios; el rol y el estado los asigna el servicio,
 * por lo que el cliente no puede elegirlos. Las anotaciones se validan con @Valid
 * en el controlador.
 */
public class BootstrapRequest {

    /** Nombre del administrador. No puede estar vacío. */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /** Email del administrador. Obligatorio y con formato válido. Será su usuario de acceso. */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    private String email;

    /** Contraseña en texto plano (se guarda solo su hash bcrypt). Mínimo 8 caracteres. */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    /** Devuelve el nombre. */
    public String getNombre() {
        return nombre;
    }

    /** Asigna el nombre (lo usa Jackson al leer el JSON). */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** Devuelve el email. */
    public String getEmail() {
        return email;
    }

    /** Asigna el email. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Devuelve la contraseña en texto plano recibida. */
    public String getPassword() {
        return password;
    }

    /** Asigna la contraseña. */
    public void setPassword(String password) {
        this.password = password;
    }
}
