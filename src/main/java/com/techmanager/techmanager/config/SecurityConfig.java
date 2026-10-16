package com.techmanager.techmanager.config;

import com.techmanager.techmanager.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración central de seguridad de la aplicación (Spring Security).
 * Define qué rutas son públicas, cuáles exigen el rol ADMIN, cómo se
 * codifican las contraseñas, qué orígenes pueden llamar a la API (CORS)
 * y en qué punto de la cadena de filtros se valida el token JWT.
 */
@Configuration
public class SecurityConfig {

    /** Filtro propio que lee y valida el token JWT de cada solicitud. */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Lista de orígenes permitidos para CORS, separados por coma.
     * Se lee de la propiedad app.cors.allowed-origin-patterns (variable de entorno
     * APP_CORS_ALLOWED_ORIGIN_PATTERNS). Si no está definida, permite localhost
     * y cualquier subdominio de vercel.app.
     */
    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,https://*.vercel.app}")
    private String allowedOriginPatterns;

    /** Spring entrega el filtro JWT por constructor. */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Codificador de contraseñas. BCrypt aplica un hash con sal y costo ajustable,
     * por lo que las contraseñas nunca se guardan en texto plano.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el AuthenticationManager de Spring como bean para que AuthService
     * pueda validar email y contraseña durante el login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Configura CORS: qué sitios web (orígenes) pueden consumir la API desde el navegador.
     * - Orígenes: los de la propiedad allowedOriginPatterns (se limpian espacios y vacíos).
     * - Métodos: GET, POST, PUT, DELETE y OPTIONS (la consulta previa del navegador).
     * - Cabeceras aceptadas: Authorization (token), Content-Type y Accept.
     * - allowCredentials en false: no se usan cookies, la sesión viaja en el token.
     * La configuración se aplica a todas las rutas ("/**").
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(
                Arrays.stream(allowedOriginPatterns.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList()
        );
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Cadena de filtros de seguridad: es la regla principal de acceso de la API.
     * - csrf deshabilitado: la API es stateless y usa token, no cookies de sesión.
     * - cors: usa la configuración definida arriba.
     * - Sesión STATELESS: el servidor no guarda sesiones; cada solicitud trae su JWT.
     * - Reglas de autorización:
     *     /auth/** y /actuator/health  -> públicas (login, primer admin, estado).
     *     /usuarios/** y /roles/**     -> solo usuarios con la autoridad ADMIN.
     *     cualquier otra ruta          -> requiere estar autenticado (ADMIN o EMPLEADO).
     * - El filtro JWT se ejecuta antes del filtro estándar de usuario y contraseña,
     *   de modo que el token autentique al usuario antes de evaluar las reglas.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/actuator/health").permitAll()
                        .requestMatchers("/usuarios/**", "/roles/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
