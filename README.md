# Reto avanzado: mapa dirigido de sedes

Aplicación de consola para registrar sedes, conectar trayectos con costo entero no negativo y calcular rutas mínimas con Dijkstra. Usa archivos locales; no necesita cuentas, credenciales ni servicios de pago.

## Requisitos y ejecución

- JDK 21, versión ya configurada en el proyecto y compatible con el requisito de Java 17 o superior.
- Apache Maven 3.9.9 como versión de referencia.
- Terminal con entrada UTF-8; en la consola clásica de Windows puede usarse `chcp 65001`.

Desde la raíz del proyecto:

```powershell
mvn clean package
java -jar target/RetoAvanzadoOlimpiadasJava-1.0-SNAPSHOT.jar
```

`package` compila y ejecuta las pruebas. Para ejecutar únicamente las pruebas:

```powershell
mvn test
```

JUnit 5.11.4 y los plugins de compilación, pruebas y empaquetado tienen versiones declaradas en `pom.xml`. La primera compilación necesita acceso a Maven Central para descargar esas dependencias. La aplicación empaquetada funciona sin conexión y no tiene dependencias externas de ejecución.

También puede ejecutarse `org.NoTokensLeft.Main` desde IntelliJ, usando la raíz del proyecto como directorio de trabajo. Se puede elegir otro archivo persistido:

```powershell
java -jar target/RetoAvanzadoOlimpiadasJava-1.0-SNAPSHOT.jar datos/otro-mapa.txt
```

## Uso y persistencia

El menú permite agregar sedes, agregar o actualizar conexiones dirigidas, consultar rutas, mostrar el mapa, importar y exportar. Cada cambio válido se guarda automáticamente en `datos/mapa.txt`, o en el archivo indicado al iniciar. También se persiste la última consulta válida con sus entradas (origen y destino) y su resultado (estado, ruta y costo). Al reiniciar se recupera el mapa y se muestra esa consulta. Cambiar el mapa descarta la consulta anterior para evitar resultados desactualizados. Si no existe el archivo, se inicia un mapa vacío; se crea con el primer cambio válido.

Importar **reemplaza todo el mapa actual** después de validar el archivo completo y guardarlo. Exportar escribe una copia y reemplaza el archivo de destino si existe. El directorio `datos/` se excluye de Git; los ejemplos reproducibles están en `ejemplos/`.

Las escrituras usan un archivo temporal en la misma carpeta y un reemplazo atómico: el archivo anterior se sustituye en una sola operación después de terminar la escritura. La memoria se actualiza después del guardado. Si falla una validación o el guardado, el cambio no se aplica. Un archivo corrupto al iniciar se reporta sin sobrescribirlo ni reiniciar el mapa silenciosamente.

## Formato de importación y exportación

Texto UTF-8 con cabecera obligatoria `MAPA_SEDES;1`. Se declaran las sedes antes de utilizarlas en conexiones:

```text
MAPA_SEDES;1
SEDE;A
SEDE;B
SEDE;C
SEDE;Aislada
CONEXION;A;B;4
CONEXION;A;C;1
CONEXION;C;B;2
```

Después de la cabecera se permiten líneas vacías y comentarios que empiezan por `#`. Los nombres distinguen mayúsculas y minúsculas, admiten espacios internos y caracteres como tildes, pero rechazan nombres vacíos, espacios en los extremos, `;` y caracteres de control. Las sedes duplicadas se rechazan. Los costos deben estar entre 0 y 2147483647. Repetir una conexión origen-destino actualiza su costo; no crea otra arista. Los errores al interpretar un registro indican la línea del archivo.

Al guardar una consulta, el archivo añade dos registros al final del mapa:

```text
CONSULTA;A;B
RESULTADO;ENCONTRADA;3;A;C;B
```

Para una consulta sin trayecto se guarda `RESULTADO;SIN_RUTA;-1`, sin sedes adicionales. Estos registros son opcionales al importar un mapa, pero si aparece una consulta debe estar acompañada por su resultado. El servicio vuelve a calcularla al cargar para comprobar que coincide con lo almacenado, incluida la ruta elegida en caso de empate.

## Diseño y complejidad

