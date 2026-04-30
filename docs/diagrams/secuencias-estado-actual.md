# Diagramas de secuencia - Estado actual del código

Estos diagramas describen solamente lo que está implementado por ahora en:

```text
app/Main.java
petri/PetriNet.java
petri/Marking.java
```

Todavía no incluyen monitor, hilos, políticas, colas ni tiempos.

## 1. Ejecución actual del Main

Este diagrama muestra el ejemplo mínimo que hay en `Main`: se crea una red con dos plazas y dos transiciones, se consulta el marcado, se consultan las transiciones sensibilizadas y se dispara `T0`.

```plantuml
@startuml
title Ejecucion actual del Main

actor Usuario
participant "Main" as Main
participant "Marking" as Marking
participant "PetriNet" as PetriNet

Usuario -> Main: ejecutar main(args)

Main -> Main: crear incidenceMatrix
Main -> Marking: new Marking([1, 0])
activate Marking
Marking -> Marking: copiar tokens
Marking --> Main: initialMarking
deactivate Marking

Main -> PetriNet: new PetriNet(initialMarking, incidenceMatrix)
activate PetriNet
PetriNet -> PetriNet: validateMatrix(...)
PetriNet -> PetriNet: copyMatrix(...)
PetriNet --> Main: petriNet
deactivate PetriNet

Main -> PetriNet: getMarking()
activate PetriNet
PetriNet -> Marking: copy()
activate Marking
Marking --> PetriNet: copia del marcado
deactivate Marking
PetriNet --> Main: Marking [1, 0]
deactivate PetriNet

Main -> PetriNet: getEnabledTransitions()
activate PetriNet
PetriNet -> PetriNet: recorrer T0 y T1
PetriNet -> PetriNet: isEnabled(0)
PetriNet -> PetriNet: isEnabled(1)
PetriNet --> Main: [0]
deactivate PetriNet

Main -> PetriNet: fire(0)
activate PetriNet
PetriNet -> PetriNet: isEnabled(0)
PetriNet -> PetriNet: calculateNextMarking(0)
PetriNet -> Marking: add(columna T0)
activate Marking
Marking --> PetriNet: nuevo Marking [0, 1]
deactivate Marking
PetriNet -> PetriNet: marking = nuevo Marking
PetriNet --> Main: true
deactivate PetriNet

Main -> PetriNet: getMarking()
activate PetriNet
PetriNet -> Marking: copy()
activate Marking
Marking --> PetriNet: copia del marcado
deactivate Marking
PetriNet --> Main: Marking [0, 1]
deactivate PetriNet

@enduml
```

## 2. Consulta de sensibilización: `isEnabled(transition)`

Este diagrama muestra cómo `PetriNet` decide si una transición está sensibilizada. La idea es calcular un marcado tentativo y verificar que no queden tokens negativos.

```plantuml
@startuml
title PetriNet.isEnabled(transition)

participant "Cliente" as Cliente
participant "PetriNet" as PetriNet
participant "Marking" as Marking

Cliente -> PetriNet: isEnabled(transition)
activate PetriNet

PetriNet -> PetriNet: validateTransition(transition)
PetriNet -> PetriNet: calculateNextMarking(transition)
activate PetriNet
PetriNet -> PetriNet: getColumn(transition)
PetriNet -> Marking: add(columna)
activate Marking
Marking -> Marking: sumar tokens + columna
Marking --> PetriNet: nextMarking
deactivate Marking
PetriNet --> PetriNet: nextMarking
deactivate PetriNet

PetriNet -> Marking: hasNegativeTokens()
activate Marking
Marking --> PetriNet: true/false
deactivate Marking

alt no hay tokens negativos
    PetriNet --> Cliente: true
else hay al menos un token negativo
    PetriNet --> Cliente: false
end

deactivate PetriNet

@enduml
```

## 3. Disparo exitoso: `fire(transition)`

Este diagrama muestra el caso en el que una transición puede dispararse. Primero se valida con `isEnabled`. Si da `true`, se calcula el nuevo marcado y se guarda en la red.

```plantuml
@startuml
title PetriNet.fire(transition) - caso exitoso

participant "Cliente" as Cliente
participant "PetriNet" as PetriNet
participant "Marking" as Marking

Cliente -> PetriNet: fire(transition)
activate PetriNet

PetriNet -> PetriNet: isEnabled(transition)
PetriNet --> PetriNet: true

PetriNet -> PetriNet: calculateNextMarking(transition)
activate PetriNet
PetriNet -> PetriNet: getColumn(transition)
PetriNet -> Marking: add(columna)
activate Marking
Marking --> PetriNet: nuevo Marking
deactivate Marking
PetriNet --> PetriNet: nuevo Marking
deactivate PetriNet

PetriNet -> PetriNet: marking = nuevo Marking
PetriNet --> Cliente: true

deactivate PetriNet

@enduml
```

## 4. Disparo fallido: `fire(transition)`

Este diagrama muestra el caso en el que una transición no puede dispararse. `isEnabled` devuelve `false`, entonces `fire` no modifica el marcado.

```plantuml
@startuml
title PetriNet.fire(transition) - caso fallido

participant "Cliente" as Cliente
participant "PetriNet" as PetriNet

Cliente -> PetriNet: fire(transition)
activate PetriNet

PetriNet -> PetriNet: isEnabled(transition)
PetriNet --> PetriNet: false

PetriNet --> Cliente: false
note right of PetriNet
  El marcado no se modifica.
end note

deactivate PetriNet

@enduml
```

## 5. Consulta de todas las transiciones sensibilizadas

Este diagrama muestra cómo `getEnabledTransitions()` recorre todas las transiciones y usa `isEnabled` para construir el conjunto resultado.

```plantuml
@startuml
title PetriNet.getEnabledTransitions()

participant "Cliente" as Cliente
participant "PetriNet" as PetriNet

Cliente -> PetriNet: getEnabledTransitions()
activate PetriNet

PetriNet -> PetriNet: crear enabledTransitions

loop transition = 0 hasta cantidadDeTransiciones - 1
    PetriNet -> PetriNet: isEnabled(transition)
    alt isEnabled devuelve true
        PetriNet -> PetriNet: agregar transition al conjunto
    else isEnabled devuelve false
        PetriNet -> PetriNet: no agregar transition
    end
end

PetriNet --> Cliente: enabledTransitions
deactivate PetriNet

@enduml
```

## Resumen para pasar junto con el código

```text
El código actual implementa una Red de Petri secuencial mínima.

Marking representa el marcado actual mediante un arreglo de tokens.
PetriNet guarda el marcado y la matriz de incidencia.

Para saber si una transición está sensibilizada, PetriNet calcula un marcado tentativo:

    marcadoTentativo = marcadoActual + columnaDeLaTransicion

Si ese marcado tentativo tiene algún valor negativo, la transición no está sensibilizada.
Si no tiene valores negativos, la transición está sensibilizada.

fire(transition) primero llama a isEnabled(transition).
Si isEnabled devuelve false, no modifica el marcado y devuelve false.
Si isEnabled devuelve true, calcula el nuevo marcado, lo guarda y devuelve true.

getEnabledTransitions() recorre todas las transiciones y devuelve las que están sensibilizadas.

Todavía no está implementado el monitor, la concurrencia, las colas, la política ni la semántica temporal.
```
