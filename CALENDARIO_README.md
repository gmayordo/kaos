# 📅 Calendario KAOS - Guía de Funcionalidades

## Vista General

El módulo de **Calendario** permite gestionar las vacaciones y ausencias de los miembros del equipo, organizado por squads y con navegación mensual.

---

## 🎯 Funcionalidades Principales

### 1. **Filtro por Squad**

- Selecciona un squad del menú desplegable para ver sus eventos
- Muestra solo las vacaciones y ausencias de las personas asignadas a ese squad
- Actualización automática al cambiar de squad

### 2. **Navegación Temporal**

- Navega entre meses usando las flechas ◀️ ▶️
- El calendario muestra eventos del mes seleccionado
- Vista clara del mes y año actual

### 3. **Gestión de Vacaciones** 🏖️

**Ver vacaciones:**

- Badge azul claro con icono de sol ☀️
- Muestra: persona, fechas, días laborables, tipo y estado
- Estados: Pendiente, Aprobada, Rechazada

**Crear vacación:**

- Botón "➕ Nueva Vacación"
- Formulario con:
  - Selección de persona
  - Fecha inicio y fin
  - Tipo: Vacaciones / Permiso / Otro
  - Estado: Pendiente / Aprobada / Rechazada
  - Observaciones (opcional)
- Cálculo automático de días laborables
- Validación de rangos de fechas

**Eliminar vacación:**

- Botón 🗑️ en cada vacación
- Confirmación antes de eliminar

### 4. **Gestión de Ausencias** 🏥

**Ver ausencias:**

- Badge rojo claro con icono de alerta ⚠️
- Muestra: persona, fechas, tipo y motivo
- Tipos: Baja Médica / Baja Maternal/Paternal / Emergencia / Otro

**Crear ausencia:**

- Botón "➕ Nueva Ausencia"
- Formulario con:
  - Selección de persona
  - Fecha inicio
  - Fecha fin (opcional - puede ser indefinida para bajas indeterminadas)
  - Tipo de ausencia
  - Comentario/motivo
- Soporte para ausencias indefinidas (sin fecha fin)

**Eliminar ausencia:**

- Botón 🗑️ en cada ausencia
- Confirmación antes de eliminar

### 5. **Estados de Carga**

- Indicadores de "Cargando..." mientras se obtienen datos
- Mensajes claros cuando no hay eventos: "No hay vacaciones/ausencias para mostrar"

---

## 🚀 Acceso al Calendario

Hay **3 formas** de acceder al calendario:

1. **Desde el Dashboard** (página de inicio `/`):
   - Tarjeta "Calendario" en el dashboard principal
2. **Desde el menú lateral** (sidebar):
   - Icono 📅 "Calendario" en el menú de navegación

3. **URL directa**:
   - Navegar a `/calendario`

---

## 🔙 Navegación al Inicio

Para volver al inicio desde cualquier página:

1. **Logo KAOS** (parte superior del sidebar):
   - Click en el logo o texto "KAOS" → regresa al dashboard `/`
2. **Logo CONTROL** (parte inferior del sidebar):
   - Click en el logo "Powered by CONTROL" → regresa al dashboard `/`

3. **Botón "Inicio"** en el menú lateral

---

## 📊 Datos que se Muestran

### Vacaciones:

- ✅ Persona (nombre completo)
- ✅ Rango de fechas (inicio - fin)
- ✅ Días laborables calculados
- ✅ Tipo (Vacaciones, Permiso, Otro)
- ✅ Estado (Pendiente, Aprobada, Rechazada)
- ✅ Observaciones (si las hay)

### Ausencias:

- ✅ Persona (nombre completo)
- ✅ Fecha inicio
- ✅ Fecha fin (o "Indefinida")
- ✅ Tipo (Baja Médica, Maternal/Paternal, Emergencia, Otro)
- ✅ Comentario/motivo

---

## 🔒 Validaciones

- ✅ Fecha fin debe ser posterior o igual a fecha inicio
- ✅ Todos los campos obligatorios deben completarse
- ✅ La persona seleccionada debe existir en el sistema
- ✅ El squad seleccionado debe tener personas asignadas

---

## 🎨 Interfaz

**Colores distintivos:**

- 🔵 **Vacaciones**: Badge azul claro con icono ☀️
- 🔴 **Ausencias**: Badge rojo claro con icono ⚠️

**Diseño:**

- Vista de cuadrícula responsiva
- Formularios en diálogos modales
- Confirmación de eliminaciones
- Feedback visual en operaciones (loading, success, error)

---

## 💡 Casos de Uso Comunes

### Planificar vacaciones del equipo:

1. Seleccionar el squad
2. Ver vacaciones existentes del mes
3. Añadir nuevas vacaciones para los miembros
4. Revisar solapamientos y disponibilidad

### Registrar una baja médica:

1. Ir a Calendario
2. Seleccionar el squad de la persona
3. "Nueva Ausencia" → Tipo: Baja Médica
4. Si aún no se sabe cuándo volverá: dejar fecha fin vacía (indefinida)

### Ver disponibilidad mensual:

1. Seleccionar squad
2. Navegar al mes deseado
3. Revisar vacaciones y ausencias programadas

---

## 🔄 Actualización de Datos

Los datos se actualizan automáticamente cuando:

- ✅ Cambias de squad
- ✅ Navegas a otro mes
- ✅ Creas una vacación/ausencia
- ✅ Eliminas una vacación/ausencia

No es necesario recargar la página manualmente.

---

## 📱 Responsive

El calendario es totalmente responsive y funciona en:

- 💻 Desktop
- 📱 Tablet
- 📲 Móvil

---

## 🆘 Soporte

Para más información sobre otras funcionalidades de KAOS, consulta:

- 👥 [Gestión de Squads](../squads)
- 👤 [Gestión de Personas](../personas)
- ⚙️ [Configuración](../configuracion)
