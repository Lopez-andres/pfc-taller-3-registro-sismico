# Informe de corrección: Taller 3, registro sísmico

Asignatura de Fundamentos de Programación Funcional y Concurrente.

## Argumentar la corrección 

Sea $f: A \to B$ la especificación de una función y $P_f$ el programa en Scala que la implementa. Se 
demuestra que $P_f$ es correcto probando

$$\forall a \in A : P_f(a) == f(a)$$

por inducción estructural sobre $A$: se prueba el caso base y, en el caso de inducción, se supone la 
hipótesis de inducción (HI) sobre el valor más pequeño y se prueba la tesis.

**Tipos.** Un $\text{Registro}$ se define recursivamente: $[\,] \in \text{Registro}$, y si $s$ es un sismo y
$r \in \text{Registro}$, entonces $s :: r \in \text{Registro}$. Un sismo tiene `region`, `magnitud`, `fecha` y
`profundidad`; solo las dos primeras intervienen en los puntos 1 y 2.

**Funciones de la librería que se usan.** Para una lista, un predicado $p$ y una función $h$:

$$\text{filter}(p, [\,]) = [\,] \qquad \text{filter}(p, s::r) = \begin{cases} s :: \text{filter}(p, r) & \text{si } p(s) \\ \text{filter}(p, r) & \text{si no} \end{cases}$$

$$\text{map}(h, [\,]) = [\,] \qquad \text{map}(h, s::r) = h(s) :: \text{map}(h, r)$$

La expresión `for (s <- r if p(s)) yield h(s)` se traduce a `r.withFilter(p).map(h)` (ver `proceso.md`), que 
devuelve la misma lista que $\text{map}(h, \text{filter}(p, r))$.

## Punto 1. Consultas con expresiones `for`

### Lema 1: filtrar y proyectar

Sea $Q_{p,h}(r) = \text{map}(h, \text{filter}(p, r))$. Se cumple:

$$Q_{p,h}([\,]) = [\,] \qquad Q_{p,h}(s::r) = \begin{cases} h(s) :: Q_{p,h}(r) & \text{si } p(s) \\ Q_{p,h}(r) & \text{si no} \end{cases}$$

Esta es la especificación $f$ de `fuertes` y `enRegion`, y el programa $P_f$ es `for (s <- r if p(s)) yield h(s)`.
Se demuestra $P_f(r) == f(r)$ por inducción sobre $r$. 

**Caso base:** $r = [\,]$.

$$P_f([\,]) \rightarrow \text{map}(h, \text{filter}(p, [\,])) \rightarrow \text{map}(h, [\,]) \rightarrow [\,] = f([\,])$$

