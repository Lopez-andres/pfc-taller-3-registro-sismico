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

$$\text{porRegion}(r)(g) = S_g(r) \qquad \text{conteo}(r)(g) = \lvert S_g(r) \rvert \qquad \text{máxima}(r)(g) = \max \{\, s.\text{magnitud} \mid s \in S_g(r) \,\}$$

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
$\text{conteo}(r') = \text{conteo}(r)$ y $\text{máxima}(r') = \text{máxima}(r)$.

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


## Punto 3. Consultas seguras

### Especificación 

El objetivo de este punto es consultar la información del registro sin que haya errores cuando el registro 
está vacío o una región no existe

Matemáticamente, lo podemos expresar como: sea $r$ un registro y $\mathrm{Reg}(r)$ el conjunto de regiones 
presentes en él (según definido en el punto 2)

- $\mathrm{Reg}(r)$: Conjunto de regiones presentes en el registro sísmico
- $Reg$: Operación que obtiene las regiones del registro 
- $r$: Es el registro del sismo completo o la lista de sismos (Registro)
- $g$: Representa una región específica (Ejemplo: "Valle", "Choco", etc.)
- $C_r(g)$: Cantidad de sismos registrados en la región $g$
- $C$: Conteo o cantidad de sismos
- $M_r(g)$: Magnitud máxima de los sismos en la región $g$
- $M$: Magnitud máxima

### conteoSeguro

La función debe devolver el número de sismos de una región existente y cero cuando la región no aparece en el registro

Matemáticamente, se puede expresar como:

$$
C_r^*(g) =
\begin{cases}
C_r(g) & \text{si } g \in \mathrm{Reg}(r)\\
0 & \text{si } g \notin \mathrm{Reg}(r)
\end{cases}
$$

- $^*$: Indica que el conteo es seguro

`Ejemplo:`

El registro sísmico contiene:
- `Valle:` 3 sismos
- `Santander:`2 sismos
- `Choco:` 2 sismos

La función que se usa es:
```scala
def conteoSeguro(r: Registro): Map[String, Int] =
  conteoPorRegion(r).withDefaultValue(0)
```

La función `conteoPorRegion(r)` da como resultado el número de sismos en cada región
```scala
Map("Valle" -> 3, "Santander" -> 2, "Choco" -> 2)
```

Aplicando `withDefaultValue(0)` se obtiene que:

| Región      | Cantidad esperada | Resultado obtenido |
|-------------|-------------------|--------------------|
| `Valle`     | $3$               | $3$                |
| `Santander` | $2$               | $2$                |
| `Amazonas`  | $0$               | $0$                |

Por lo tanto, si la región existe devuelve $C_r(g)$ y si no $0$, coincidiendo los casos con $C_r^*(g)$


### `maximaDe`
Debe devolver la magnitud máxima de una región si tiene sismos registrados o `None` cuando no tiene

Se expresa matemáticamente como:

$$
f(r,g) =
\begin{cases}
\mathrm{Some}(M_r(g)) & \text{si } g \in \mathrm{Reg}(r)
\mathrm{None} & \text{si } g \notin  \mathrm{Reg}(r)
\end{cases}
$$

Si la región existe en el registro, la función devuelve `Some` con la magnitud máxima de esa región y 
si no devuelve `None`

- $f(r, g)$: La función `maximaDe`, que recibe el registro y una región
- `Some:` Que encontró un valor 
- `None:` Que no existe un valor para devolver

`Ejemplo:`

El registro sísmico contiene las siguientes magnitudes:
- `Valle:` 4.2, 3.8 y 5.3
- `Santander:` 5.1 y 4.7
- `Choco:` 6.0 y 3.1

Entonces se usa la función:
```scala
def maximaDe(r: Registro, region: String): Option[Double] =
  magnitudMaxima(r).get(region)
```

La función `magnitudMaxima(r)` da como resultado un mapa con la magnitud mas alta registrada de cada región
```scala
Map("Valle" -> 5.3, "Santander" -> 5.1, "Choco" -> 6.0)
```

