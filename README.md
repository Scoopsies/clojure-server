# clojure-server

A Clojure wrapper around a [Java HTTP server](https://github.com/Scoopsies/HTTP-Server) that adds route handling, including a web-playable [tic-tac-toe](https://github.com/Scoopsies/clojure-tic-tac-toe) game.

## Why

This project is an exercise in integrating separately built libraries and writing clean, extensible code. The HTTP server was written in Java with no knowledge of tic-tac-toe, and tic-tac-toe was written in Clojure with no knowledge of the server. Neither was designed with this integration in mind — but because both were built with clean boundaries, they work together with minimal glue. The server exposes a `RouteHandler` interface for adding new routes, and tic-tac-toe exposes its game logic as pure functions. This project is the payoff of that discipline.

## Interoperability

This project combines two languages and two independently built JARs:

- **Java <-> Clojure**: The HTTP server is written in Java and packaged as a JAR. Clojure runs on the JVM, so Java classes are available directly via `:import`. The server's `RouteHandler` interface is implemented in Clojure using `deftype`, and Java classes like `ResponseBuilder` and `ArgParser` are called like any other Clojure function.

- **Local JAR dependencies**: Both `HttpServer.jar` and `TicTacToe.jar` are declared as `:local/root` dependencies in `deps.edn`, pointing to the `lib/` directory. Clojure's tools.deps treats them like any other dependency — Java classes are importable and Clojure namespaces (including `.cljc` files) are requireable.

- **Why JARs instead of source paths?**: Each project has its own dependency tree. Packaging them as JARs keeps the boundaries clean — this project depends on their public interfaces, not their source trees or build tooling.

## Prerequisites

- [Java](https://adoptium.net/) (JDK 8+)
- [Clojure](https://clojure.org/guides/install_clojure)
- [Maven](https://maven.apache.org/) (for building the HTTP-Server JAR)
- [PostgreSQL](https://www.postgresql.org/download/) (for tic-tac-toe game persistence)

## Setup

The project depends on two local JARs in `lib/`. The `setup.sh` script builds them from sibling repos:

```bash
# Clone the dependency repos as siblings
git clone https://github.com/Scoopsies/HTTP-Server.git ../HTTP-Server
git clone https://github.com/Scoopsies/clojure-tic-tac-toe.git ../clojure-tic-tac-toe

# Build JARs and copy them into lib/
./setup.sh
```

## Running

```bash
clj -M:run -x
```

## Running Tests

```bash
clj -M:test:spec
```
