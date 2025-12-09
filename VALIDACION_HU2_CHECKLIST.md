# ✅ CHECKLIST DE VALIDACIÓN - HU2 (Sistema de Login y Registro)

## 🎯 VALIDACIÓN FUNCIONAL (Manual Testing)

### 1️⃣ UI - Diseño Visual
- [ ] **Fondo negro:** Pantalla completamente negra (sin toolbar)
- [ ] **Logo:** Visible en parte superior
- [ ] **Título "Inventory":** Color naranja, centrado

### 2️⃣ Campo Email
- [ ] Hint "Email" visible en blanco antes de escribir
- [ ] Hint flota hacia arriba al enfocar
- [ ] Borde blanco al enfocar
- [ ] Máximo 40 caracteres (intentar escribir 50 y verificar que se corta)
- [ ] Texto escrito en color blanco

### 3️⃣ Campo Password
- [ ] Hint "Password" visible en blanco antes de escribir
- [ ] Hint flota hacia arriba al enfocar
- [ ] Solo acepta números (intentar escribir letras y verificar que no aparecen)
- [ ] Máximo 10 dígitos (intentar escribir 15 y verificar que se corta)
- [ ] Icono de ojo visible a la derecha (Material toggle)
- [ ] Click en ojo muestra/oculta contraseña

### 4️⃣ Validación en Tiempo Real (PASSWORD)
**Test Case 1: Menos de 6 dígitos**
- [ ] Escribir "12345" (5 dígitos)
- [ ] Debe aparecer error rojo "Mínimo 6 dígitos" debajo del campo
- [ ] El borde del campo debe ser rojo
- [ ] Botón Login debe estar gris (deshabilitado)
- [ ] Botón Registro debe estar gris claro (opaco)

**Test Case 2: 6 o más dígitos**
- [ ] Escribir "123456" (6 dígitos)
- [ ] Error debe desaparecer
- [ ] Borde debe volver a blanco
- [ ] Si email está lleno, botones deben habilitarse

### 5️⃣ Botón Login
**Estado Deshabilitado:**
- [ ] Color gris cuando campos vacíos
- [ ] Color gris cuando email vacío y password válido
- [ ] Color gris cuando password < 6 dígitos
- [ ] No clickable cuando deshabilitado

**Estado Habilitado:**
- [ ] Color naranja cuando email + password (6-10 dígitos) válidos
- [ ] Texto "Login" blanco en negrita
- [ ] Bordes redondos visibles
- [ ] Clickable

**Acción - Login Correcto:**
- [ ] Ingresar: `test@example.com` / `123456`
- [ ] Si el usuario existe: Navega a MainActivity (pantalla de inventario)
- [ ] No debe volver a LoginActivity al presionar Back

**Acción - Login Incorrecto:**
- [ ] Ingresar credenciales inválidas: `wrong@test.com` / `999999`
- [ ] Debe mostrar Toast: "Login incorrecto"
- [ ] Permanece en LoginActivity
- [ ] Campos no se limpian (mantienen el texto)

