package com.techmanager.techmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que autentica cada solicitud a partir del token JWT.
 * OncePerRequestFilter garantiza que se ejecute una sola vez por solicitud.
 * SecurityConfig lo coloca antes del filtro estándar de usuario y contraseña.
 *
 * Importante: este filtro NO rechaza solicitudes. Si el token falta o es inválido,
 * simplemente deja pasar la solicitud sin autenticar; luego las reglas de
 * SecurityConfig deciden si la ruta exigía estar autenticado.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    /** Spring inyecta el servicio JWT y el cargador de usuarios por constructor. */
    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Lógica del filtro, paso a paso:
     * 1) Lee la cabecera Authorization. Si no existe o no empieza con "Bearer ",
     *    continúa la cadena sin autenticar (rutas públicas o solicitud sin sesión).
     * 2) Quita los 7 caracteres de "Bearer " para obtener el token y extrae el email.
     *    Si el token está mal formado, vencido o con firma falsa, la excepción se
     *    captura y la solicitud sigue sin autenticar.
     * 3) Si hay email y todavía no hay un usuario autenticado en el contexto, carga
     *    al usuario desde la base (esto también verifica que siga activo).
     * 4) Si el token es válido para ese usuario, crea la autenticación con sus
     *    autoridades (rol), le agrega datos de la solicitud y la guarda en el
     *    SecurityContextHolder: desde ese momento Spring lo considera autenticado.
     * 5) Continúa con el resto de los filtros.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);
        String email;

        try {
            email = jwtService.extraerEmail(token);
        } catch (Exception exception) {
            filterChain.doFilter(request, response);
            return;
        }

        if (email != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                    customUserDetailsService.loadUserByUsername(email);

            if (jwtService.tokenValido(token, userDetails)) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
