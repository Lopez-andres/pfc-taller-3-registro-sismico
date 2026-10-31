package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import Datos.registro

@RunWith(classOf[JUnitRunner])
class RegistroSismicoTest extends AnyFunSuite {

  val s = new RegistroSismico()
  import s._

  val vacio: List[Sismo] = List()

  // Punto 1 -------------------------------------------------------------------

  test("fuertes: los que superan 5.0, en orden") {
    assert(fuertes(registro, 5.0) == List("Santander", "Choco", "Valle"))
  }

  test("fuertes: con un umbral alto no queda ninguno") {
    assert(fuertes(registro, 9.0) == List())
  }

  test("fuertes: conserva los repetidos") {
    assert(fuertes(registro, 3.0).length == 7)
  }

  test("fuertes: el umbral es estricto") {
    // Hay un sismo de magnitud 6.0 exacta; con umbral 6.0 no debe salir.
    assert(!fuertes(registro, 6.0).contains("Choco"))
  }

  test("enRegion: las magnitudes del Valle") {
    assert(enRegion(registro, "Valle") == List(4.2, 3.8, 5.3))
  }

  test("enRegion: una región que no está da la lista vacía") {
    assert(enRegion(registro, "Amazonas") == List())
  }

  test("enRegion: sobre el registro vacío no hay magnitudes") {
    assert(enRegion(vacio, "Valle") == List())
  }

  test("paresCercanos: con 0.6 solo Valle y Santander") {
    assert(paresCercanos(registro, 0.6) == List(("Valle", "Santander")))
  }

  test("paresCercanos: con 1.0 salen las tres parejas, en orden de aparición") {
    assert(paresCercanos(registro, 1.0) ==
      List(("Valle", "Santander"), ("Valle", "Choco"), ("Santander", "Choco")))
  }

  test("paresCercanos: con 0.1 no hay ninguna") {
    assert(paresCercanos(registro, 0.1) == List())
  }

  test("paresCercanos: cada pareja sale una sola vez") {
    val ps = paresCercanos(registro, 1.0)
    assert(ps.map { case (a, b) => Set(a, b) }.distinct.length == ps.length)
  }

  test("paresCercanos: nunca empareja una región consigo misma") {
    assert(paresCercanos(registro, 5.0).forall { case (a, b) => a != b })
  }

  // test del punto 1 agregados
  val cauca: List[Sismo] = List(
    Sismo(Fecha(2026, 1), "Cauca", 3.0, 10),
    Sismo(Fecha(2026, 2), "Huila", 9.0, 10),
    Sismo(Fecha(2026, 3), "Cauca", 5.0, 10),
    Sismo(Fecha(2026, 4), "Cauca", 4.0, 10)
  )

  test("propia fuertes: un umbral negativo deja pasar los siete, en orden") {
    assert(fuertes(registro, -1.0) == List("Valle", "Santander", "Valle", "Choco", "Santander", "Valle", "Choco"))
  }

  test("propia enRegion: respeta el orden de aparición, no el de magnitud") {
    assert(enRegion(cauca, "Cauca") == List(3.0, 5.0, 4.0))
  }

  test("propia paresCercanos: la diferencia exacta de 0.2 cuenta (a lo sumo)") {
    assert(paresCercanos(registro, 0.2) == List(("Valle", "Santander")))
  }

  test("propia paresCercanos: con dif 0.0 emparejan magnitudes iguales de regiones distintas") {
    val iguales = List(
      Sismo(Fecha(2026, 1), "A", 4.0, 10),
      Sismo(Fecha(2026, 1), "B", 4.0, 10),
      Sismo(Fecha(2026, 1), "C", 4.0, 10),
    )

    assert(paresCercanos(iguales, 0.0) == List("A", "B"))
  }

  test("propia paresCercanos: con una sola región, o sin registro, no hay parejas") {
    assert(paresCercanos(cauca.filter(x => x.region == "Cauca"), 5.0) == List())
    assert(paresCercanos(vacio, 5.0) == List())
  }

  // Punto 2 -------------------------------------------------------------------

  test("porRegion: agrupa los siete sismos en tres regiones") {
    assert(porRegion(registro).keySet == Set("Valle", "Santander", "Choco"))
    assert(porRegion(registro).values.map(_.length).sum == 7)
  }

  test("conteoPorRegion: tres, dos y dos") {
    assert(conteoPorRegion(registro) ==
      Map("Valle" -> 3, "Santander" -> 2, "Choco" -> 2))
  }

  test("magnitudMaxima: la mayor de cada región") {
    assert(magnitudMaxima(registro) ==
      Map("Valle" -> 5.3, "Santander" -> 5.1, "Choco" -> 6.0))
  }

  test("conteoPorRegion: el registro vacío da el mapa vacío") {
    assert(conteoPorRegion(vacio) == Map())
  }

  test("magnitudMaxima: las regiones ausentes no aparecen") {
    assert(!magnitudMaxima(registro).contains("Amazonas"))
  }

