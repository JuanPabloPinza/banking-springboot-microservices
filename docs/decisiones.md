# Decisiones técnicas

[Volver al README](../README.md)

## Servicios y eventos

Se separaron clientes y cuentas en dos microservicios con arquitectura hexagonal. Cada uno tiene su esquema y usuario en PostgreSQL. La instancia compartida simplifica la ejecución local.

Accounts mantiene una copia de los datos de clientes mediante eventos de RabbitMQ. Así puede operar sin consultar a clients por HTTP, aunque los cambios tardan en propagarse. El consumidor descarta eventos anteriores al estado recibido y tiene reintentos y una cola de mensajes fallidos.

La publicación ocurre después del commit. Si falla, el evento puede perderse. Un Outbox permitiría guardar el evento en la misma transacción y reintentar su envío.

## Saldos y concurrencia

Cada operación que modifica el saldo bloquea la cuenta dentro de la transacción. Crear o modificar cuentas bloquea primero al cliente, para coordinarse con su baja. Al corregir o eliminar un movimiento, las entidades se cargan después del bloqueo para evitar trabajar con un estado anterior.

Crear una cuenta solo inserta. La restricción única del número impide duplicados y se traduce a HTTP 409. Clientes y cuentas usan borrado lógico. Los movimientos solo pueden corregirse o eliminarse si son los últimos de una cuenta activa, recalculando el saldo.

Las pruebas de integración incluyen retiros simultáneos, creaciones duplicadas y cruces entre bajas, reactivaciones y cambios en movimientos.

## Idempotencia

La cabecera opcional `Idempotency-Key` identifica la solicitud original de un movimiento. Se guarda en una tabla independiente, dentro de la misma transacción que el saldo y el movimiento.

| Reintento con la misma clave | Respuesta |
|---|---|
| Misma cuenta y valor | 200 con el movimiento en su estado actual, sin aplicar otra vez el importe |
| Otra cuenta o valor | 409 `IDEMPOTENCY_KEY_REUTILIZADA` |
| Operación eliminada | 409 `OPERACION_ANULADA` |

La clave solo se inserta y permanece aunque se elimine el movimiento. Si dos cuentas compiten por ella, la restricción de clave primaria revierte toda la operación perdedora.

## Reporte de estado de cuenta

El reporte agrupa cliente, cuentas y movimientos. Incluye cuentas sin actividad que ya estuvieran abiertas al final del rango. Las fechas son inclusivas, con un máximo de 366 días.

| Campo | Significado |
|---|---|
| `saldoInicial` | Saldo de apertura de la cuenta |
| `saldoDisponible` | Saldo actual en la instantánea consultada |
| `saldoInicioPeriodo` | Saldo del último movimiento registrado con fecha anterior al rango, o saldo de apertura si no existe |
| `saldoFinPeriodo` | Saldo del último movimiento del rango en orden de registro, o saldo inicial del período si no hubo movimientos |
| `totalCreditos` / `totalDebitos` | Sumas de depósitos y retiros del rango, con su signo |

Si la cuenta se abrió dentro del rango, el saldo inicial del período es su saldo de apertura. Cada movimiento incluye fecha, tipo, valor y saldo resultante, como en el ejemplo del enunciado.

Se usa `REPEATABLE_READ` para que las consultas compartan una instantánea. Los movimientos se ordenan por registro. El nombre del cliente, tipo y estado de la cuenta corresponden a los datos actuales de esa instantánea.

## Alcance de la entrega

Además de los pendientes de Outbox y autorización descritos en el README, hay tres límites a considerar:

- Hay un consumidor de eventos por instancia. Antes de desplegar varias, habría que coordinar la creación de la réplica del cliente y el orden de los eventos.
- Corregir o eliminar movimientos modifica el historial y los reportes posteriores. Un registro financiero de producción necesitaría auditoría y movimientos inmutables.
- Las fechas usan el reloj de la aplicación en `America/Guayaquil`. Si retrocede y cruza los límites del rango, el filtro puede dejar de corresponder con la secuencia de saldos.

## Entorno local

Para trabajar desde el IDE, ejecuta `docker compose up -d postgres rabbitmq keycloak` y arranca ambos servicios con JDK 21. Usan los mismos datos de PostgreSQL que el entorno de Compose.

Para repetir la colección desde cero, estos comandos reinician el entorno. **El primero elimina los contenedores y el volumen de PostgreSQL del proyecto, con todos sus datos. No se recuperan sin una copia de seguridad.**

```bash
docker compose down -v
docker compose up --build -d
```

El nuevo volumen se inicializa con `BaseDatos.sql`. Para detener los servicios conservando los datos, usa `docker compose stop`.
