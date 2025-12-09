# 🎯 GUÍA VISUAL: Configurar Reglas de Firestore (3 Minutos)

## 📱 PASO A PASO CON CAPTURAS

### PASO 1: Abrir Firebase Console (30 segundos)

```
1. Abre Chrome/Edge/Firefox
2. Ve a: https://console.firebase.google.com
3. Inicia sesión con tu cuenta de Google
4. Verás tus proyectos Firebase
```

**Screenshot de referencia:**
```
┌─────────────────────────────────────────┐
│  Firebase Console                       │
│                                         │
│  🔥 Tus Proyectos                       │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  📱 InventarioApp                 │ │
│  │  Android App                      │ │
│  │  [Seleccionar proyecto]           │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

---

### PASO 2: Navegar a Firestore Database (30 segundos)

```
1. En el menú lateral izquierdo, busca sección "Build" (Compilar)
2. Click en "Firestore Database"
3. Si es primera vez, click "Create database"
   - Modo: Production mode
   - Ubicación: us-central o la más cercana
   - Click "Enable"
```

**Ubicación del menú:**
```
┌───────────────────────┐
│ 🏠 Descripción general│
│                       │
│ 🔨 Build              │ ← Click aquí
│   • Authentication    │
│   • Firestore Database│ ← Luego aquí
│   • Realtime Database │
│   • Storage           │
│                       │
│ 📊 Analytics          │
└───────────────────────┘
```

---

### PASO 3: Acceder a Rules (10 segundos)

```
1. En Firestore Database, verás pestañas en la parte superior
2. Click en la pestaña "Rules"
```

**Pestañas disponibles:**
```
┌──────────────────────────────────────────┐
│  Data  |  Rules  |  Indexes  |  Usage   │
│         ^^^^^^                            │
│         Click aquí                        │
└──────────────────────────────────────────┘
```

---

### PASO 4: Editar Reglas (90 segundos)

#### 4.1 Ver Reglas Actuales (Problema)

**Lo que verás (reglas restrictivas):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;  // ❌ TODO BLOQUEADO
    }
  }
}
```

#### 4.2 Reemplazar con Nuevas Reglas

**Pasos:**
1. **Selecciona TODO el texto** (Ctrl+A / Cmd+A)
2. **Borra** (Delete/Backspace)
3. **Copia y pega** el siguiente código:

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // Regla para la colección de productos
    // Permite lectura y escritura solo a usuarios autenticados
    match /products/{productId} {
      // Permitir lectura (get, list) a usuarios autenticados
      allow read: if request.auth != null;
      
      // Permitir escritura (create, update, delete) a usuarios autenticados
      allow write: if request.auth != null;
    }
    
    // Regla por defecto: denegar todo acceso no especificado
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

**Editor de reglas:**
```
┌────────────────────────────────────────────────┐
│  Firestore Security Rules                     │
│                                                │
│  [Aquí pegas el código nuevo]                 │
│                                                │
│  1  rules_version = '2';                      │
│  2  service cloud.firestore {                 │
│  3    match /databases/{database}/documents { │
│  4      match /products/{productId} {         │
│  5        allow read: if request.auth != null;│
│  ...                                           │
│                                                │
│  [Publish]  [Cancel]                          │
└────────────────────────────────────────────────┘
```

---

### PASO 5: Publicar Reglas (10 segundos)

```
1. Click en el botón "Publish" (color azul)
2. Verás un mensaje: "Rules published successfully"
3. En la parte superior verás: "Published: Today at [hora]"
```

**Confirmación visual:**
```
┌────────────────────────────────────────┐
│  ✅ Rules published successfully      │
│                                        │
│  Published: Dec 9, 2025 at 10:30 AM   │
└────────────────────────────────────────┘
```

---

### PASO 6: Validar Reglas (Opcional - 30 segundos)

```
1. En la pestaña "Rules", click en "Playground" (parte superior derecha)
2. Configurar simulación:
   - Operation: get
   - Location: /products/TEST001
   - Authenticated as: [tu email]
3. Click "Run"
4. Resultado: ✅ "Access allowed"
```

**Playground:**
```
┌────────────────────────────────────────────┐
│  Rules Playground                          │
│                                            │
│  Operation:  [get ▼]                       │
│  Location:   /products/TEST001             │
│  Auth as:    usuario@example.com           │
│                                            │
│  [Run]                                     │
│                                            │
│  Result: ✅ Access allowed                │
└────────────────────────────────────────────┘
```

---

## 🧪 VALIDACIÓN EN LA APP (60 segundos)

### Test 1: Ver Productos

```
1. CIERRA COMPLETAMENTE la app (no solo minimizar)
2. Abre la app
3. Login con tus credenciales
4. Observa la pantalla Home
```

**Resultado Esperado:**
```
┌────────────────────────────┐
│  🏠 Inventario             │
│  ─────────────────────     │
│                            │
│  📦 Producto 1             │
│     Cantidad: 10           │
│     $100.00                │
│                            │
│  📦 Producto 2             │
│     Cantidad: 5            │
│     $50.00                 │
│                            │
│  Total inventario: $150.00 │
│                            │
│              [+] ← FAB     │
└────────────────────────────┘
```

**Si NO hay productos:**
- Lista vacía (sin errores)
- Puedes crear productos

---

### Test 2: Crear Producto

