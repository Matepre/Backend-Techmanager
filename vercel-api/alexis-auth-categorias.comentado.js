// =============================================================================
// TechManager · API serverless de Vercel · Partes de ALEXIS (comentadas)
// =============================================================================
// Este archivo es una GUÍA DE LECTURA: reúne solo las funciones asignadas a Alexis
// en frontend/api/[...path].js. NO reemplaza al archivo original, que sigue siendo
// uno solo y compartido con Mateo y Valentín.
//
// Funciones de apoyo que usan estas funciones y que viven en el archivo original
// (no son de Alexis):
//   - HttpError(status, message): error con código HTTP para responder al cliente.
//   - database(): devuelve el pool de conexiones a PostgreSQL (Neon) usando DATABASE_URL.
//   - requiredText(valor, etiqueta, maxLargo): valida un texto obligatorio y su largo.
//   - bodyOf(request): obtiene el cuerpo JSON de la solicitud.
//   - bcrypt (bcryptjs) y jwt (jsonwebtoken): librerías importadas al inicio.
// =============================================================================

/**
 * Valida y normaliza un email.
 * Lo exige como texto obligatorio de hasta 100 caracteres, lo pasa a minúsculas y
 * comprueba el formato básico algo@dominio.ext con una expresión regular.
 * Lanza HttpError 400 si el formato no es válido. Devuelve el email limpio.
 */
function normalizedEmail(value) {
  const email = requiredText(value, "El email", 100).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    throw new HttpError(400, "El email no es válido");
  }
  return email;
}

/**
 * Devuelve la clave secreta con la que se firman los tokens (variable JWT_SECRET).
 * Si no está configurada o tiene menos de 32 caracteres, lanza HttpError 503:
 * es una protección para no firmar tokens con una clave débil o inexistente.
 */
function jwtSecret() {
  if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 32) {
    throw new HttpError(503, "Falta configurar un JWT_SECRET seguro en Vercel");
  }
  return process.env.JWT_SECRET;
}

/**
 * Crea la respuesta de sesión que recibe el frontend tras el login o el alta inicial.
 * Firma un token JWT que contiene id, email y rol del usuario, con vencimiento a
 * 24 horas y emisor "techmanager". Devuelve el token junto con nombre, email y rol.
 * (En la versión Spring el token solo lleva el email; aquí también lleva el rol.)
 */
function sessionResponse(user) {
  const token = jwt.sign(
    { id: Number(user.id), email: user.email, rol: user.rol },
    jwtSecret(),
    { expiresIn: "24h", issuer: "techmanager" }
  );
  return { token, nombre: user.nombre, email: user.email, rol: user.rol };
}

/**
 * Identifica al usuario que hace una solicitud protegida.
 * 1) Exige la cabecera Authorization con formato "Bearer <token>"; si no, 401.
 * 2) Verifica firma, vencimiento y emisor del token; si falla, 401 "sesión vencida".
 * 3) Consulta en la base al usuario del token (con su rol) y comprueba que siga
 *    activo. Así, desactivar una cuenta corta el acceso aunque el token no haya vencido.
 * Devuelve el usuario actual {id, nombre, email, rol} para usarlo en los permisos.
 */
async function authenticatedUser(request) {
  const authorization = String(request.headers.authorization || "");
  if (!authorization.startsWith("Bearer ")) throw new HttpError(401, "Sesión no válida");

  let payload;
  try {
    payload = jwt.verify(authorization.slice(7), jwtSecret(), { issuer: "techmanager" });
  } catch {
    throw new HttpError(401, "La sesión venció o no es válida");
  }

  const result = await database().query(
    `SELECT u.id::int, u.nombre, u.email, r.nombre AS rol
       FROM usuarios u
       JOIN roles r ON r.id = u.rol_id
      WHERE u.id = $1 AND u.activo = TRUE`,
    [payload.id]
  );
  if (!result.rowCount) throw new HttpError(401, "El usuario no está activo");
  return result.rows[0];
}

/**
 * Restringe una acción a administradores.
 * Si el rol del usuario no es "ADMIN" lanza HttpError 403. Se usa para las rutas
 * /usuarios y /roles; es la contraparte de hasAuthority("ADMIN") en Spring.
 */
function requireAdmin(user) {
  if (user.rol !== "ADMIN") throw new HttpError(403, "Acceso exclusivo para administradores");
}

/**
 * Maneja las rutas públicas de autenticación (/auth/...).
 * Según la ruta y el método HTTP:
 *
 * GET /auth/bootstrap-status
 *   Cuenta los usuarios y responde {requiereAdministrador: true} si no hay ninguno.
 *
 * POST /auth/bootstrap
 *   Crea el primer administrador. Valida nombre, email y contraseña (mínimo 8
 *   caracteres). Trabaja dentro de una transacción (BEGIN/COMMIT): vuelve a contar
 *   los usuarios y, si ya hay alguno, responde 409; busca el rol ADMIN, genera el
 *   hash bcrypt de la contraseña (costo 12), inserta el usuario y devuelve la
 *   sesión. Si algo falla hace ROLLBACK y siempre libera la conexión (finally).
 *
 * POST /auth/login
 *   Valida email y contraseña, busca al usuario con su rol y compara la contraseña
 *   con el hash usando bcrypt. Si el usuario no existe, está inactivo o la contraseña
 *   no coincide, responde con el mismo mensaje genérico (401), sin revelar cuál
 *   dato falló. Si es correcto, devuelve la sesión con el token.
 *
 * Cualquier otra ruta de /auth responde 404.
 */
