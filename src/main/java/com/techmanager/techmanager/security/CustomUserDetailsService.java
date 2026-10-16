package com.techmanager.techmanager.security;

import com.techmanager.techmanager.entity.Usuario;
import com.techmanager.techmanager.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Puente entre la base de datos y Spring Security.
 * Spring Security no conoce la entidad Usuario; necesita un objeto UserDetails.
 * Esta clase busca al usuario por email y lo convierte a ese formato, incluyendo su
 * contraseña (hash) y su rol como autoridad. La usan el proceso de login y el filtro JWT.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /** Spring inyecta el repositorio de usuarios por constructor. */
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carga un usuario por su email (el "username" del sistema).
     * 1) Lo busca ignorando mayúsculas; si no existe lanza UsernameNotFoundException.
     * 2) Si el usuario está inactivo, también lanza una excepción: una cuenta
     *    desactivada no puede autenticarse aunque tenga un token vigente.
     * 3) Toma el nombre del rol (ADMIN o EMPLEADO) y lo registra como autoridad,
     *    que es lo que luego comprueba hasAuthority("ADMIN") en SecurityConfig.
     * 4) Devuelve un UserDetails con email, hash de contraseña y autoridad.
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado con el email: " + email
                        )
                );

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsernameNotFoundException("El usuario está inactivo");
        }

        String nombreRol = usuario.getRol().getNombre();

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(List.of(
                        new SimpleGrantedAuthority(nombreRol)
                ))
                .disabled(!usuario.getActivo())
                .build();
    }
}