Al aplicar `.get(region)` se obtiene `Some(valor)` cuando la región existe y `None` cuando no está registrada

| Región      | Magnitud máxima esperada | Resultado obtenido |
|-------------|--------------------------|--------------------|
| `Valle`     | $5.3$                    | `Some(5.3)`        |
| `Santander` | $5.1$                    | `Some(5.1)`        |
| `Choco`     | $6.0$                    | `Some(6.0)`        |
| `Amazonas`  | `None`                   | `None`             |

Arrojando que si la región existe devuelve $\mathrm{Some}(M_r(g))$ y si no $\mathrm{None}$,
coincidiendo ambos casos con la especificación matemática $f(r, g)$, evitando consultar directamente 
claves inexistentes

### `regionMasActiva`

Debe devolver la región con mayor cantidad de sismos. Si varias regiones tienen la misma cantidad exacta, 
debe seleccionar la primera en orden alfabético y si está vacío el registro devuelve `None` 

Para un registro no vacío es: $m = \max_{g \in \mathrm{Reg}(r)} C_r(g)$

El conjunto de regiones con mayor cantidad de sismos es: $A = \{g \in \mathrm{Reg}(r) \mid C_r(g) = m\}$

La región seleccionada es: $g^* = \min A$

- $\min A$: La primera región en orden alfabético
- $g^*$: El asterisco indica cuál es la región seleccionada como la más activa
- $A$: Conjunto de regiones que tienen la máxima cantidad de sismos

La especificación viene siendo: Para cualquier registro sísmico, la función devuelve `None` si el registro está vacío,
en caso contrario devuelve `Some` con la región que tiene la mayor cantidad de sismos y si hay empate entonces es con 
la primera región en orden alfabético

$$
f(r) =
\begin{cases}
\mathrm{None} & \text{si } r = []\\
\mathrm{Some}(g^*) & \text{si } r \neq []
\end{cases}
$$

- $f$: Representa la función `regionMasActiva`

`Ejemplo:`

El registro sísmico se sabe que contiene:
- `Valle:` 3 sismos
- `Santander:`2 sismos
- `Choco:` 2 sismos

La función que se usa es:

```scala
def regionMasActiva(r: Registro): Option[String] = {
  val conteo = conteoPorRegion(r)

  if (conteo.isEmpty) None
  else {
    val mayor = conteo.values.max
    Some(conteo.keys.filter(region => conteo(region) == mayor).min)
  }
}
```

Entonces después de que la función `conteoPorRegion(r)` de sus resultados `conteo.values.max` obtiene la cantidad 
máxima de sismos:

```scala
val mayor = conteo.values.max // 3
```

Aplicando `filter`, se seleccionan las regiones que tienen una cantidad igual al máximo en sismos


```scala
conteo.keys.filter(region => conteo(region) == mayor) 
// Selecciona la región "Valle"
```

Finalmente, `min` selecciona la primera región en orden alfabético entre las que tienen la máxima cantidad de sismos, 
y `Some(...)` devuelve el resultado 

```scala
regionMasActiva(registro) // Some("Valle")
```

| Región      | Cantidad de sismos | ¿Cantidad máxima? |
|-------------|--------------------|-------------------|
| `Valle`     | $3$                | Sí                |
| `Santander` | $2$                | No                |
| `Choco`     | $2$                | No                |


### Demostración

#### Caso 1: Registro con empate
Si el registro tuviera 2 sismos en `Valle`, 2 en `Santander` y 1 en `Choco`, las regiones con mayor cantidad serían
`Valle` y `Santander`. En ese caso, `.min` seleccionaría `Santander`, porque aparece primero en orden alfabético 

| Región      | Cantidad de sismos | ¿Cantidad máxima? |
|-------------|--------------------|-------------------|
| `Valle`     | $2$                | Sí                |
| `Santander` | $2$                | Sí                |
| `Choco`     | $1$                | No                |


#### Caso 2: Registro vacío

```scala
regionMasActiva(List()) // None
```
Cuando el registro está vacío $r = []$, `conteo.isEmpty` es verdadero y la función devuelve `None`, 
evitando aplicar `.max` sobre un mapa vacío. 

