package dev.privateeye.stealth

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PrivateEyeConnector - Bridges app and core-stealth modules via Shizuku
 * 
 * Implements Shizuku listeners to manage the connection to PrivateEyeUserService
 * running with Shell privileges.
 */
@Singleton
class PrivateEyeConnector @Inject constructor(
    @ApplicationContext private val context: Context
) : Shizuku.OnBinderReceivedListener, Shizuku.OnBinderDeadListener {
    
    companion object {
        private const val TAG = "PrivateEyeConnector"
    }
    
    private var service: IPrivateEyeService? = null
    private var connectionCallback: ConnectionCallback? = null
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                service = IPrivateEyeService.Stub.asInterface(binder)
                android.util.Log.i(TAG, "PrivateEyeUserService connected via Shizuku")
                connectionCallback?.onConnected(service!!)
                
                // Log service details
                try {
                    val pid = service?.getPid()
                    val hasPrivileges = service?.hasShellPrivileges()
                    android.util.Log.i(TAG, "Service PID: $pid, Shell privileges: $hasPrivileges")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error getting service info", e)
                }
            } else {
                android.util.Log.e(TAG, "Binder is null or not alive")
                connectionCallback?.onError("Invalid binder")
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            android.util.Log.w(TAG, "PrivateEyeUserService disconnected")
            service = null
            connectionCallback?.onDisconnected()
        }
    }
    
    /**
     * Initialize Shizuku listeners
     */
    fun initialize() {
        Shizuku.addBinderReceivedListener(this)
        Shizuku.addBinderDeadListener(this)
    }
    
    /**
     * Clean up Shizuku listeners
     */
    fun cleanup() {
        Shizuku.removeBinderReceivedListener(this)
        Shizuku.removeBinderDeadListener(this)
    }
    
    /**
     * Connect to PrivateEyeUserService via Shizuku
     */
    fun connect(callback: ConnectionCallback) {
        this.connectionCallback = callback
        
        try {
            // Check if Shizuku binder is available
            if (!Shizuku.pingBinder()) {
                android.util.Log.e(TAG, "Shizuku binder not available")
                callback.onError("Shizuku Not Running")
                return
            }
            
            android.util.Log.i(TAG, "Shizuku binder received, binding to service...")
            callback.onLog("[System] Shizuku Binder Received")
            
            // Build user service arguments
            val userServiceArgs = Shizuku.UserServiceArgs(
                ComponentName(
                    context.packageName,
                    PrivateEyeUserService::class.java.name
                )
            )
                .daemon(false)
                .processNameSuffix("privateeye_service")
                .debuggable(false)
                .version(1)
            
            // Bind to user service
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
            callback.onLog("[System] Binding to PrivateEyeUserService...")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error connecting to service", e)
            callback.onError("Connection failed: ${e.message}")
        }
    }
    
    /**
     * Disconnect from PrivateEyeUserService
     */
    fun disconnect() {
        try {
            service?.destroy()
            Shizuku.unbindUserService(
                Shizuku.UserServiceArgs(
                    ComponentName(
                        context.packageName,
                        PrivateEyeUserService::class.java.name
                    )
                ),
                serviceConnection,
                true
            )
            service = null
            connectionCallback?.onDisconnected()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error disconnecting from service", e)
        }
    }
    
    /**
     * Get the current service instance
     */
    fun getService(): IPrivateEyeService? = service
    
    /**
     * Check if service is connected
     */
    fun isConnected(): Boolean = service != null
    
    // Shizuku.OnBinderReceivedListener implementation
    override fun onBinderReceived() {
        android.util.Log.i(TAG, "Shizuku binder received callback")
        connectionCallback?.onLog("[System] Shizuku Binder Received")
    }
    
    // Shizuku.OnBinderDeadListener implementation
    override fun onBinderDead() {
        android.util.Log.w(TAG, "Shizuku binder died")
        service = null
        connectionCallback?.onError("Shizuku Binder Dead")
        connectionCallback?.onDisconnected()
    }
    
    /**
     * Callback interface for connection events
     */
    interface ConnectionCallback {
        fun onConnected(service: IPrivateEyeService)
        fun onDisconnected()
        fun onError(message: String)
        fun onLog(message: String)
    }
}
