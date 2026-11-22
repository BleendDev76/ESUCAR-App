package cl.duoc.esucar.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.duoc.esucar.R
import java.util.concurrent.atomic.AtomicInteger

class AndroidNotificador(private val context: Context) : NotificadorSistema {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val siguienteId = AtomicInteger(ID_INICIAL)

    init {
        crearCanal()
    }

    override fun notificar(titulo: String, descripcion: String) {
        if (!puedeNotificar()) return

        val notificacion = NotificationCompat.Builder(context, CANAL_GENERAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(descripcion)
            .setStyle(NotificationCompat.BigTextStyle().bigText(descripcion))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(siguienteId.getAndIncrement(), notificacion)
    }

    private fun puedeNotificar(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canal = NotificationChannel(
            CANAL_GENERAL,
            context.getString(R.string.notificaciones_canal_nombre),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notificaciones_canal_descripcion)
        }

        notificationManager.createNotificationChannel(canal)
    }

    private companion object {
        const val CANAL_GENERAL = "esucar_general"
        const val ID_INICIAL = 1
    }
}
