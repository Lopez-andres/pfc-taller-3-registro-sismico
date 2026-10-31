package taller

/**
 * Taller 3 — consultas sobre un registro sísmico con expresiones for, mapas y
 * secuencias perezosas.
 *
 * Complete cada función. Las firmas y los tipos no se modifican.
 */
class RegistroSismico {

  type Registro = List[Sismo]

  // Punto 1: consultas con expresiones for --------------------------------------

  /** Las regiones de los sismos que superan el umbral, en orden y con repetidos. */
  // ejemplo: fuertes(registro, 5.0) == List("Santander", "Choco", "Valle")

  def fuertes(r: Registro, umbral: Double): List[String] = for (s <- r if s.magnitud > umbral) yield s.region

  /** Las magnitudes registradas en una región. */
  // ejemplo: enRegion(registro, "Valle") == List(4.2, 3.8, 5.3)

  def enRegion(r: Registro, region: String): List[Double] = for (s <- r if s.region == region) yield s.magnitud

  /** Parejas de regiones distintas con algún sismo de magnitudes cercanas. */
  // ejemplo:  paresCercanos(registro, 0.5) == List(("Valle", "Santander"))

  def paresCercanos(r: Registro, dif: Double): List[(String, String)] = {

    // se entregan las regiones distintas junto con la cantidad total
    val regiones = (for (s <- r) yield s.region).distinct
    val n = regiones.length

    // nos devolvera un booleano si dos sismos ocurridos en 2 regiones son cercanos
    def cercanas(a: String, b: String) : Boolean =
      r.exists(s1 => s1.region == a &&
      r.exists(s2 => s2.region == b &&
      s1.magnitud - s2.magnitud <= dif && s2.magnitud - s1.magnitud <= dif))

    (for {
      i <- 0 until n
      j <- (i + 1) until n
      if cercanas(regiones(i), regiones(j))
    } yield (regiones(i), regiones(j))).toList
  }

  // Punto 2: agrupar con mapas --------------------------------------------------

  // nos entrega una coleccion con las regiones como claves y la lista de sismos de cara region como valores
  //  ejemplo: porRegion(registro) == Map( "Valle" -> List(<sismos de 4.2, 3.8 y 5.3>))

  def porRegion(r: Registro): Map[String, List[Sismo]] = r.groupBy(s => s.region)

  // nos entrega el nombre de las regiones junto con la cantidad de sismos de cada region
  // ejemplo: conteoPorRegion(registro) == Map("Valle" -> 3, "Santander" -> 2, "Choco" -> 2)

  def conteoPorRegion(r: Registro): Map[String, Int] = porRegion(r).map { case (region, sismos) => (region, sismos.length) }


  // nos entrega las regiones junto con su mayor magnitud en esa misma region
  // ejemplo: magnitudMaxima(registro) == Map("Valle" -> 5.3, "Santander" -> 5.1, "Choco" -> 6.0)
  def magnitudMaxima(r: Registro): Map[String, Double] = porRegion(r).map { case (region, sismos) => (region, sismos.map(s => s.magnitud).max) }

  // Punto 3: consultas seguras --------------------------------------------------

  /** Un mapa que responde cero por las regiones que no están en el registro. */
  def conteoSeguro(r: Registro): Map[String, Int] = ???

  /** La magnitud máxima de una región, o vacío si no hay registros suyos. */
  def maximaDe(r: Registro, region: String): Option[Double] = ???

  /** La región con más sismos; en empate, la primera en orden alfabético. */
  def regionMasActiva(r: Registro): Option[String] = ???

  // Punto 4: réplicas perezosas -------------------------------------------------

  def redondear(x: Double): Double = Math.round(x * 100.0) / 100.0

  /** La secuencia infinita que empieza en la magnitud y multiplica por 0.8. */
  def replicas(magnitud: Double): LazyList[Double] = ???

  /** Las réplicas mientras superen el mínimo, incluida la primera. */
  def replicasSensibles(magnitud: Double, minimo: Double): List[Double] = ???

  // Punto 5: informe combinado --------------------------------------------------

  /** Conteo, magnitud máxima y réplicas esperadas del sismo más fuerte, por región. */
  def informe(r: Registro, minimo: Double): Map[String, Resumen] = ???
}