### 6️⃣ TextView Registro
**Estado Deshabilitado:**
- [ ] Texto gris claro (#9EA1A1) con opacidad 0.6
- [ ] No clickable cuando formulario inválido

**Estado Habilitado:**
- [ ] Texto gris (#9EA1A1) con opacidad 1.0
- [ ] Clickable cuando formulario válido

**Acción - Registro Exitoso:**
- [ ] Ingresar: `nuevo@example.com` / `654321`
- [ ] Si el email NO existe: Crea usuario y navega a MainActivity
- [ ] Debe aparecer en Firebase Console (Authentication > Users)

**Acción - Registro Fallido:**
- [ ] Intentar registrar email ya existente: `test@example.com` / `123456`
- [ ] Debe mostrar Toast: "Error en el registro"
- [ ] Permanece en LoginActivity

### 7️⃣ ProgressBar
- [ ] Invisible al inicio
- [ ] Aparece durante login (mientras espera respuesta de Firebase)
- [ ] Desaparece después de éxito/error

---

## 🧪 VALIDACIÓN TÉCNICA (Tests Automáticos)

### Ejecutar Tests Unitarios
```bash
./gradlew test --tests "com.univalle.inventarioapp.ui.auth.AuthViewModelTest"
```

**Expected Output:**
```
AuthViewModelTest > test password less than 6 digits shows error PASSED
AuthViewModelTest > test password with 6 digits clears error PASSED
AuthViewModelTest > test password only accepts numbers PASSED
AuthViewModelTest > test password max length is 10 digits PASSED
AuthViewModelTest > test email max length is 40 characters PASSED
AuthViewModelTest > test form is valid with correct email and password PASSED
AuthViewModelTest > test form is invalid with empty email PASSED
AuthViewModelTest > test form is invalid with password less than 6 digits PASSED
AuthViewModelTest > test form is invalid with password more than 10 digits PASSED
AuthViewModelTest > test login success navigates to home PASSED
AuthViewModelTest > test login failure shows error toast PASSED
AuthViewModelTest > test login shows loading state PASSED
AuthViewModelTest > test register success navigates to home PASSED
AuthViewModelTest > test register failure shows error toast PASSED
AuthViewModelTest > test toggle password visibility changes state PASSED

BUILD SUCCESSFUL
16 tests completed, 16 passed
```

### Coverage Report
- [ ] Ejecutar: `./gradlew testDebugUnitTest jacocoTestReport`
- [ ] Abrir: `app/build/reports/jacoco/test/html/index.html`
- [ ] Verificar: AuthViewModel coverage > 30%

---

## 🔍 VALIDACIÓN DE ARQUITECTURA

### Verificar Archivos Creados
- [ ] `data/model/Resource.kt` existe
- [ ] `data/repository/AuthRepository.kt` existe
- [ ] `data/repository/AuthRepositoryImpl.kt` existe
- [ ] `ui/auth/AuthUiState.kt` existe
- [ ] `ui/auth/AuthViewModel.kt` existe
- [ ] `test/.../AuthViewModelTest.kt` existe

### Verificar Inyección de Dependencias
- [ ] `AppModule.kt` tiene `provideAuthRepository()`
- [ ] `AuthViewModel` usa `@HiltViewModel`
- [ ] `AuthRepositoryImpl` usa `@Inject constructor`
- [ ] `LoginActivity` usa `@AndroidEntryPoint`

### Verificar Firebase
- [ ] `google-services.json` existe en `app/`
- [ ] Firebase Auth habilitado en Console (Email/Password)
- [ ] Internet permission en `AndroidManifest.xml`

---

## 🚀 VALIDACIÓN DE BUILD

### Compilación
```bash
./gradlew assembleDebug
```
- [ ] Build exitoso sin errores
- [ ] APK generado en: `app/build/outputs/apk/debug/app-debug.apk`
- [ ] Tamaño del APK < 50MB

### Instalación
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
- [ ] Instalación exitosa en dispositivo/emulador
- [ ] Ícono visible en launcher
- [ ] App abre sin crash

---

## 📊 CRITERIOS DE ACEPTACIÓN (Checklist Final)

| # | Criterio | Implementado | Probado |
|---|----------|--------------|---------|
| 1 | Fondo negro sin Toolbar | ✅ | [ ] |
| 2 | Email: Hint blanco, max 40 chars | ✅ | [ ] |
| 3 | Password: Solo números, 6-10 dígitos | ✅ | [ ] |
| 4 | Validación en tiempo real con error rojo | ✅ | [ ] |
| 5 | Toggle visibilidad password | ✅ | [ ] |
| 6 | Botón Login naranja/gris con estados | ✅ | [ ] |
| 7 | Botón Registro gris con mismas reglas | ✅ | [ ] |
| 8 | Toast "Login incorrecto" en error | ✅ | [ ] |
| 9 | Toast "Error en el registro" en error | ✅ | [ ] |
| 10 | Navegación exitosa a MainActivity | ✅ | [ ] |
| 11 | MVVM Architecture | ✅ | [ ] |
| 12 | Repository Pattern | ✅ | [ ] |
| 13 | Dagger Hilt DI | ✅ | [ ] |
| 14 | Firebase Auth Email/Password | ✅ | [ ] |
| 15 | Tests unitarios >30% coverage | ✅ | [ ] |
| 16 | ViewBinding | ✅ | [ ] |
| 17 | Material Components | ✅ | [ ] |

---

## 🐛 CASOS DE BORDE A PROBAR

### Edge Cases
- [ ] Escribir email > 40 caracteres → debe truncar
- [ ] Escribir password > 10 dígitos → debe truncar
- [ ] Escribir letras en password → no debe aparecer nada
- [ ] Borrar password hasta < 6 dígitos → error inmediato
- [ ] Rotar pantalla → estado debe preservarse
- [ ] Intentar login sin conexión → debe mostrar error
- [ ] Presionar Back después de login exitoso → no debe volver a LoginActivity

### Escenarios Extremos
- [ ] Email vacío + Password válido → botón deshabilitado
- [ ] Email válido + Password vacío → botón deshabilitado
- [ ] Email válido + Password "12345" (5 dígitos) → botón deshabilitado + error rojo
- [ ] Email válido + Password "1234567890" (10 dígitos) → botón habilitado
- [ ] Email válido + Password "12345678901" (11 dígitos) → trunca a 10, botón habilitado

---

## ✅ FIRMA DE VALIDACIÓN

**Fecha de Validación:** _____________  
**Validado por:** _____________  
**Resultado:** [ ] APROBADO  [ ] REQUIERE CORRECCIONES

**Notas:**
_______________________________________
_______________________________________
_______________________________________