**Caso de inducción:** $r = s :: r'$, con HI: $P_f(r') == f(r')$.

* Si $p(s)$: $\text{filter}(p, s::r') = s :: \text{filter}(p, r')$, luego

$$P_f(s::r') \rightarrow h(s) :: \text{map}(h, \text{filter}(p, r')) = h(s) :: P_f(r') \overset{\text{HI}}{=} h(s) :: f(r') = f(s::r')$$

* Si no $p(s)$: $\text{filter}(p, s::r') = \text{filter}(p, r')$, luego

$$P_f(s::r') \rightarrow \text{map}(h, \text{filter}(p, r')) = P_f(r') \overset{\text{HI}}{=} f(r') = f(s::r')$$

En ambos casos se cumple la tesis. Por inducción estructural, $\forall r : P_f(r) == f(r)$. $\blacksquare$

Del lema se sigue que el resultado conserva el orden del registro y no quita repetidos: cada sismo que pasa el 
filtro se agrega al frente de lo que produce el resto, sin comparar con los demás.

### `fuertes` y `enRegion`

Ambas son casos del Lema 1:

| Función                | $p(s)$                       | $h(s)$        |
|------------------------|------------------------------|---------------|
| `fuertes(r, umbral)`   | $s.\text{magnitud} > umbral$ | $s.\text{region}$   |
| `enRegion(r, region)`  | $s.\text{region} = region$   | $s.\text{magnitud}$ |

Por lo tanto ambas son correctas. Con el registro del enunciado, `fuertes(registro, 5.0)` pasa los sismos 2, 4 y 6
y da `List("Santander", "Choco", "Valle")`, como pide el enunciado. Si ningún sismo cumple $p$, la HI y el 
caso base dan $[\,]$, que es el resultado de `enRegion(registro, "Amazonas")`.

### `paresCercanos`

**Especificación.** Sea $[\rho_0, \dots, \rho_{n-1}]$ la lista de regiones del registro sin repetidos y en orden 
de primera aparición, es decir $\text{distinct}(\text{map}(\text{region}, r))$. Dos regiones son cercanas si 

$$\text{cerca}_d(a, b) \iff \exists\, s_1, s_2 \in r :\ s_1.\text{region} = a \,\wedge\, s_2.\text{region} = b \,\wedge\, \lvert s_1.\text{magnitud} - s_2.\text{magnitud} \rvert \le d$$

y la especificación es la lista de parejas, en orden lexicográfico de $(i, j)$:

$$f(r, d) = [\,(\rho_i, \rho_j) \mid 0 \le i < j < n, \ \text{cerca}_d(\rho_i, \rho_j)\,]$$

**Demostración.** El programa tiene tres piezas:

1. **`cercanas(a, b)` es $\text{cerca}_d(a, b)$.** `exists` cumple $\text{exists}(q, [\,]) = \text{falso}$ y
   $\text{exists}(q, s::r) = q(s) \vee \text{exists}(q, r)$, así que es verdadero si y solo si algún elemento cumple $q$.
   Los dos `exists` anidados expresan los dos cuantificadores $\exists s_1, s_2$ de la definición.
2. **El `for` interior es una instancia del Lema 1.** Para cada $i$, `j <- (i + 1) until n` recorre la lista
   $[i+1, \dots, n-1]$ con $p(j) = \text{cercanas}(\rho_i, \rho_j)$ y $h(j) = (\rho_i, \rho_j)$. Por el Lema 1 produce
   $L_i = [(\rho_i, \rho_j) \mid i < j < n,\ \text{cerca}_d(\rho_i, \rho_j)]$, con $j$ creciente.
3. **El `for` exterior concatena.** `i <- 0 until n` se traduce a `flatMap`, que une las listas
   $L_0 \mathbin{+\!\!+} L_1 \mathbin{+\!\!+} \cdots \mathbin{+\!\!+} L_{n-1}$. Esa lista contiene exactamente las parejas de
   $f(r, d)$ y en el mismo orden lexicográfico de $(i, j)$. Entonces $P_f(r, d) == f(r, d)$. $\blacksquare$

**Cada pareja sale una sola vez.** Como $i < j$ y las regiones $\rho_i$ son distintas entre sí, ninguna pareja 
tiene dos regiones iguales. Si $(\rho_i, \rho_j)$ sale, la pareja inversa $(\rho_j, \rho_i)$ necesitaría $j < i$, lo que 
el rango $(i+1)$ `until` $n$ nunca produce. 

**La tolerancia $10^{-9}$.** El programa compara contra $d + 10^{-9}$ y la especificación contra $d$. Con magnitudes 
y $d$ de a lo sumo dos decimales, como en el registro, la diferencia exacta es múltiplo de $0.01$: o es $\le d$ o es 
$\ge d + 0.01$. El error de punto flotante de la resta es del orden de $10^{-15}$, menor que $10^{-9}$ y que 
$0.01$, así que la tolerancia solo evita que ese error deje fuera una pareja que sí cumple. Por ejemplo, 
$\lvert 5.1 - 5.3 \rvert$ da $0.20000000000000018$ en `Double`, y con $d = 0.2$ la pareja 
`("Valle", "Santander")` sigue saliendo. 

### Conclusión del punto 1

`fuertes` y `enRegion` son instancias del Lema 1, y `paresCercanos` es un `flatMap` de instancias del Lema 1 sobre 
la especificación $\text{cerca}_d$. Las tres se demostraron correctas por inducción estructural sobre el registro. 

## Punto 2. Agrupar con mapas 

### Especificación 

Sea $\text{Reg}(r)$ el conjunto de regiones que aparecen en $r$, y para $g \in \text{Reg}(r)$ sea 

$$S_g(r) = \text{filter}(s \mapsto s.\text{region} = g,\ r)$$

la lista de sismos de la región $g$, en el orden del registro. Las especificaciones son, para $g \in \text{Reg}(r)$:

$$\text{porRegion}(r)(g) = S_g(r) \qquad \text{conteo}(r)(g) = \lvert S_g(r) \rvert \qquad \text{maxima}(r)(g) = \max \{\, s.\text{magnitud} \mid s \in S_g(r) \,\}$$

y los tres mapas no tienen más llaves que $\text{Reg}(r)$. Si $r = [\,]$, $\text{Reg}(r) = \varnothing$ y los tres son `Map()`.

### `porRegion`

El programa es `r.groupBy(s => s.region)`. El contrato de `groupBy` es: sus llaves son exactamente los valores 
que toma la función sobre los elementos, y el valor de cada llave es la lista de los elementos con esa llave, en 
el orden original. Con la función `region`, las llaves son $\text{Reg}(r)$ y el valor de $g$ es $S_g(r)$. Entonces
$P_{\text{porRegion}}(r) == \text{porRegion}(r)$. $\blacksquare$

### `conteoPorRegion` y `magnitudMaxima`

Ambas son `porRegion(r).map { case (g, sismos) => (g, F(sismos)) }`, con
$F(\text{sismos}) = \text{sismos.length}$ en el conteo y $F(\text{sismos}) = \text{sismos.map(}s \Rightarrow s.\text{magnitud}\text{).max}$
en la máxima. 

* `map` sobre un `Map` aplica la función a cada entrada. Las llaves de `porRegion(r)` son distintas, así que las 
  entradas nuevas $(g, F(S_g(r)))$ también tienen llaves distintas y el resultado tiene una entrada por cada 
  $g \in \text{Reg}(r)$ y ninguna más.
* **Conteo:** $F(S_g(r)) = \lvert S_g(r) \rvert$ por definición de `length`.
* **Máxima:** `.max` falla con una lista vacía, pero $g \in \text{Reg}(r)$ significa que algún sismo tiene región 
  $g$, es decir $S_g(r) \neq [\,]$. Luego `.max` está definido y es el mayor valor de la lista de magnitudes
  de $S_g(r)$.

Por el resultado de `porRegion`, ambas coinciden con su especificación. Con el registro del enunciado:
`Valle` $\mapsto$ sismos 1, 3, 6 da conteo $3$ y máxima $\max\{4.2, 3.8, 5.3\} = 5.3$. $\blacksquare$

### El resultado no depende del orden en que se recorre el registro

Sea $r'$ cualquier reordenamiento de $r$ (la misma colección de sismos en otro orden). Se demuestra que
$\text{conteo}(r') = \text{conteo}(r)$ y $\text{maxima}(r') = \text{maxima}(r)$.

* **Llaves.** $\text{Reg}(r') = \text{Reg}(r)$, porque ambos registros contienen los mismos sismos. 
* **Conteo.** Con el corchete de Iverson $[\,\cdot\,]$, que vale $1$ si la condición se cumple y $0$ si no:

$$\lvert S_g(r) \rvert = \sum_{s \in r} [\,s.\text{region} = g\,]$$

La suma de números naturales es conmutativa, así que no cambia al reordenar los sismos. Entonces
$\lvert S_g(r') \rvert = \lvert S_g(r) \rvert$.

* **Máxima.** $\max$ es conmutativa y asociativa, así que el máximo de una colección finita no depende del orden de
  sus elementos. $S_g(r')$ tiene los mismos sismos que $S_g(r)$, luego
  $\max \{\, s.\text{magnitud} \mid s \in S_g(r') \,\} = \max \{\, s.\text{magnitud} \mid s \in S_g(r) \,\}$.
* **Mapa.** Dos `Map` son iguales si tienen las mismas llaves con los mismos valores, sin importar el orden en que
  se escriban o se impriman.

Entonces `conteoPorRegion` y `magnitudMaxima` devuelven el mismo `Map` para $r$ y para $r'$. En `porRegion` lo único
que depende del orden es el orden interno de cada lista $S_g$, que es un reordenamiento de la lista que se obtiene 
con $r$; como conjunto de sismos por región, el resultado es el mismo. $\blacksquare$

### Conclusión del punto 2

`porRegion`, `conteoPorRegion` y `magnitudMaxima` coinciden con su especificación para toda región $g \in \text{Reg}(r)$,
y `.max` nunca se aplica a una lista vacía. Además, `conteoPorRegion` y `magnitudMaxima` devuelven el mismo `Map` para 
cualquier reordenamiento del registro, porque la suma y el máximo son conmutativos.