async function handleAuth(path, method, request, response) {
  const db = database();
  if (path === "/auth/bootstrap-status" && method === "GET") {
    const result = await db.query("SELECT COUNT(*)::int AS total FROM usuarios");
    return response.status(200).json({ requiereAdministrador: result.rows[0].total === 0 });
  }

  if (path === "/auth/bootstrap" && method === "POST") {
    const body = bodyOf(request);
    const nombre = requiredText(body.nombre, "El nombre", 100);
    const email = normalizedEmail(body.email);
    const password = String(body.password ?? "");
    if (password.length < 8) throw new HttpError(400, "La contraseña debe tener al menos 8 caracteres");

    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const count = await client.query("SELECT COUNT(*)::int AS total FROM usuarios");
      if (count.rows[0].total > 0) throw new HttpError(409, "El administrador inicial ya fue creado");
      const role = await client.query("SELECT id FROM roles WHERE nombre = 'ADMIN'");
      const passwordHash = await bcrypt.hash(password, 12);
      const inserted = await client.query(
        `INSERT INTO usuarios (nombre, email, password, rol_id, activo)
         VALUES ($1, $2, $3, $4, TRUE)
         RETURNING id::int, nombre, email`,
        [nombre, email, passwordHash, role.rows[0].id]
      );
      await client.query("COMMIT");
      return response.status(200).json(sessionResponse({ ...inserted.rows[0], rol: "ADMIN" }));
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  if (path === "/auth/login" && method === "POST") {
    const body = bodyOf(request);
    const email = normalizedEmail(body.email);
    const password = String(body.password ?? "");
    if (!password) throw new HttpError(400, "La contraseña es obligatoria");
    const result = await db.query(
      `SELECT u.id::int, u.nombre, u.email, u.password, u.activo, r.nombre AS rol
         FROM usuarios u
         JOIN roles r ON r.id = u.rol_id
        WHERE LOWER(u.email) = LOWER($1)`,
      [email]
    );
    const user = result.rows[0];
    if (!user || !user.activo || !(await bcrypt.compare(password, user.password))) {
      throw new HttpError(401, "Email o contraseña incorrectos");
    }
    return response.status(200).json(sessionResponse(user));
  }

  throw new HttpError(404, "Endpoint no encontrado");
}

/**
 * CRUD genérico de "entidades simples" con solo id y nombre. Lo comparten
 * categorías (parte de Alexis) y marcas (parte de Mateo); la variable `entity`
 * ("categorias" o "marcas") es el nombre de la tabla. Para Alexis interesa el caso
 * entity === "categorias".
 *
 *   GET    /categorias[/id]  Lista todas ordenadas por nombre o trae una; 404 si el id no existe.
 *   POST   /categorias       Crea una categoría con el nombre validado (obligatorio, máx. 100).
 *   PUT    /categorias/{id}  Cambia el nombre; 404 si el id no existe.
 *   DELETE /categorias/{id}  Elimina; responde 204 sin cuerpo, o 404 si no existe.
 *
 * Todas las consultas usan parámetros ($1, $2) para evitar inyección SQL. Un nombre
 * repetido o una categoría en uso producen errores de PostgreSQL (códigos 23505 y
 * 23503) que el manejador principal traduce a respuestas 409. Un método distinto
 * a los anteriores responde 405.
 */
async function handleSimpleEntity(entity, id, method, request, response) {
  const db = database();
  const singular = entity === "categorias" ? "Categoría" : "Marca";
  if (method === "GET") {
    const result = await db.query(
      `SELECT id::int, nombre FROM ${entity} ${id ? "WHERE id = $1" : ""} ORDER BY nombre`,
      id ? [id] : []
    );
    if (id && !result.rowCount) throw new HttpError(404, `${singular} no encontrada`);
    return response.status(200).json(id ? result.rows[0] : result.rows);
  }

  if (method === "POST" || method === "PUT") {
    const nombre = requiredText(bodyOf(request).nombre, "El nombre", 100);
    const result = method === "POST"
      ? await db.query(`INSERT INTO ${entity} (nombre) VALUES ($1) RETURNING id::int, nombre`, [nombre])
      : await db.query(`UPDATE ${entity} SET nombre = $1 WHERE id = $2 RETURNING id::int, nombre`, [nombre, id]);
    if (!result.rowCount) throw new HttpError(404, `${singular} no encontrada`);
    return response.status(200).json(result.rows[0]);
  }

  if (method === "DELETE") {
    const result = await db.query(`DELETE FROM ${entity} WHERE id = $1`, [id]);
    if (!result.rowCount) throw new HttpError(404, `${singular} no encontrada`);
    return response.status(204).end();
  }
  throw new HttpError(405, "Método no permitido");
}
