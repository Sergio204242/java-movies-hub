package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;


public class MoviesServer {
    private final int port;
    private final HttpServer server;
    private MoviesStore moviesStore;

    public MoviesServer(MoviesStore moviesStore, int port) throws IOException {
        this.moviesStore = moviesStore;
        this.port = port;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new BaseHttpHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

}