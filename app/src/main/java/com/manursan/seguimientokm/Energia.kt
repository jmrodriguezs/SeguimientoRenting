package com.manursan.seguimientokm

/**
 * Textos y unidades que dependen del tipo de vehículo: de combustión (litros, repostajes)
 * o eléctrico (kWh, recargas). Los datos se guardan igual en ambos casos; solo cambia
 * cómo se presentan.
 */
data class Energia(
    val electrico: Boolean,
    /** Unidad corta: "l" / "kWh". */
    val unidad: String,
    /** Nombre de la cantidad: "Litros" / "kWh". */
    val cantidad: String,
    /** La cantidad dentro de una frase: "litros" / "kWh". */
    val cantidadEnFrase: String,
    /** Precio por unidad: "€/l" / "€/kWh". */
    val precioUnidad: String,
    /** "Precio por litro" / "Precio por kWh". */
    val precioPor: String,
    /** Dentro de una frase: "precio por litro" / "precio por kWh". */
    val precioPorEnFrase: String,
    /** Unidad de consumo: "l/100 km" / "kWh/100 km". */
    val consumo: String,
    /** "Combustible" / "Electricidad". */
    val nombre: String,
    /** "Repostaje" / "Recarga". */
    val carga: String,
    /** "Repostajes" / "Recargas". */
    val cargas: String,
    /** "repostajes" / "recargas". */
    val cargasMin: String,
    /** "un repostaje" / "una recarga". */
    val unaCarga: String,
    /** "el repostaje" / "la recarga". */
    val laCarga: String,
    /** "Nuevo repostaje" / "Nueva recarga". */
    val nueva: String,
    /** "Editar repostaje" / "Editar recarga". */
    val editar: String,
    /** "Sin repostajes" / "Sin recargas". */
    val sinCargas: String,
    /** Dónde va incluido el IVA de la energía. */
    val ivaIncluido: String,
) {
    companion object {
        val COMBUSTIBLE = Energia(
            electrico = false, unidad = "l", cantidad = "Litros", cantidadEnFrase = "litros", precioUnidad = "€/l",
            precioPor = "Precio por litro", precioPorEnFrase = "precio por litro",
            consumo = "l/100 km", nombre = "Combustible", carga = "Repostaje", cargas = "Repostajes", cargasMin = "repostajes",
            unaCarga = "un repostaje", laCarga = "el repostaje", nueva = "Nuevo repostaje", editar = "Editar repostaje",
            sinCargas = "Sin repostajes", ivaIncluido = "Incluido en surtidor",
        )
        val ELECTRICIDAD = Energia(
            electrico = true, unidad = "kWh", cantidad = "kWh", cantidadEnFrase = "kWh", precioUnidad = "€/kWh",
            precioPor = "Precio por kWh", precioPorEnFrase = "precio por kWh",
            consumo = "kWh/100 km", nombre = "Electricidad", carga = "Recarga", cargas = "Recargas", cargasMin = "recargas",
            unaCarga = "una recarga", laCarga = "la recarga", nueva = "Nueva recarga", editar = "Editar recarga",
            sinCargas = "Sin recargas", ivaIncluido = "Incluido en el precio de la recarga",
        )

        fun de(p: ContractParams): Energia = if (FuelPrices.esElectrico(p.combustibleId)) ELECTRICIDAD else COMBUSTIBLE
    }
}
