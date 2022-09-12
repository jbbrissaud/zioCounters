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

### 3. Test the app


Then you can access the frontend at http://localhost:8090 \
click on one button or the other and see parallelism.


This project is a very basic app in Scala 3 + ZIO 2 + zio-http + Laminar.\
The backend uses zio-http and zio 2.\
The frontend uses Laminar and zio 2, allowing parallelism in a single-threaded web page.

The project is in itself totally useless, but is a good starting point for anyone interested in this amazing technology.

Note: The .gitignore file is a little bit unconventional, indicating what to keep and not what to ignore.

