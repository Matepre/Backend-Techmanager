package com.techmanager.techmanager.dto;

/**
 * DTO de salida de la autenticación (login y alta del primer administrador).
 * Devuelve al frontend el token JWT y los datos básicos de la sesión, sin exponer
 * la entidad Usuario completa (por ejemplo, sin la contraseña).
 */
public class LoginResponse {

    /** Token JWT que el cliente enviará en la cabecera Authorization: Bearer ... */
    private String token;

    /** Nombre del usuario autenticado. */
    private String nombre;

    /** Email del usuario autenticado. */
    private String email;

    /** Nombre del rol del usuario: ADMIN o EMPLEADO. */
    private String rol;

    /** Constructor vacío, requerido para serialización/deserialización con Jackson. */
    public LoginResponse() {
    }

    /** Crea la respuesta con todos los datos de la sesión. */
    public LoginResponse(String token, String nombre, String email, String rol) {
        this.token = token;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }

    /** Devuelve el token JWT. */
    public String getToken() {
        return token;
    }

    /** Asigna el token JWT. */
    public void setToken(String token) {
        this.token = token;
    }

    /** Devuelve el nombre del usuario. */
    public String getNombre() {
        return nombre;
    }

    /** Asigna el nombre del usuario. */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** Devuelve el email del usuario. */
    public String getEmail() {
        return email;
    }

    /** Asigna el email del usuario. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Devuelve el rol del usuario. */
    public String getRol() {
        return rol;
    }

    /** Asigna el rol del usuario. */
    public void setRol(String rol) {
        this.rol = rol;
    }
}
