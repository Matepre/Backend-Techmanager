package com.techmanager.techmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio que crea y valida tokens JWT usando la librería JJWT.
 * Un JWT es un texto firmado que contiene datos del usuario (claims) y una fecha de
 * vencimiento. Solo quien conoce la clave secreta puede generar una firma válida,
 * por eso el servidor puede confiar en el token sin consultar una sesión guardada.
 */
@Service
public class JwtService {

    /** Clave secreta en Base64, tomada de la propiedad jwt.secret (variable JWT_SECRET). */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Duración del token en milisegundos (jwt.expiration; por defecto 86400000 = 24 horas). */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /** Obtiene el email del token. Se guarda en el campo estándar "subject". */
    public String extraerEmail(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /** Genera un token para el usuario sin datos adicionales (claims extra vacíos). */
    public String generarToken(UserDetails userDetails) {
        return generarToken(new HashMap<>(), userDetails);
    }

    /**
     * Genera un token firmado.
     * Incluye: los claims adicionales recibidos, el email como subject, la fecha de
     * emisión y la de expiración (ahora + jwtExpiration). Se firma con la clave
     * secreta y se devuelve en su formato compacto de texto.
     */
    public String generarToken(
            Map<String, Object> claimsAdicionales,
            UserDetails userDetails
    ) {
        Date fechaActual = new Date();
        Date fechaExpiracion = new Date(
                fechaActual.getTime() + jwtExpiration
        );

        return Jwts.builder()
                .claims(claimsAdicionales)
                .subject(userDetails.getUsername())
                .issuedAt(fechaActual)
                .expiration(fechaExpiracion)
                .signWith(obtenerClave())
                .compact();
    }

    /**
     * Indica si el token es válido para el usuario dado: el email del token debe
     * coincidir con el del usuario y el token no debe estar vencido. La firma se
     * verifica al leer los claims; si fuera falsa, se lanza una excepción.
     */
    public boolean tokenValido(
            String token,
            UserDetails userDetails
    ) {
        String email = extraerEmail(token);

        return email.equals(userDetails.getUsername())
                && !tokenExpirado(token);
    }

    /** Devuelve true si la fecha de expiración del token ya pasó. */
    private boolean tokenExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    /** Obtiene la fecha de expiración almacenada en el token. */
    private Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    /**
     * Método genérico para leer un dato puntual del token.
     * Recibe una función (por ejemplo Claims::getSubject) que indica qué claim extraer
     * y la aplica sobre el contenido ya verificado del token.
     */
    private <T> T extraerClaim(
            String token,
            Function<Claims, T> resolver
    ) {
        Claims claims = extraerTodosLosClaims(token);
        return resolver.apply(claims);
    }

    /**
     * Lee y verifica el token completo: comprueba la firma con la clave secreta y
     * devuelve el contenido (payload). Lanza excepción si la firma no coincide,
     * el token está mal formado o ya venció.
     */
    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Construye la clave criptográfica HMAC a partir del secreto en Base64.
     * Decodifica el texto a bytes y crea la SecretKey; el secreto debe tener
     * al menos 32 bytes (256 bits) para ser aceptado.
     */
    private SecretKey obtenerClave() {
        byte[] bytesClave = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(bytesClave);
    }
}
