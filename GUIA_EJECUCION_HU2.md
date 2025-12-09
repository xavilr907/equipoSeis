# 🚀 GUÍA DE EJECUCIÓN - HU2 (Sistema de Login y Registro)

## 📋 PRE-REQUISITOS

Antes de ejecutar la aplicación, asegúrate de tener:

1. **Android Studio** (versión Hedgehog o superior)
2. **JDK 11** configurado
3. **Dispositivo Android** (físico o emulador):
   - Android 7.0 (API 24) o superior
4. **Conexión a Internet** (para Firebase Auth)

---

## 🛠️ CONFIGURACIÓN INICIAL

### 1. Abrir Proyecto
```bash
cd "C:\Users\usuario\OneDrive\Documents\ACADEMICO\SEPTIMO SEMESTRE\DESARROLLO DE APLICATIVOS MOVILES\proyecto 2"
```

Abrir con Android Studio o usar terminal.

### 2. Sincronizar Gradle
```bash
.\gradlew.bat build --refresh-dependencies
```

### 3. Verificar Firebase
- **Archivo:** `app/google-services.json` debe existir
- **Console:** https://console.firebase.google.com
  - Verificar que "Email/Password" esté habilitado en Authentication

---

## 🧪 EJECUTAR TESTS UNITARIOS

### Opción 1: Todos los tests
```bash
.\gradlew.bat test
```

### Opción 2: Solo tests de AuthViewModel
```bash
.\gradlew.bat test --tests "com.univalle.inventarioapp.ui.auth.AuthViewModelTest"
```

### Ver Reporte de Tests
```bash
# Abrir en navegador:
app\build\reports\tests\testDebugUnitTest\index.html
```

**Expected Output:**
```
BUILD SUCCESSFUL in 15s
16 tests completed, 16 passed
```

---

## 📱 COMPILAR Y EJECUTAR APP

### Opción A: Desde Android Studio

1. **Build > Make Project** (Ctrl+F9)
2. **Run > Run 'app'** (Shift+F10)
3. Seleccionar dispositivo/emulador
4. App se instalará y abrirá automáticamente

### Opción B: Desde Terminal

#### 1. Compilar APK Debug
```bash
.\gradlew.bat assembleDebug
```

**Output esperado:**
```
BUILD SUCCESSFUL in 1m 23s
APK ubicado en: app\build\outputs\apk\debug\app-debug.apk
```

