## I. Start the app

### 1. Start backend

#### - tab 1

```shell
sbt "~backend / run"
```

### 2. Start frontend

#### - tab 2

```shell
sbt "~frontend / fastOptJS"
```

#### - tab 3


Then you can access the frontend at http://localhost:8090


This project is a Proof of Concept for Scala 3 + ZIO 2 + zio-http + Laminar so not all simplifications or refactors that
can be done are done.
