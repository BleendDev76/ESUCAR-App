package cl.duoc.esucar.core.notifications

interface NotificadorSistema {

    fun notificar(titulo: String, descripcion: String)

    object Ninguno : NotificadorSistema {
        override fun notificar(titulo: String, descripcion: String) = Unit
    }
}
