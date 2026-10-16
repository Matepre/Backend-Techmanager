package com.techmanager.techmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para iniciar sesión (POST /auth/login).
 * Contiene las credenciales que envía el formulario de login.
 */
public class LoginRequest {

    /** Email del usuario. Obligatorio y con formato válido. */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    private String email;

    /** Contraseña en texto plano. Solo se exige que no esté vacía. */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /** Constructor vacío: necesario para que Jackson pueda armar el objeto desde el JSON. */
    public LoginRequest() {
    }

    /** Constructor de conveniencia para crear la solicitud con ambos datos (útil en pruebas). */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /** Devuelve el email. */
    public String getEmail() {
        return email;
    }

    /** Asigna el email. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Devuelve la contraseña recibida. */
    public String getPassword() {
        return password;
    }

    /** Asigna la contraseña. */
    public void setPassword(String password) {
        this.password = password;
    }
}
