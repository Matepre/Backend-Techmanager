package com.techmanager.techmanager.controller;

import com.techmanager.techmanager.dto.BootstrapRequest;
import com.techmanager.techmanager.dto.LoginRequest;
import com.techmanager.techmanager.dto.LoginResponse;
import com.techmanager.techmanager.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST de autenticación. Expone los endpoints públicos bajo /auth
 * (SecurityConfig los declara con permitAll). No tiene lógica propia: valida el
 * cuerpo de la solicitud con @Valid y delega todo en AuthService.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /** Spring inyecta el servicio de autenticación por constructor. */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * GET /auth/bootstrap-status
     * Informa si todavía falta crear el primer administrador.
     * Responde {"requiereAdministrador": true} cuando no hay usuarios registrados.
     * El frontend lo usa para decidir si muestra el formulario de alta inicial o el login.
     */
    @GetMapping("/bootstrap-status")
    public Map<String, Boolean> bootstrapStatus() {
        return Map.of("requiereAdministrador", authService.requiereAdministradorInicial());
    }

    /**
     * POST /auth/bootstrap
     * Crea el primer administrador (nombre, email y contraseña) y devuelve
     * la sesión ya iniciada (token incluido). Solo funciona una vez: si ya existe
     * algún usuario, el servicio lanza un error.
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<LoginResponse> bootstrap(
            @Valid @RequestBody BootstrapRequest request
    ) {
        return ResponseEntity.ok(authService.crearAdministradorInicial(request));
    }

    /**
     * POST /auth/login
     * Valida email y contraseña; si son correctos devuelve el token JWT junto con
     * el nombre, el email y el rol del usuario. Si no, el error se traduce a un 401.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}
