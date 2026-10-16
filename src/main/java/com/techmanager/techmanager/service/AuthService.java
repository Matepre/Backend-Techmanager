package com.techmanager.techmanager.service;

import com.techmanager.techmanager.dto.BootstrapRequest;
import com.techmanager.techmanager.dto.LoginRequest;
import com.techmanager.techmanager.dto.LoginResponse;
import com.techmanager.techmanager.entity.Rol;
import com.techmanager.techmanager.entity.Usuario;
import com.techmanager.techmanager.repository.RolRepository;
import com.techmanager.techmanager.repository.UsuarioRepository;
import com.techmanager.techmanager.security.CustomUserDetailsService;
import com.techmanager.techmanager.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de la autenticación: login y creación del primer administrador.
 * Coordina el AuthenticationManager de Spring (validación de credenciales), los
 * repositorios de usuarios y roles, el codificador de contraseñas y el servicio JWT.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    /** Spring inyecta por constructor todas las dependencias que usa el servicio. */
    public AuthService(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService customUserDetailsService,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Indica si falta crear el primer administrador: es verdadero cuando la tabla
     * de usuarios está vacía. Con esta regla el alta inicial solo puede hacerse una vez.
     */
    public boolean requiereAdministradorInicial() {
        return usuarioRepository.count() == 0;
    }

    /**
     * Inicia sesión.
     * 1) Normaliza el email (sin espacios y en minúsculas).
     * 2) Pide al AuthenticationManager que valide email y contraseña; internamente usa
     *    CustomUserDetailsService y compara con el hash bcrypt. Si fallan, lanza
     *    BadCredentialsException, que se traduce a un 401.
     * 3) Si todo es correcto, arma la respuesta con el token y los datos del usuario.
     */
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        return crearRespuestaAutenticada(email);
    }

    /**
     * Crea el primer administrador y lo deja con la sesión iniciada.
     * @Transactional: todos los pasos se ejecutan en una sola transacción; si algo falla,
     * no queda nada guardado a medias.
     * 1) Si ya existe algún usuario, lanza IllegalStateException (la ruta queda cerrada).
     * 2) Busca el rol ADMIN; si no existe, lo crea.
     * 3) Crea el usuario con nombre y email normalizados, la contraseña codificada con
     *    bcrypt (nunca en texto plano), el rol ADMIN y el estado activo.
     * 4) Devuelve la respuesta autenticada con el token.
     */
    @Transactional
    public LoginResponse crearAdministradorInicial(BootstrapRequest request) {
        if (!requiereAdministradorInicial()) {
            throw new IllegalStateException("El administrador inicial ya fue creado");
        }

        Rol rolAdmin = rolRepository.findByNombreIgnoreCase("ADMIN")
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre("ADMIN");
                    return rolRepository.save(rol);
                });

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre().trim());
        usuario.setEmail(request.getEmail().trim().toLowerCase());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(rolAdmin);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        return crearRespuestaAutenticada(usuario.getEmail());
    }

    /**
     * Arma el LoginResponse para un email ya verificado: recupera al usuario, genera el
     * token JWT a partir de sus datos de seguridad y devuelve token, nombre, email y rol.
     */
    private LoginResponse crearRespuestaAutenticada(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        String token = jwtService.generarToken(userDetails);

        return new LoginResponse(
                token,
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol().getNombre()
        );
    }
}
