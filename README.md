# Tp-Concurrente

Trabajo final de Programación Concurrente (FCEFyN – UNC): simulación en Java de una
Red de Petri que modela un sistema de procesamiento de datos, controlada por un
monitor con semántica temporal y políticas de resolución de conflictos.

**Integrantes:** Ferrero, Juan Manuel · Trachta, Luciano · Fioramonti, Martino

## Requisitos

- Java 17 o superior (`java -version`).
- No usa dependencias externas ni herramientas de build.

## Compilar

Desde la raíz del repositorio:

```bash
javac -d out $(find src -name "*.java")
```

## Ejecutar la simulación

```bash
java -cp out ar.edu.unc.concurrente.aplicacion.Main [politica] [ruta-log] [objetivo] [perfil]
```

Todos los argumentos son opcionales y posicionales (para indicar el perfil hay que
indicar también los tres anteriores).

| Posición | Argumento   | Valores                                            | Por defecto           |
|----------|-------------|----------------------------------------------------|-----------------------|
| 1        | `politica`  | `prioridad` (prioriza el modo simple, T5) · `aleatorio` | `prioridad`      |
| 2        | `ruta-log`  | archivo donde se escribe el log                    | `logs/simulacion.log` |
| 3        | `objetivo`  | cantidad de invariantes de transición a completar (> 0) | `200`            |
| 4        | `perfil`    | `rapido` · `equilibrado` · `lento` · `penalizado`  | `equilibrado`         |

Ejemplos:

```bash
# Configuración por defecto: prioridad, 200 invariantes, perfil equilibrado
java -cp out ar.edu.unc.concurrente.aplicacion.Main

# Política aleatoria, 200 invariantes, perfil rápido
java -cp out ar.edu.unc.concurrente.aplicacion.Main aleatorio logs/aleatorio-rapido.log 200 rapido
```

Para ver por consola cada intento, espera y disparo, cambiar `DETALLADO` a `true` en
`aplicacion/Main.java`.

### Perfiles temporales (alfa en ms)

| Perfil      | T1 | T3  | T4  | T6  | T8  | T9  | T10 |
|-------------|----|-----|-----|-----|-----|-----|-----|
| rapido      | 40 | 60  | 60  | 70  | 60  | 60  | 60  |
| equilibrado | 60 | 90  | 90  | 100 | 90  | 90  | 90  |
| lento       | 80 | 120 | 120 | 140 | 120 | 120 | 120 |
| penalizado  | 60 | 90  | 90  | 180 | 90  | 90  | 90  |

En todos los perfiles beta es `Long.MAX_VALUE` (sin vencimiento); la justificación está
en la sección "Semántica temporal" del informe.

## Verificar el log con expresiones regulares

```bash
java -cp out ar.edu.unc.concurrente.aplicacion.Regex [ruta-log]
```

Hace tres verificaciones:

1. Reconstruye el camino de cada token, lo clasifica como modo simple, medio o alto
   mediante expresiones regulares y lo contrasta con lo registrado por la simulación.
2. Une todos los disparos en una sola secuencia (`T0 T1 T0 T5 T1 ...`) y aplica una
   expresión regular que encuentra un invariante de transición aunque esté
   intercalado con otros:
   `T0 … T1 … ( T5 … T6 | T2 … T3 … T4 | T7 … T8 … T9 … T10 ) … T11`.
   Cada invariante encontrado se borra (conservando los disparos intermedios) y se
   repite hasta que no haya más coincidencias. En un log válido no sobra nada.
3. Reproduce en orden todos los disparos del log sobre la Red de Petri, desde el
   marcado inicial (M' = M + C·e). Si algún disparo no estaba sensibilizado en ese
   momento (por ejemplo, dos datos usando a la vez el bus P2 o la unidad P6), el log
   describe una ejecución imposible.

Termina con código de salida 1 e indica la línea del problema si el log fue
alterado: un disparo borrado, cambiado, duplicado o movido a un orden imposible.

## Qué muestra la salida

Al terminar, `Main` imprime:

- el marcado final y la cantidad de intentos y disparos por transición;
- el reporte de invariantes de plaza (`ok=true` en cada uno);
- `Invariantes completos: N/N` y cuántos se procesaron por modo `[medio, simple, alto]`;
- el tiempo total de ejecución.

Con la política `prioridad`, el modo simple completa exactamente la mitad de los
invariantes: lo atiende un único hilo y, mientras ese hilo ejecuta T6 (temporal), no
puede volver a tomar T5, por lo que los disparos alternan simple / otro modo.

## Estructura

| Paquete          | Contenido                                                                 |
|------------------|---------------------------------------------------------------------------|
| `aplicacion`     | `Main` (simulación) y `Regex` (análisis del log)                          |
| `configuracion`  | Matriz de incidencia, marcado inicial, hilos y perfiles temporales        |
| `monitor`        | `Monitor`, `Mutex` y `ColasTransiciones` (colas de condición con traspaso del monitor) |
| `redpetri`       | `RedDePetri` y `Marcado`                                                  |
| `tiempo`         | `SensibilizadoConTiempo`: ventanas alfa/beta, timestamps y marca `esperando` |
| `politica`       | `PoliticaAleatoria` y `PoliticaPrioridadSimple`                           |
| `hilo`           | `Hilo`: ejecuta en bucle su secuencia de transiciones                     |
| `registro`       | `RegistradorTransiciones`: escribe el log                                 |
| `analisis`       | Estado de la simulación, invariantes de plaza y analizador regex          |

Los diagramas de clases y de secuencia están en la raíz (`.puml` y `.png`).