#### 2. Instalar en Dispositivo
```bash
# Conectar dispositivo físico o iniciar emulador
adb devices  # Verificar que el dispositivo aparece

# Instalar APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

#### 3. Ejecutar App
```bash
adb shell am start -n com.univalle.inventarioapp/.LoginActivity
```

---

## 🎯 PRUEBAS MANUALES (Escenarios)

### Escenario 1: Login Exitoso

**Pasos:**
1. Abrir app (debe mostrar LoginActivity)
2. Escribir email: `test@example.com`
3. Escribir password: `123456`
4. Click en botón "Login" (naranja)
5. Esperar 1-2 segundos

**Resultado Esperado:**
- ProgressBar aparece brevemente
- Navega a MainActivity (pantalla de inventario)
- No se puede volver con Back

**Si falla:** Verificar que el usuario exista en Firebase Console o crear uno primero con Escenario 3.

---

### Escenario 2: Login Incorrecto

**Pasos:**
1. Escribir email: `wrong@test.com`
2. Escribir password: `999999`
3. Click en botón "Login"

**Resultado Esperado:**
- Toast: "Login incorrecto" (parte inferior de la pantalla)
- Permanece en LoginActivity
- Campos mantienen el texto escrito

---

### Escenario 3: Registro Nuevo Usuario

**Pasos:**
1. Escribir email nuevo: `nuevo123@test.com`
2. Escribir password: `654321`
3. Click en "¿No tienes cuenta? Regístrate" (texto gris)
4. Esperar 1-2 segundos

**Resultado Esperado:**
- Usuario se crea en Firebase Auth
- Navega a MainActivity
- Puedes verificar en Firebase Console > Authentication > Users

---

### Escenario 4: Validación en Tiempo Real

**Pasos:**
1. Click en campo Password
2. Escribir: `1` → ver estado
3. Escribir: `2` → ver estado
4. Escribir: `3` → ver estado
5. Escribir: `4` → ver estado
6. Escribir: `5` → ver estado (error debe aparecer)
7. Escribir: `6` → ver estado (error debe desaparecer)

**Resultado Esperado:**
- Al escribir 5° dígito:
  - Aparece texto rojo "Mínimo 6 dígitos"
  - Borde del campo se vuelve rojo
  - Botón Login permanece gris
- Al escribir 6° dígito:
  - Error desaparece
  - Borde vuelve a blanco
  - Si email válido, botón se vuelve naranja

---

### Escenario 5: Límite de Caracteres

**Email (Max 40):**
1. Intentar escribir: `aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee@test.com` (60 chars)
2. Verificar que solo aparecen 40 caracteres

**Password (Max 10):**
1. Intentar escribir: `12345678901234567890` (20 dígitos)
2. Verificar que solo aparecen 10 dígitos

---

### Escenario 6: Solo Números en Password

**Pasos:**
1. Intentar escribir: `abc123def456`
2. Verificar resultado

**Resultado Esperado:**
- Solo aparece: `123456` (letras ignoradas)

---

## 🐛 TROUBLESHOOTING

### Error: "Unresolved reference: dagger"
**Solución:**
```bash
.\gradlew.bat clean build --refresh-dependencies
```

### Error: "google-services.json not found"
**Solución:**
- Descargar desde Firebase Console
- Colocar en: `app/google-services.json`

### Error: "Firebase Auth not enabled"
**Solución:**
1. Ir a Firebase Console
2. Authentication > Sign-in method
3. Habilitar "Email/Password"
4. Guardar cambios

### Error: Tests fallan con "Null pointer exception"
**Solución:**
- Verificar que Mockito esté configurado correctamente
- Ejecutar: `.\gradlew.bat clean test`

### App crash al abrir
**Solución:**
1. Ver logs: `adb logcat | Select-String "univalle"`
2. Verificar que Firebase esté configurado
3. Verificar permisos de Internet en Manifest

---

## 📊 VALIDACIÓN DE COVERAGE

### Generar Reporte de Cobertura (con Jacoco)
```bash
.\gradlew.bat testDebugUnitTest jacocoTestReport
```

### Ver Reporte HTML
```bash
# Abrir en navegador:
app\build\reports\jacoco\testDebugUnitTest\html\index.html
```

**Buscar:** `com.univalle.inventarioapp.ui.auth.AuthViewModel`  
**Esperado:** Coverage > 30%

---

## 📸 CAPTURAS DE PANTALLA ESPERADAS

### 1. Pantalla Inicial
- Fondo negro
- Logo centrado arriba
- Título "Inventory" naranja
- Campo Email vacío (borde blanco)
- Campo Password vacío (borde blanco)
- Botón Login gris (deshabilitado)
- Texto "Regístrate" gris

### 2. Validación de Error
- Password con texto "12345"
- Error rojo "Mínimo 6 dígitos" visible
- Borde del campo rojo
- Botón Login gris

### 3. Formulario Válido
- Email: "test@example.com"
- Password: "123456" (sin error)
- Botón Login naranja (habilitado)
- Texto "Regístrate" visible (opacidad 1.0)

### 4. Estado de Carga
- ProgressBar circular visible (debajo del texto Registro)
- Botones deshabilitados temporalmente

---

## 🔄 FLUJO COMPLETO DE NAVEGACIÓN

```
[LoginActivity]
      |
      | login() o register()
      | (credenciales válidas)
      ▼
[MainActivity / Home]
      |
      | (inventario visible)
      |
      | Back press
      ▼
[App se cierra]
(no vuelve a LoginActivity)
```

---

## 📝 COMANDOS ÚTILES

### Ver logs en tiempo real
```bash
adb logcat -c  # Limpiar logs
adb logcat | Select-String "AuthViewModel"  # Filtrar ViewModel
```

### Limpiar caché de app
```bash
adb shell pm clear com.univalle.inventarioapp
```

### Desinstalar app
```bash
adb uninstall com.univalle.inventarioapp
```

### Ver estado de Firebase Auth (desde app)
```bash
adb shell dumpsys package com.univalle.inventarioapp | Select-String "userId"
```

---

## ✅ CHECKLIST PRE-ENTREGA

Antes de entregar/demostrar, verificar:

- [ ] Proyecto compila sin errores (`.\gradlew.bat build`)
- [ ] Tests pasan correctamente (`.\gradlew.bat test`)
- [ ] APK se instala en dispositivo
- [ ] Escenario 1 (Login exitoso) funciona
- [ ] Escenario 3 (Registro) funciona
- [ ] Validación en tiempo real funciona (error rojo)
- [ ] Botones cambian de color según estado
- [ ] Toasts aparecen correctamente
- [ ] Navegación a MainActivity funciona
- [ ] Firebase Console muestra usuarios registrados
- [ ] No hay crashes al rotar pantalla
- [ ] Documentación generada (IMPLEMENTACION_HU2_RESUMEN.md)

---

## 📞 SOPORTE

**Errores comunes:** Ver sección TROUBLESHOOTING arriba  
**Logs detallados:** `adb logcat > logs.txt`  
**Firebase Status:** https://status.firebase.google.com

---

**Última actualización:** Diciembre 9, 2025  
**Versión:** 1.0.0  
**Estado:** ✅ PRODUCCIÓN

