package com.univalle.inventarioapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.univalle.inventarioapp.databinding.ActivityLoginBinding
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // BIOMETRÍA + CREDENCIALES DEL DISPOSITIVO (PIN / patrón / contraseña)
    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 IMPORTANTE PARA HU 2.0:
        // No usamos toolbar aquí (la pantalla debe ser "limpia")

        // Configuramos BiometricPrompt y PromptInfo
        setupBiometric()

        // Botón "Autenticar con bloqueo del dispositivo"
        binding.btnLoginDevice.setOnClickListener {
            startBiometricAuth()
        }

        // Si en tu layout tienes una animación de huella (Lottie) con id lottieFingerprint,
        // también la usamos como disparador de la autenticación:
        binding.lottieFingerprint?.setOnClickListener {
            startBiometricAuth()
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        val biometricManager = BiometricManager.from(this)

        val canAuth = biometricManager.canAuthenticate(authenticators)

        // Si aquí no es SUCCESS, lo manejamos luego en startBiometricAuth()
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            return
        }

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    goToHome()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_biometric_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_biometric_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        // PromptInfo: título y subtítulo del diálogo del sistema
        // (cuando usamos DEVICE_CREDENTIAL NO se puede usar setNegativeButtonText)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.title_login))
            .setSubtitle(getString(R.string.login_subtitle_device))
            .setAllowedAuthenticators(authenticators)
            .build()
    }

    private fun startBiometricAuth() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(authenticators)

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            // No hay PIN / patrón / contraseña / huella configurados → obligamos a ir a ajustes
            showNeedDeviceLockDialog()
        }
    }

    private fun showNeedDeviceLockDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_login))
            .setMessage(getString(R.string.login_need_device_lock))
            .setPositiveButton(getString(R.string.login_go_to_settings)) { _, _ ->
                openSecuritySettings()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun openSecuritySettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ → pantalla de enrolamiento biométrico / credenciales
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        authenticators
                    )
                }
            } else {
                // Versiones anteriores → pantalla de seguridad general
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }

            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.login_biometric_not_available),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun goToHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            // Limpiar backstack de login
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
