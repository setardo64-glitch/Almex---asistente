# Almex Asistente

Asistente personal con IA para Android optimizado para dispositivos ZTE A35.

## Características Principales

### Módulo 1: Arquitectura Base
- **Wake Word Engine**: Escucha pasiva local para la palabra "Almex"
- **Gestión de Energía**: Timeout automático de 10 segundos tras respuesta
- **Memoria Inteligente**: Base de datos SQLite para recordar conversaciones
- **Integración Groq**: API para procesamiento de lenguaje natural

### Estética Tech-Enterprise
- Diseño minimalista y profesional
- Colores oscuros/azules profundos
- Tipografía técnica (Roboto Mono)
- Personalización de fondo y color de acento

## Configuración

1. **API Key de Groq**: 
   - Edita `GroqApiClient.kt`
   - Reemplaza `TU_API_KEY_AQUI` con tu clave de API

2. **Permisos requeridos**:
   - Micrófono (RECORD_AUDIO)
   - Almacenamiento (READ_EXTERNAL_STORAGE)
   - Servicio en primer plano (FOREGROUND_SERVICE)

## Arquitectura

```
app/
├── data/
│   ├── database/          # Room Database (SQLite)
│   └── repository/        # Repositorio de conversaciones
├── service/              # Servicio de escucha de voz
├── api/                  # Cliente API de Groq
├── ui/                   # Interfaz de usuario
└── utils/                # Utilidades y preferencias
```

## Optimizaciones para ZTE A35

- Uso eficiente de recursos de CPU y memoria
- Gestión inteligente de energía
- Compresión de imágenes de fondo con overlay
- Base de datos optimizada con límite de conversaciones

## Uso

1. Inicia la aplicación
2. Concede permisos de micrófono y almacenamiento
3. Di "Almex" para activar el asistente
4. Habla tu comando después del wake word
5. El sistema volverá a modo pasivo tras 10 segundos de silencio

## Personalización

- **Fondo**: Toca "FONDO" para seleccionar imagen de galería
- **Color**: Toca "COLOR" para cambiar el color de acento
- Las configuraciones se guardan automáticamente

## Tecnologías

- **Kotlin** - Lenguaje principal
- **Room Database** - Persistencia local
- **OkHttp** - Cliente HTTP para API
- **Glide** - Carga y procesamiento de imágenes
- **Material Design** - Componentes UI
- **Coroutines** - Programación asíncrona