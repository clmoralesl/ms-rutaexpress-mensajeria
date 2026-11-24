# ms-rutaexpress-mensajeria

Microservicio administrador de topologia RabbitMQ (colas, exchanges y bindings) para el ecosistema RutaExpress.

## Endpoints de la API

Todos los endpoints estan protegidos por la cabecera `X-Servicio-Interno`.

- `GET /api/mensajeria/colas`: Listar colas existentes y su resumen.
- `POST /api/mensajeria/colas`: Declarar una nueva cola (con argumentos opcionales TTL, DLX, DLK, longitud maxima).
- `DELETE /api/mensajeria/colas/{nombre}?forzar=false`: Eliminar una cola (modo seguro por defecto).
- `GET /api/mensajeria/exchanges`: Listar exchanges administrables y su tipo.
- `POST /api/mensajeria/exchanges`: Declarar un exchange (`direct`, `topic` o `fanout`).
- `DELETE /api/mensajeria/exchanges/{nombre}?forzar=false`: Eliminar un exchange.
- `GET /api/mensajeria/bindings`: Listar enlaces activos entre exchanges y colas.
- `POST /api/mensajeria/bindings`: Crear un enlace con clave de enrutamiento.
- `DELETE /api/mensajeria/bindings?exchange=..&cola=..&clave=..`: Eliminar un enlace existente.

Los recursos con prefijo `q.cmd.`, `cmd.` y `amq.` pertenecen a la topologia del sistema y estan protegidos contra creacion, eliminacion y modificacion.