  test("propia porRegion: el registro vacío da el mapa vacío usando PorRegion") {
    assert(porRegion(vacio) == Map())
  }

  test("propia porRegion: cada grupo conserva sus sismos en orden de aparicion") {
    assert(porRegion(registro)("Choco") ==
      List(Sismo(Fecha(2025, 4), "Choco", 6.0, 40), Sismo(Fecha(2025, 6), "Choco", 3.1, 20)))
  }

  test("propia conteoPorRegion: los conteos suman el tamaño del registro") {
    assert(conteoPorRegion(registro).values.sum == registro.length)
    assert(conteoPorRegion(cauca) == Map("Cauca" -> 3, "Huila" -> 1))
  }

  test("propia magnitudMaxima: el máximo puede estar en medio de la lista") {
    assert(magnitudMaxima(cauca) == Map("Cauca" -> 5.0, "Huila" -> 9.0))
  }

  test("propia punto 2: el resultado no depende del orden del registro") {
    assert(conteoPorRegion(registro.reverse) == conteoPorRegion(registro))
    assert(magnitudMaxima(registro.reverse) == magnitudMaxima(registro))
    assert(porRegion(registro.reverse).map {
      case (k, v) => (k, v.toSet) } ==
      porRegion(registro).map { case (k, v) => (k, v.toSet) })
  }

  // Punto 3 -------------------------------------------------------------------

  test("conteoSeguro: una región desconocida responde cero") {
    assert(conteoSeguro(registro)("Amazonas") == 0)
  }

  test("conteoSeguro: una región conocida responde su conteo") {
    assert(conteoSeguro(registro)("Valle") == 3)
  }

  test("conteoSeguro: sobre el registro vacío todo responde cero") {
    assert(conteoSeguro(vacio)("Valle") == 0)
  }

  test("maximaDe: el Chocó llega a 6.0") {
    assert(maximaDe(registro, "Choco") == Some(6.0))
  }

  test("maximaDe: una región sin registros da vacío") {
    assert(maximaDe(registro, "Amazonas") == None)
  }

  test("regionMasActiva: el Valle, con tres sismos") {
    assert(regionMasActiva(registro) == Some("Valle"))
  }

  test("regionMasActiva: sobre el registro vacío da vacío") {
    assert(regionMasActiva(vacio) == None)
  }

  test("regionMasActiva: en empate gana la primera alfabéticamente") {
    val empate = List(
      Sismo(Fecha(2025, 1), "Zulia", 4.0, 10),
      Sismo(Fecha(2025, 1), "Arauca", 4.0, 10))
    assert(regionMasActiva(empate) == Some("Arauca"))
  }

  // Punto 4 -------------------------------------------------------------------

  test("replicas: las primeras cuatro de un sismo de 5.0") {
    assert(replicas(5.0).take(4).toList == List(5.0, 4.0, 3.2, 2.56))
  }

  test("replicas: la secuencia es infinita y no se cuelga") {
    assert(replicas(5.0).take(200).toList.length == 200)
  }

  test("replicas: empieza en la magnitud dada") {
    assert(replicas(7.3).head == 7.3)
  }

  test("replicas: los valores van redondeados a dos decimales") {
    assert(replicas(5.0).take(6).toList.forall(x => x == Math.round(x * 100.0) / 100.0))
  }

  test("replicasSensibles: las que superan 3.0 partiendo de 5.0") {
    assert(replicasSensibles(5.0, 3.0) == List(5.0, 4.0, 3.2))
  }

  test("replicasSensibles: si la primera no supera el mínimo, no hay ninguna") {
    assert(replicasSensibles(2.0, 3.0) == List())
  }

  test("replicasSensibles: el mínimo es estricto") {
    assert(replicasSensibles(3.0, 3.0) == List())
  }

  // Punto 5 -------------------------------------------------------------------

  test("informe: el resumen de las tres regiones") {
    assert(informe(registro, 3.0) ==
      Map("Valle" -> Resumen(3, 5.3, 3),
          "Santander" -> Resumen(2, 5.1, 3),
          "Choco" -> Resumen(2, 6.0, 4)))
  }

  test("informe: el registro vacío da el mapa vacío") {
    assert(informe(vacio, 3.0) == Map())
  }

  test("informe: el conteo coincide con conteoPorRegion") {
    assert(informe(registro, 3.0).map { case (r, res) => (r, res.sismos) } ==
      conteoPorRegion(registro))
  }

  test("informe: la máxima coincide con magnitudMaxima") {
    assert(informe(registro, 3.0).map { case (r, res) => (r, res.maxima) } ==
      magnitudMaxima(registro))
  }

  test("informe: las réplicas coinciden con replicasSensibles del más fuerte") {
    assert(informe(registro, 3.0)("Choco").replicas ==
      replicasSensibles(6.0, 3.0).length)
  }
}