- `dominio/MapaSedes`: lista de adyacencia con mapas de Java; valida sedes y costos y expone colecciones de solo lectura.
- `dominio/ResultadoRuta`: resultado inmutable con estado, recorrido y costo `long`. `SIN_RUTA` tiene lista vacía y costo `-1`, que representa ausencia de costo.
- `dominio/ConsultaRuta`: origen, destino y resultado que se conservan para la recarga.
- `servicios/Dijkstra`: usa `PriorityQueue`, distancias `long` y predecesores para reconstruir una ruta válida. Descarta entradas antiguas de la cola. Para una sede existente consultada contra sí misma devuelve esa sede y costo cero.
- `servicios/ServicioMapa`: coordina las consultas, la carga y el guardado automático. Trabaja sobre una copia para que una escritura fallida no cambie el estado en memoria.
- `persistencia/RepositorioMapaArchivo`: lectura y escritura del formato local, reutilizado para importar y exportar.
- `Main`: interacción de consola y mensajes controlados, sin algoritmo ni lectura del formato de archivo.

Con **V** sedes y **E** conexiones, Dijkstra tarda **O((V + E) log V)** para este grafo sin aristas paralelas. La cola puede conservar hasta O(E) entradas por las mejoras pendientes; el espacio adicional es **O(V + E)**. Reconstruir la ruta cuesta O(V). Leer, exportar y copiar el mapa cuesta O(V + E), además del tamaño de los nombres y la escritura del archivo. Al recuperar una consulta se ejecuta Dijkstra otra vez para validarla. Cada modificación y cada consulta guardan el estado completo: esta decisión prioriza la sencillez y la consistencia para mapas pequeños.

## Demostración de los criterios y del reinicio

1. Ejecutar la aplicación y seleccionar **5** para importar `ejemplos/mapa-ejemplo.txt`.
2. Seleccionar **3**, origen `A` y destino `B`: resultado esperado `A -> C -> B`, costo `3`.
3. Consultar de `A` a `Aislada`: resultado esperado `SIN_RUTA`.
4. Seleccionar **2** e intentar conectar `B` con `A`, costo `-1`: debe mostrarse un error. Mostrar el mapa para comprobar que esa conexión no se añadió.
5. Consultar de `A` a `A`: ruta `A`, costo `0`.
6. Volver a consultar de `A` a `B`, exportar con **6** a `datos/copia.txt` y salir con **0**.
7. Volver a iniciar usando el mismo archivo de estado: debe mostrarse la última consulta `A -> B`, ruta `A -> C -> B` y costo `3`. Mostrar el mapa y repetir la consulta para comprobar también la recarga de las sedes y conexiones. Para revisar la exportación puede iniciarse con `datos/copia.txt` como argumento.

Estos son resultados esperados para la demostración; no se ha ejecutado desde el asistente.

## Pruebas y reparto para revisión

Las 41 pruebas JUnit cubren los tres criterios obligatorios, sentido dirigido, origen igual a destino, sedes inexistentes, ciclos de costo cero, suma superior al máximo de `int`, entradas inválidas y recarga. También cubren importación inválida sin perder el estado, fallo de guardado, recuperación de entradas/resultados y flujo de consola. Cada prueba de archivos usa un directorio temporal independiente.

| Responsable asignado | Archivos para revisar |
| --- | --- |
| Kayro | Dominio, sus pruebas y datos de ejemplo |
| Andre | `Dijkstra` y `DijkstraTest` |
| Erickson | Repositorio de archivo y sus pruebas |
| Erving | `Main`, `ServicioMapa`, sus pruebas, Maven, README y `.gitignore` |

Los comentarios `Responsable:` indican asignación para revisión. Cada integrante debe revisar y comprender su módulo y enviar sus propios commits con su identidad Git. No se han creado commits ni publicado cambios desde el asistente.

## Limitaciones conocidas

- Una sola instancia de la aplicación debe escribir cada archivo. No hay edición concurrente; el enunciado no la exige.
- El mapa completo se mantiene en memoria y se vuelve a escribir en cada modificación o consulta. Se conserva únicamente la última consulta, sin historial de resultados.
- El guardado requiere un sistema de archivos que admita reemplazo atómico. Si no lo admite, se reporta un error y no se confirma el cambio.
- No hay garantía de recuperación ante daño físico del disco. Una importación o exportación válida reemplaza el destino; exportar antes si se necesita conservar otra versión.
- Las pruebas están escritas, pero no se han ejecutado ni se ha comprobado la compilación por indicación de trabajar solo sobre el código. Falta que el equipo ejecute `mvn test` y la demostración antes de entregar.
- La entrega pública en GitHub corresponde al líder del equipo y está pendiente.