Por lo tanto, si el registro contiene sismos, la función devuelve $\operatorname{Some}(g^*)$, donde $g^*$ representa la 
primera región alfabéticamente entre las que tiene la cantidad máxima de sismos. Si el registro está vacío, 
devuelve $\operatorname{None}$, coincidiendo ambos casos con la especificación matemática $f(r)$.

| Registro   | Resultado de `conteoPorRegion` | `conteo.isEmpty` | Resultado |
|------------|--------------------------------|------------------|-----------|
| `List()`   | `Map()`                        | `true`           | `None`    |


#### Caso 3: Registro no vacío

Si el registro contiene al menos un sismo, `conteoPorRegion(r)` genera un mapa con las regiones y sus cantidades 
correspondientes, realizando estas operaciones:

1. `conteo.values.max` obtiene la cantidad mayor de sismos representada por $m$
2. `conteo.keys.filter(region => conteo(region) == mayor)` selecciona las regiones que tienen esa cantidad máxima, 
    formando el conjunto $A$
3.  `.min` selecciona la primera región alfabéticamente dentro del conjunto $A$
4.  `Some(...)` devuelve la región seleccionada

| Región  | Cantidad de sismos | ¿Cantidad máxima? |
|---------|--------------------|-------------------|
| `Cauca` | $3$                | Sí                |
| `Huila` | $1$                | No                |

Como no está vacío siempre existe al menos una región con la cantidad máxima de sismos, por eso `.max` y `.min` 
pueden operar.

Por lo tanto, la función devuelve $\operatorname{Some}(g^*)$, donde $g^*$ representa la región seleccionada, cumpliendo 
la especificación matemática 

### Independencia del orden del registro

Si cambiamos el orden de los sismos en el registro, los resultados de las funciones no cambian,
porque serían los mismos con las mismas regiones, para demostrarlo usamos $r$ al registro original, y $r'$ al organizado 
con otro orden

