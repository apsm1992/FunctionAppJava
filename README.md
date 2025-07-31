# Configuración de Variables de Entorno (Azure App Settings)

> Última actualización: 2025-07-31 05:09:43

Esta función requiere las siguientes variables de entorno configuradas en **Azure Function App > Configuration > Application settings**:

| Variable             | Tipo      | Descripción                                                                 |
|----------------------|-----------|------------------------------------------------------------------------------|
| `MONITOR_SCHEDULE`   | string    | Expresión CRON usada por TimerTrigger (por ejemplo: `0 */5 * * * *`)         |
| `ENDPOINTS_US`       | string    | Lista JSON de URLs a monitorear para la región US                            |
| `ENDPOINTS_EU`       | string    | Lista JSON de URLs a monitorear para la región EU                            |
| `GRAIL_ENDPOINT`     | string    | URL real del endpoint de Grail para envío de logs                            |
| `GRAIL_AUTH_TOKEN`   | string    | Token Bearer de autenticación para envío a Grail                             |

--

## Ejemplo de configuración:

```json
ENDPOINTS_US = ["https://us-api1.com/health", "https://us-api2.com/ping"]
ENDPOINTS_EU = ["https://eu-api1.com/health"]
GRAIL_ENDPOINT = https://api.grail.company.com/v1/logs
GRAIL_AUTH_TOKEN = Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
MONITOR_SCHEDULE = 0 */5 * * * *
```

> ⚠️ **Nota:** No compartas tokens reales ni hardcodees en tu código fuente. Usa App Settings o Azure Key Vault.
