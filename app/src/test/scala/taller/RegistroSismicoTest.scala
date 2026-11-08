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
    assert(paresCercanos(iguales, 0.0) == List(("A", "B"), ("A","C"), ("B", "C")))
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

  // Test agregados punto 3 ---------------------------------

   val pruebaP3 = List(
     Sismo(Fecha(2026, 1), "Cauca", 3.0, 10),
     Sismo(Fecha(2026, 1), "Huila", 9.0, 20),
     Sismo(Fecha(2026, 1), "Cauca", 5.0, 15)
   )

  // Test 1: Encuentra la magnitud mayor de Cauca
  test("maximaDe: Encuentra la magnitud mas alta"){
    assert(maximaDe(pruebaP3, "Cauca") == Some(5.0))
  }

  // Test 2: Sins sismos no existe magnitud maxima
  test("maximaDe: Con registro vacío devuelve None"){
    assert(maximaDe(List(), "Cauca") == None)
  }

  // Test 3: Gana la región con más sismos, no con mayor magnitud
  test("regionMasActiva: Selecciona la región con mas sismos"){
    assert(regionMasActiva(pruebaP3) == Some("Cauca"))
  }

  // Test 4: Comprueba que Cauca tiene 2 sismos
  test("conteoSeguro: Cuenta dos sismos en Cauca"){
    assert(conteoSeguro(pruebaP3)("Cauca") == 2)
  }

  // Test 5: Una región con un solo sismo
  test("maximaDe: Una región con un solo sismo"){
    assert(maximaDe(pruebaP3, "Huila") == Some(9.0))
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

  val nuevo: List[Sismo] = List(
    Sismo(Fecha(2026, 1), "Cauca", 4.4, 12),
    Sismo(Fecha(2026, 2), "Huila", 5.8, 70),
    Sismo(Fecha(2026, 2), "Cauca", 6.2, 33),
    Sismo(Fecha(2026, 3), "Huila", 3.9, 15),
    Sismo(Fecha(2026, 3), "Cauca", 3.3, 20),
  )

  test("replicas: primeros cuatro valores de una magnitud 7.0") {
    assert(replicas(7.0).take(4).toList == List(7.0, 5.6, 4.48, 3.58))
  }

  test("replicas: primeros seis valores de una magnitud 5.5") {
    assert(replicas(5.5).take(6).toList == List(5.5, 4.4, 3.52, 2.82, 2.25, 1.8))
  }

  test("replicas: el primer elemento es la magnitud redondeada a dos decimales"){
    assert(replicas(3.456).head == 3.46)
  }

  test("replicas: la secuencia es infinita y termina estancada en 0.0"){
    assert(replicas(1.0).drop(30).take(3).toList == List(0.0, 0.0, 0.0))
  }

  test("replicas: ningun valor es mayor que el anterior"){
    val valores = replicas(8.0).take(10).toList
    assert(valores.zip(valores.tail).forall {case (a, b) => b <= a })
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

  test("informe: dos regiones con mínimo 4.0"){
    assert(informe(nuevo, 4.0) ==
    Map("Cauca" -> Resumen(3, 6.2, 2), "Huila" -> Resumen (2, 5.8, 2)))
  }

  test("replicasSensibles: todas superan el minimo y van en orden decreciente"){
    val r =replicasSensibles(9.0 , 2.0)
    assert(r.forall(_> 2.0) && r == r.sorted.reverse)
  }

  test("replicasSensibles: es el comienzo de replicas") {
    val r = replicasSensibles(6.0, 2.0)
    assert(r == replicas(6.0).take(r.length).toList)
  }
  test("replicasSensibles: un minimo mas alto nunca da mas replicas"){
    assert(replicasSensibles(8.0, 5.0).length <= replicasSensibles(8.0, 2.0).length)
  }

  test("replicasSensibles: con minimo negativo termina y no incluye ceros"){
    val r = replicasSensibles(1.0, -1.0)
    assert(r.length == 24 && r.head == 1.0 && r.last == 0.01)
  }
  test("replicasSensibles: magnitud 9.0 con minimo 4.0"){
    assert(replicasSensibles(9.0, 4.0) == List(9.0, 7.2, 5.76, 4.61))
  }

  test("replicasSensibles: con minimo 0.0 todos los valores son positivos"){
    val r = replicasSensibles(5.0, 0.0)
    assert(r.length == 31 && r.forall(_ > 0))
  }

  test("informe: tiene las mismas regiones que porRegion"){
    assert(informe(nuevo, 4.0).keySet == porRegion(nuevo).keySet)
  }

  test("informe: el conteo y la maxima coinciden con los del punto 2"){
    val i = informe(nuevo, 4.0)
    assert(i.map { case (r, x) => (r, x.sismos)} == conteoPorRegion(nuevo))
    assert(i.map { case (r, x) => (r, x.maxima)} == magnitudMaxima(nuevo))
  }
  test("informe: no depende del orden del registro"){
    assert(informe(nuevo.reverse, 4.0) == informe(nuevo, 4.0))
  }
  test("informe: con minimo muy alto no hay replicas sensibles"){
    assert(informe(nuevo, 10.0) == Map("Cauca" -> Resumen(3, 6.2, 0), "Huila" -> Resumen(2, 5.8, 0)))
  }
  test("informe: una sola region con un solo sismo"){
    val unico = List(Sismo(Fecha(2026, 5), "Narino", 4.5, 10))
    assert(informe(unico, 4.0) == Map("Narino" -> Resumen(1, 4.5, 1)))
  }
}

