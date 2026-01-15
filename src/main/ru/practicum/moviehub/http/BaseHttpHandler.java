package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BaseHttpHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final MoviesStore moviesStore;
    private int id;
    private int year;

    public BaseHttpHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();
        String[] splitStrings = path.split("/");

        if (splitStrings.length >= 3) {
            try {
                id = Integer.parseInt(splitStrings[2]);
                method += "_ID";
            } catch (NumberFormatException e) {
                ex.sendResponseHeaders(400, 0);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write("Некорректный ID".getBytes(StandardCharsets.UTF_8));
                }
                return;
            }
        }
        if (query != null && query.startsWith("year=")) {
            try {
                year = Integer.parseInt(query.split("=")[1]);
                method += "_YEAR";
            } catch (NumberFormatException e) {
                ex.sendResponseHeaders(400, 0);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write("Некорректный параметр запроса — 'year'".getBytes(StandardCharsets.UTF_8));
                }
                return;
            }
        }

        switch (method) {
            case "GET":
                handleGet(ex);
                break;
            case "POST":
                handlePost(ex);
                break;
            case "GET_ID":
                handleGetId(ex);
                break;
            case "DELETE_ID":
                handleDeleteId(ex);
                break;
            case "GET_YEAR":
                handleGetYear(ex);
                break;
            default:
                ex.sendResponseHeaders(405, 0);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write("Method Not Allowed".getBytes(StandardCharsets.UTF_8));
                }
                break;
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        List<Movie> movies = List.copyOf(moviesStore.getMovies().values());
        String responseJson = gson.toJson(movies);

        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(200, 0);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(responseJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.equals("application/json; charset=UTF-8")) {
            ex.sendResponseHeaders(415, 0);
            try (OutputStream os = ex.getResponseBody()) {
                os.write("Unsupported Media Type".getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        String body;
        try (InputStream is = ex.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        Movie movie;
        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (JsonSyntaxException exception) {
            ex.sendResponseHeaders(422, 0);
            try (OutputStream os = ex.getResponseBody()) {
                os.write("Unprocessable Entity".getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        if (movie.getTitle() == null || movie.getTitle().isBlank() || movie.getTitle().length() > 100) {
            sendValidationError(ex, "Неверное название фильма");
            return;
        }

        int currentYear = LocalDate.now().getYear();
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            sendValidationError(ex, "Неверный год фильма");
            return;
        }

        moviesStore.addMovie(movie);

        String responseJson = gson.toJson(movie);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(201, 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(responseJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleGetId(HttpExchange ex) throws IOException {
        if (!moviesStore.getMovies().containsKey(id)) {
            ex.sendResponseHeaders(404, 0);
            try (OutputStream os = ex.getResponseBody()) {
                os.write("Фильм не найден".getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        String responseMovie = gson.toJson(moviesStore.getMovie(id));
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(200, 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(responseMovie.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleDeleteId(HttpExchange ex) throws IOException {
        if (!moviesStore.getMovies().containsKey(id)) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        moviesStore.getMovies().remove(id);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(200, -1);
    }

    private void handleGetYear(HttpExchange ex) throws IOException {
        List<Movie> movies = new ArrayList<>();

        for (Movie movie : moviesStore.getMovies().values()) {
            if (movie.getYear() == year) {
                movies.add(movie);
            }
        }

        if (movies.isEmpty()) {
            ex.sendResponseHeaders(400, 0);
            try (OutputStream os = ex.getResponseBody()) {
                os.write("Фильм с данным годом отсутствует в каталоге".getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        String response = gson.toJson(movies);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(200, 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendValidationError(HttpExchange ex, String errorMessage) throws IOException {
        String errorResponse = gson.toJson(new ErrorResponse(errorMessage));
        ex.sendResponseHeaders(422, 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
        }
    }
}