En `conteoSeguro`, la cantidad de sismos de cada región sigue siendo la misma: $C_{r'}(g) = C_r(g)$
En `maximaDe`, la magnitud máxima tampoco cambia, porque se comparan los mismos valores: $M_{r'}(g) = M_r(g)$
En `regionMasActiva`, al conservarse las mismas cantidades de sismos, también conserva las regiones más activas 
y si hay empate `.min` sigue seleccionando la primera región en orden alfabético

| Posición | Registro original $r$ | Registro reorganizado $r'$ |
|----------|-----------------------|----------------------------|
| 1        | `Valle` (4.2)         | `Choco` (6.0)              |
| 2        | `Choco` (6.0)         | `Valle` (5.3)              |
| 3        | `Valle` (5.3)         | `Valle` (4.2)              |

Por tanto: $\operatorname{regionMasActiva}(r') = \operatorname{regionMasActiva}(r)$

Demostrando que las tres funciones mantienen sus resultados aunque aparezcan en diferentes órdenes dentro del registro

### Conclusión 

Se demostró que las funciones `conteoSeguro`, `maximaDe` y `regionMasActiva` cumplen con sus especificaciones 
matemáticas, permitiendo consultar la cantidad de sismos, la magnitud máxima e identificar la región más activa de 
manera segura. 

Además, se comprobó que las funciones manejan correctamente los registros vacíos, las regiones inexistentes y los 
empates, sin que sus resultados dependan del orden de los sismos.



## Punto 4. `replicas` y `replicasSensibles`

**Notación.** $r_k$ ("r sub k") es la réplica de la posición $k$, y la primera posición es la $0$: $\rightarrow$ 
se entiende que "evalúa a" y $\texttt{==}$, "es igual a".

### `replicas`: la secuencia de réplicas

Sea $m$ la magnitud inicial. Cada réplica se obtiene reduciendo en un 20% la magnitud anterior, sin redondear el valor
que se usa para calcular la siguiente; el resultado que se muestra en cada posición sí se redondea a dos decimales.

Sea $\operatorname{red}(x) = \dfrac{\operatorname{round}(100\,x)}{100}$ la función `redondear`. Las réplicas de una 
magnitud $m$ son la secuencia infinita:

$$
R(m) = (r_0, r_1, r_2, \dots), \qquad r_k = \operatorname{red}(0.8^{k}\, m)
$$

Sea $P_{\text{replicas}}$ el programa:

```scala
 def replicas(magnitud: Double): LazyList[Double] =
    redondear(magnitud) #:: replicas(magnitud * 0.8)
```

Para una secuencia perezosa, la celda $\langle x \mid xs \rangle$ tiene cabeza $x$ y cola $xs$, y la cola solo se calcula al pedirla. El razonamiento se hace en aritmética real; 
en `Double`, los productos sucesivos pueden diferir de $0.8^k\, m$ en los últimos bits, sin afectar que la secuencia tienda a cero.

**Lema 2.** Para todo $m$ y todo $k \geq 0$, el elemento de posición $k$ de $P_{\text{replicas}}(m)$ es $r_k = \operatorname{red}(0.8^{k}\, m)$.

**Demostración.** Por inducción sobre $k$, con la propiedad enunciada para todo $m$.

**Caso base:** $k = 0$

$$
P_{\text{replicas}}(m) \rightarrow \langle \operatorname{red}(m) \mid P_{\text{replicas}}(0.8\, m)\rangle
$$

La cabeza es $\operatorname{red}(m) = \operatorname{red}(0.8^{0}\, m) = r_0$. Por lo tanto, la propiedad se cumple para la 
posición inicial.

**Caso de inducción:** $k = j + 1$, $j \geq 0$. La hipótesis de inducción (HI) dice que, para 
cualquier magnitud $m'$, el elemento de posición $j$ de $P_{\text{replicas}}(m')$ es $\operatorname{red}(0.8^{j}\, m')$.

El elemento de posición $j + 1$ de $P_{\text{replicas}}(m)$ es el de posición $j$ de su cola, que por la definición es $P_{\text{replicas}}(0.8\, m)$.
Aplicando la HI con $m' = 0.8\, m$:

$$
\rightarrow \operatorname{red}(0.8^{j} \cdot 0.8\, m) = \operatorname{red}(0.8^{j+1}\, m) = r_{j+1}
$$

Por lo tanto, la propiedad también se cumple para la posición $j + 1$.

Concluimos por inducción que, para todo $m$ y todo $k \geq 0$, el elemento de posición $k$ de $P_{\text{replicas}}(m)$ es $r_k$.
$\blacksquare$

**Corolario (pereza).** Para todo $n$, $P_{\text{replicas}}(m).\mathrm{take}(n).\mathrm{toList}$ devuelve $(r_0, \dots, r_{n-1})$ y solo calcula $n$ celdas. 
La demostración nunca usa toda la secuencia, que es infinita, sino sus primeros $n$ elementos, y por eso `take(4)` termina.

**Ejemplo.** Para $m = 7.0$, $P_{\text{replicas}}(7.0).\mathrm{take}(4)$ devuelve $(7.0,\ 5.6,\ 4.48,\ 3.58)$, que corresponden a 
$\operatorname{red}(7.0),\ \operatorname{red}(5.6),\ \operatorname{red}(4.48)$ y  $\operatorname{red}(3.584)$.

### `replicasSensibles`: especificación

Aquí $\mu$ ("mu") es el mínimo, $\land$ se lee "y" y $\neg$ se lee "no".

Sea $\mu$ el mínimo y sea $p_\mu$ el predicado que usa el programa:

$$
p_\mu(x) \equiv x > \mu \ \land\ x > 0 
$$

Si $\mu \geq 0$, entonces $x > \mu$ implica $x > 0$ y $p_\mu(x)$ equivale a $x > \mu$, que es exactamente la condición del enunciado.
La cláusula $x > 0$ solo interviene cuando $\mu < 0$: en ese caso la lectura literal del enunciado exigiría una lista infinita, y se decidió
cortar cuando la réplica llega a cero.

La especificación $f(m, \mu)$ es el prefijo más largo de $R(m)$ cuyos elementos cumplen $p_\mu$:

$$
f(m, \mu) = (r_0, \dots, r_{j-1}), \qquad j = \min\{\, k \in \mathbb{N} : \neg\, p_\mu(r_k) \,\}
$$

Sea $P_{\text{sensibles}}$ el programa: 

```scala
  def replicasSensibles(magnitud: Double, minimo: Double): List[Double] =
    replicas(magnitud).takeWhile(m => m > minimo && m > 0).toList
```
Para una secuencia perezosa $\langle x \mid xs \rangle$ con cabeza $x$ y cola $xs$, `takeWhile` cumple
estas ecuaciones, donde $\mathrm{Nil}$ es la lista vacía y $::$ agrega un elemento al inicio:
$$
\mathrm{takeWhile}_p(\langle x \mid xs \rangle) = \begin{cases}
x :: \mathrm{takeWhile}_p(xs) & \text{si } p(x) \\ 
\mathrm{Nil} & \text{si } \neg\, p(x)
\end{cases}
$$

### Lema 3: la secuencia deja de cumplir $p_\mu$

**Enunciado.** Para todo $m$ y todo $\mu$ existe $k$ tal que $\neg\, p_\mu(r_k)$.
Por tanto, el $j$ de la especificación existe.

**Demostración.**

- Si $m \leq 0$: $r_0 = \operatorname{red}(m) \leq 0$, de modo que $r_0 > 0$ es falso y $\neg\, p_\mu(r_0)$.
- Si $m > 0$: como $0 <0.8 < 1$, se tiene $0.8^k\, m \to 0$. Sea $k > \log_{0.8}(0.005 / m)$. Entonces $0.8^k\, m < 0.005$,
así que $100 \cdot 0.8^k\, m < 0.5$ y $\operatorname{round}(100 \cdot 0.8^k\, m) = 0$.
Resulta $r_k = 0$, y $r_k > 0$ es falso, luego $\neg\, p_\mu(r_k)$.

En ambos casos existe una posición en la que la condición falla. Por tanto, la selección termina y el índice $j$ de la 
especificación está definido.

$\blacksquare$

### Teorema 1: `replicasSensibles` cumple su especificación

$$
\forall m, \mu \in \mathbb{R} : P_{\text{sensibles}}(m, \mu) == f(m, \mu)  
$$

**Propiedad.** Sea $s= (s_0, s_1, \dots)$ una secuencia perezosa y $p$ un predicado. Si $p(s_i)$ para todo $i < n$ y 
$\neg\, p(s_n)$, entonces

$$
\mathrm{takeWhile}_p(s) == (s_0, \dots, s_{n-1})
$$

La demostración es por inducción sobre $n$.

**Caso base:** $n = 0$. Como $\neg\, p(s_0)$,

$$
\mathrm{takeWhile}_p(s) \rightarrow \mathrm{Nil} == ()
$$
que es el prefijo vacío.

**Caso de inducción.** $n = k + 1$. La hipótesis de inducción (HI) dice que la propiedad vale para cualquier secuencia cuyo 
primer fallo esté en la posición $k$. Como $k + 1 > 0$, se cumple $p(s_0)$ y entonces

$$
\mathrm{takeWhile}_p(s) \rightarrow s_0 :: \mathrm{takeWhile}_p(\langle s_1, s_2, \dots \rangle)
$$

La cola $(s_1, s_2, \dots)$ cumple $p$ en sus primeras $k$ posiciones y falla en la $k$-ésima. Por la HI:

$$
\rightarrow s_0 :: (s_1, \dots, s_k) == (s_0, \dots, s_k)
$$
que es el prefijo de longitud $k +1$. Concluimos por inducción que la propiedad vale para todo $n$.

**Aplicación.** Por el Lema 2, la secuencia que produce `replicas(m)` es $s = R(m)$. 
Se toma $p = p_\mu$. Por el Lema 3 existe
$j = \min\{k : \neg\, p_\mu(r_k)\}$, y por la propiedad:

$$
\mathrm{takeWhile}_{p_\mu}(R(m)) == (r_0, \dots, r_{j-1}) = f(m, \mu)
$$
Con el modelo de sustitución:

$$
P_{\text{sensibles}}(m, \mu) \rightarrow R(m).\mathrm{takeWhile}(p_\mu).\mathrm{toList}
\rightarrow \mathrm{takeWhile}_{p_\mu}(R(m)).\mathrm{toList}
\rightarrow (r_0, \dots, r_{j-1}) == f(m, \mu)
$$

Como `toList` solo convierte ese prefijo finito en una `List`, $P_{\text{sensibles}}(m, \mu) == f(m, \mu)$. $\blacksquare$

**Sobre la pereza.** La demostración solo examina $r_0, \dots, r_j$, es decir, $j +1$ elementos.
La propiedad se enuncia sobre los primeros elementos y no sobre toda la secuencia, que es infinita y nunca se materializa.

**Ejemplo.** Para $m = 7.0$ y $\mu = 4.0$ las réplicas son $7.0,\ 5.6,\ 4.48,\ 3.58,\ \dots$. El primer fallo está en $r_3 = 3.58$,
así que $j = 3$ y $f(7.0, 4.0) = (7.0,\ 5.6,\ 4.48)$, valor que devuelve el programa.

### Conclusión del punto 4
El Lema 2 da el valor de cada réplica. El Lema 3 demuestra que la secuencia, que no aumenta y tiende a 0, deja de cumplir la condición en algún punto
y por eso `takeWhile` termina. El Teorema 1 demuestra que `replicasSensibles` devuelve exactamente las réplicas que pide la especificación. 
Por lo tanto, `replicasSensibles`$(m, \mu)$ es correcta para todo $m$ y todo $\mu$.

## Punto 5. `informe`

### Especificación de `informe`

Sea $r$ un registro, $\rho$ ("ro") el nombre de una región y $\lvert A \rvert$ la cantidad de elementos de $A$.
Para una región $\rho$:

- $G_\rho(r)$ es la sublista de los sismos de $r$ cuya región es $\rho$, en el orden en que aparecen.
- $\mathcal{R}(r) = \{\, \rho : G_\rho(r) \neq \mathrm{Nil} \,\}$ es el conjunto de regiones presentes.
- $n_\rho = \lvert G_\rho(r) \rvert$ es la cantidad de sismos.
- $M_\rho$ es la mayor magnitud entre los sismos de $G_\rho(r)$.

La especificación es el mapa $\mathrm{inf}(r, \mu)$ de dominio $\mathcal{R}(r)$ tal que

$$
\mathrm{inf}(r, \mu)(\rho) = \mathrm{Resumen}\bigl(n_\rho,\ M_\rho,\ \lvert f(M_\rho, \mu) \rvert\bigr)
$$
 **Hipótesis (agrupamiento del punto 2).** EL comportamiento de `groupBy` es colocar cada sismo en el grupo de su región, 
sin importar su posición, y conservar el orden relativo dentro del grupo. Es decir, el dominio de
`porRegion(r)` es $\mathcal{R}(r)$ y el valor de $porRegion(r)(\rho)$ en $G_\rho(r)$.

**Traducción del `for`.** Según el informe de proceso (sección 5.2), el compilador traduce 

```scala
 for ((region, n) <- conteo) yield (region, Resumen(...))
```

a `conteo.withFilter(q).map(g)`. El filtro $q$ comprueba que cada entrada sea una pareja, y toda entrada de un `Map`
lo es, de modo que no descarta ninguna.

### Lema 4: los dos mapas del punto 2 comparten dominio

**Enunciado.** `conteoPorRegion(r)` y `magnitudMaxima(r)` tienen dominio $\mathcal{R}(r)$, y 

$$
\texttt{conteoPorRegion}(r)(\rho) = n_\rho, \qquad \texttt{magnitudMaxima} (r)(\rho) = M_\rho
$$
**Demostración.** Ambas funciones son de la forma `porRegion(r).map {case (region, g) => (region, h(g))}`, con $h$ igual a la longitud
de $g$ en el primer caso y al máximo de sus magnitudes en el segundo.
El `map` conserva las llaves, que son distintas, y aplica $h$ al valor de cada una. 
Por la hipótesis, la llave $\rho$ tiene como valor $G_\rho(r)$, así que el resultado es $n_\rho$ o $M_\rho$. Ningún grupo es vacío, 
porque `groupBy` solo crea un grupo para una región con al menos un sismo: por eso el máximo está definido.

### Teorema 2: `informe` cumple su especificación

$$
\forall r, \mu : \texttt{informe}(r, \mu) == \mathrm{inf}(r, \mu)
$$
**Demostración.** Por la traducción de `for`, `informe` aplica `map` a las entradas de `conteo`sin descartar ninguna. Por el Lema 4:

- El dominio del resultado es el de `conteo`, es decir, $\mathcal{R}(r)$.
- La llamada `maximas(region)` está definida para toda llave $\rho$ que recorre el `for`, porque `máximas` tiene
el mismo dominio que `conteo`. No se lanza ninguna excepción.

Para cada $\rho \in \mathcal{R}(r)$, el valor en $\rho$ es 

$$
\mathrm{Resumen}\bigl(n_\rho,\ M_\rho,\ \lvert P_{\text{sensibles}}(M_\rho, \mu) \rvert\bigr)
$$
y por el Teorema 1, $\lvert P_{\text{sensibles}}(M_\rho, \mu) \rvert = \lvert f(M_\rho, \mu) \rvert$.
Esto coincide con $\mathrm{inf}(r, \mu)(\rho)$.

El registro vacío queda incluido: $\mathcal{R}(\mathrm{Nil}) = \emptyset$.
`conteo` es un mapa vacío y el resultado es `Map()`.

**Ejemplo.** Para el registro `nuevo` del informe de proceso, la región `Cauca` tiene tres sismos y su mayor magnitud es $6.2$. 
Con $\mu = 4.0$: las réplicas de $6.2$ son $6.2,\ 4.96,\ 3.97,\ \dots$; la primera que falla es $3.97$, así que $\lvert f(6.2, 4.0) \rvert = 2$
 y el valor es $\mathrm{Resumen}(3,\ 6.2,\ 2)$.

### Teorema 3: el resultado no depende del orden del registro

$$
\forall r,\ \forall \pi \text{ permutación de } r : \texttt{informe}(\pi(r), \mu) == \texttt{informe}(r, \mu)
$$
Aquí $\pi(r)$ es el registro $r$ con los sismos en otro orden.

**Demostración.** Por el Teorema 2, basta comparar $\mathrm{inf}(\pi(r), \mu)$ con $\mathrm{inf}(r, \mu)$.

1. **Mismas regiones.** Un sismo pertenece al grupo de su región sin importar en qué posición esté. Por eso 
$G_\rho(\pi(r))$ es una permutación de $G_\rho(r)$ y $\mathcal{R}(\pi(r)) == \mathcal{R}(r)$.
2. **Mismo conteo.** Permutar una lista no cambia su longitud, así que $n_\rho(\pi(r)) = n_\rho(r)$.
3. **Misma máxima.** El máximo de una colección finita de reales depende de los valores y no de su orden, porque $\max(a, b) = \max(b, a)$
y $\max(\max(a, b), c) = \max(a, \max(b, c))$. Por tanto $M_\rho(\pi(r)) = M_\rho(r)$ 
4. **Mismas réplicas.** El tercer componente es función de $M_\rho$ y $\mu$ únicamente. `replicasSensibles` no recorre el registro.

Dos mapas son iguales cuando tienen las mismas llaves y el mismo valor para cada una; el orden de inserción no forma parte de la igualdad. Los dos 
resúmenes coinciden en todas las regiones, y por lo tanto $\mathrm{inf}(\pi(r), \mu) == \mathrm{inf}(r, \mu)$.

### Conclusión del punto 5
Por el Lema 4, los mapas de `conteoPorRegion` y `magnitudMaxima` tienen las mismas regiones, así que `informe` puede consultarlos 
sin error. El Teorema 2 demuestra que `informe` cumple su especificación, y el Teorema 3, que el resultado no depende del orden 
del registro. Por lo tanto, `informe` es correcto.