```
1. Click en el FAB naranja (+)
2. Completa el formulario:
   - Código: PROD001
   - Nombre: Laptop
   - Categoría: Electrónicos
   - Cantidad: 5
   - Precio: 800
3. Click "Guardar"
```

**Resultado Esperado:**
```
┌────────────────────────────┐
│  ✅ Producto creado        │
│                            │
│  [Se cierra el diálogo]    │
│                            │
│  📦 Laptop                 │
│     Cantidad: 5            │
│     $800.00                │
│  ← Aparece en la lista     │
└────────────────────────────┘
```

---

### Test 3: Verificar en Firebase Console

```
1. Ve a Firebase Console
2. Firestore Database > Data (pestaña)
3. Debes ver la colección "products"
4. Click en "products"
5. Debes ver el documento "PROD001"
```

**Vista en Console:**
```
┌─────────────────────────────────────────┐
│  Firestore Database > Data              │
│                                         │
│  📁 products                            │
│     └─ 📄 PROD001                       │
│         • code: "PROD001"               │
│         • name: "Laptop"                │
│         • category: "Electrónicos"      │
│         • quantity: 5                   │
│         • price: 800.0                  │
└─────────────────────────────────────────┘
```

---

## 🎯 CREAR PRODUCTO DE PRUEBA (OPCIONAL)

Si quieres agregar un producto manualmente desde Firebase Console:

### Paso 1: Ir a Data

```
Firestore Database > Data (pestaña)
```

### Paso 2: Crear Colección (Si No Existe)

```
1. Click "Start collection"
2. Collection ID: products
3. Click "Next"
```

### Paso 3: Agregar Documento

```
1. Document ID: TEST001 (o auto-generado)
2. Agregar campos:

   Field         Type      Value
   ─────────────────────────────────
   code          string    TEST001
   name          string    Producto Prueba
   category      string    Electrónicos
   quantity      number    10
   price         number    100.0
   imageUrl      string    https://via.placeholder.com/150

3. Click "Save"
```

**Formulario de Firebase:**
```
┌────────────────────────────────────────┐
│  Add a document                        │
│                                        │
│  Document ID: [TEST001]                │
│                                        │
│  Field              Type    Value      │
│  ┌──────────────────────────────────┐ │
│  │ code           string  TEST001   │ │
│  │ name           string  Producto  │ │
│  │ category       string  Electró.. │ │
│  │ quantity       number  10        │ │
│  │ price          number  100.0     │ │
│  └──────────────────────────────────┘ │
│                                        │
│  [Add field]     [Cancel]  [Save]     │
└────────────────────────────────────────┘
```

---

## ⏱️ TIEMPO TOTAL: ~3 MINUTOS

```
┌─────────────────────────────────┐
│  PASO 1: Abrir Console  (30s)  │
│  PASO 2: Ir a Firestore (30s)  │
│  PASO 3: Abrir Rules    (10s)  │
│  PASO 4: Editar Reglas  (90s)  │
│  PASO 5: Publicar       (10s)  │
│  PASO 6: Validar        (30s)  │
│  ─────────────────────────────  │
│  TOTAL:                3m 20s   │
└─────────────────────────────────┘
```

---

## 🆘 AYUDA RÁPIDA

### No encuentro "Firestore Database"

**Solución:**
- Busca en la sección **"Build"** (no "Engage")
- Si no aparece, tu proyecto no tiene Firestore habilitado
- Click en "Get started" o "Create database"

### No puedo publicar las reglas

**Solución:**
- Verifica que tu cuenta sea **Owner** o **Editor** del proyecto
- Ve a: ⚙️ Project Settings > Users and permissions
- Si eres "Viewer", solicita permisos de Editor

### Reglas publicadas pero sigue sin funcionar

**Solución:**
1. Espera 60 segundos (propagación)
2. Cierra COMPLETAMENTE la app (Force Stop)
3. Abre Android Studio > Logcat
4. Filtra por "firestore"
5. Busca mensajes de error específicos
6. Copia el error y búscalo en Google

---

## 📞 SOPORTE ADICIONAL

### Documentación Oficial
- **Firebase Security Rules:** https://firebase.google.com/docs/firestore/security/get-started
- **Testing Rules:** https://firebase.google.com/docs/firestore/security/test-rules-emulator

### Comandos Firebase CLI (Avanzado)

```bash
# Instalar Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Ver reglas actuales
firebase firestore:rules get

# Desplegar reglas desde archivo local
firebase deploy --only firestore:rules
```

---

## ✅ CONFIRMACIÓN FINAL

Una vez completados los pasos, deberías tener:

```
╔═══════════════════════════════════════════════╗
║                                               ║
║  ✅ Reglas de Firestore Publicadas           ║
║  ✅ App puede leer productos                 ║
║  ✅ App puede crear productos                ║
║  ✅ App puede editar productos               ║
║  ✅ App puede eliminar productos             ║
║  ✅ Sincronización en tiempo real            ║
║                                               ║
║  🎉 PROBLEMA RESUELTO                        ║
║                                               ║
╚═══════════════════════════════════════════════╝
```

---

**Creado por:** GitHub Copilot  
**Fecha:** Diciembre 9, 2025  
**Tiempo estimado:** 3 minutos  
**Dificultad:** ⭐⭐☆☆☆ (Fácil)

