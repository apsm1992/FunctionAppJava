# FunctionAppJava - Monitor Web Apps

Este proyecto implementa una Azure Function en Java para monitorear la disponibilidad de endpoints críticos y enviar logs estructurados a una API de observabilidad tipo Grail.

---

## 📐 Arquitectura General

```
[Usuario/Cliente] ──► [Azure Function: MonitorWebAppAvailability]
                          └───► Verifica HTTP status de endpoints
                          └───► Calcula tiempos de respuesta
                          └───► Envía logs a [Grail API]
```

- Tecnología principal: **Azure Functions en Java (Java 17)**
- Estilo de ejecución: **TimerTrigger configurado vía `App Settings`**
- Observabilidad: **Logs estructurados enviados a una API externa (Grail)**
- Código organizado bajo el patrón **modular single-class** con responsabilidad dividida por método

---

## 🚀 Despliegue

### Prerrequisitos

- Tener configurado Azure CLI o Azure Portal
- Acceso a una Azure Subscription con permisos para crear Function Apps
- Java 17 y Maven instalados

### Instrucciones

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/apsm1992/FunctionAppJava.git
   cd FunctionAppJava
   ```

2. **Compilar el proyecto**
   ```bash
   mvn clean package
   ```

3. **Desplegar a Azure**
   ```bash
   func azure functionapp publish <NOMBRE_TU_APP>
   ```

---

## ⚙️ Variables de entorno (App Settings)

Estas variables deben definirse en el portal de Azure (Function App → Configuration):

| Variable             | Descripción                                         | Ejemplo                                     |
|----------------------|-----------------------------------------------------|---------------------------------------------|
| `MONITOR_SCHEDULE`   | Cron para ejecución periódica                       | `0 */5 * * * *` (cada 5 minutos)            |
| `ENDPOINTS_US`       | JSON con URLs a monitorear en US                    | `["https://us.api1.com", "https://us.api2"]`|
| `ENDPOINTS_EU`       | JSON con URLs a monitorear en EU                    | `["https://eu.api1.com"]`                   |
| `GRAIL_ENDPOINT`     | Endpoint real del API de observabilidad             | `https://grail.company.com/api/logs`        |
| `GRAIL_AUTH_TOKEN`   | Token Bearer para autenticación con Grail           | `Bearer eyJ...`                             |

---

## ✅ Pruebas

Se utilizan pruebas unitarias con **JUnit 5 + Mockito**:

- Comando:
  ```bash
  mvn test
  ```

- Travis CI ejecuta automáticamente pruebas en cada Push/Merge:
  - Verifica que la lógica de `checkEndpointWithRetries()` funcione correctamente para respuestas 200, 500, y timeouts.

---

## 📁 Estructura del proyecto

```
FunctionAppJava/
├── src/
│   └── com.function/
│       └── MonitorWebAppAvailability.java
├── test/
│   └── com.function/
│       └── MonitorWebAppAvailabilityTest.java
├── .travis.yml
├── pom.xml
└── README.md
```

---

## 📌 Consideraciones

- No se utiliza Azure Key Vault por simplicidad (ver Card #10).
- Los logs contienen `timestamp`, `url`, `status`, `responseTimeMs`, `source`, `level`.
- En caso de fallos de conexión, se aplica backoff exponencial con reintentos.

---

## 👤 Autor

- **apsm1992**  

---
