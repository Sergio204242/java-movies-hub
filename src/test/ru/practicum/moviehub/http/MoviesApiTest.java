package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoviesApiTest {
    private static final Gson gson = new Gson();
    private static MoviesServer moviesServer;
    private static HttpClient client;
    private static MoviesStore moviesStore;
    private static URI uri;
    private static HttpResponse.BodyHandler<String> handler;

    @BeforeAll
    static void startServer() throws IOException {
        moviesStore = new MoviesStore();
        moviesServer = new MoviesServer(moviesStore, 8080);
        moviesServer.start();
        client = HttpClient.newHttpClient();
        uri = URI.create("http://localhost:8080/movies");
        handler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    }

    @AfterAll
    static void stopServer() {
        moviesServer.stop();
    }

    @BeforeEach
    void clearStore() {
        moviesStore.getMovies().clear();
        uri = URI.create("http://localhost:8080/movies");

    }

    @Test
    public void getMovies() throws Exception {
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        Movie movie2 = new Movie(2, "1+1", 2011);
        Movie movie3 = new Movie(3, "Побег из Шоушенга", 1994);
        moviesStore.addMovie(movie1);
        moviesStore.addMovie(movie2);
        moviesStore.addMovie(movie3);
        List<Movie> expected = new ArrayList<>(moviesStore.getMovies().values());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        List<Movie> moviesFromJson = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertEquals(expected, moviesFromJson, "GET /movies должен вернуть список фильмов");
    }

    @Test
    public void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertEquals("[]", resp.body(), "GET /movies должен вернуть []");
    }

    @Test
    public void postMovieWithCorrectData() throws IOException, InterruptedException {
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        String jsonMovie = gson.toJson(movie1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        Movie movieFromJson = gson.fromJson(resp.body(), Movie.class);
        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertEquals(movie1, movieFromJson, "POST /movies должен вернуть фильм Интерстеллар");
    }

    @Test
    public void postMovieWithEmptyTitle() throws IOException, InterruptedException {
        Movie movie1 = new Movie(1, "", 2014);
        String jsonMovie = gson.toJson(movie1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        ErrorResponse errorResponseFromJson = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals("Неверное название фильма", errorResponseFromJson.getError(),
                "POST /movies должен вернуть ошибку - \"Неверное название фильма\"");
    }

    @Test
    public void postMovieWithTitle101() throws IOException, InterruptedException {
        String title = "a".repeat(101);
        Movie movie1 = new Movie(1, title, 2014);
        String jsonMovie = gson.toJson(movie1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        ErrorResponse errorResponseFromJson = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals("Неверное название фильма", errorResponseFromJson.getError(),
                "POST /movies должен вернуть ошибку - \"Неверное название фильма\"");
    }

    @Test
    public void postMovieWithYear1887() throws IOException, InterruptedException {
        Movie movie1 = new Movie(1, "Интерстеллар", 1887);
        String jsonMovie = gson.toJson(movie1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        ErrorResponse errorResponseFromJson = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals("Неверный год фильма", errorResponseFromJson.getError(),
                "POST /movies должен вернуть ошибку - \"Неверный год фильма\"");
    }

    @Test
    public void postMovieWithYear2028() throws IOException, InterruptedException {
        Movie movie1 = new Movie(1, "Интерстеллар", 2028);
        String jsonMovie = gson.toJson(movie1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        ErrorResponse errorResponseFromJson = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals("Неверный год фильма", errorResponseFromJson.getError(),
                "POST /movies должен вернуть ошибку - \"Неверный год фильма\"");
    }

    @Test
    public void postWithBadContentType() throws IOException, InterruptedException {
        Movie movie1 = new Movie(1, "Интерстеллар", 2004);

        String jsonMovie = gson.toJson(movie1);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");
        assertEquals("Unsupported Media Type", resp.body(),
                "POST /movies должен вернуть ошибку - \"Unsupported Media Type\"");
    }

    @Test
    public void postWithJsonSyntaxException() throws IOException, InterruptedException {
        String jsonMovie = gson.toJson(Math.PI);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertEquals("Unprocessable Entity", resp.body(),
                "POST /movies должен вернуть ошибку - \"Unprocessable Entity\"");
    }

    @Test
    public void getMovieById() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies/1");
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        moviesStore.addMovie(movie1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        Movie movieFromJson = gson.fromJson(resp.body(), Movie.class);
        assertEquals(200, resp.statusCode(), "GET /movies/id должен вернуть 200");
        assertEquals(movie1, movieFromJson, "GET /movies/id должен вернуть фильм с id = 1");
    }

    @Test
    public void getNonExistentMovieById() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies/2");
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        Movie movie2 = new Movie(3, "1+1", 2011);
        Movie movie3 = new Movie(4, "Побег из Шоушенга", 1994);
        moviesStore.addMovie(movie1);
        moviesStore.addMovie(movie2);
        moviesStore.addMovie(movie3);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(404, resp.statusCode(), "GET /movies/id должен вернуть 404");
        assertEquals("Фильм не найден", resp.body(),
                "GET /movies/id должен вернуть ошибку - \"Фильм не найден\"");
    }

    @Test
    public void getMovieByIdWithNumberFormatException() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies/fdaf4");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(400, resp.statusCode(), "GET /movies/fdaf4 должен вернуть 400");
        assertEquals("Некорректный ID", resp.body(),
                "GET /movies/fdaf4 должен вернуть ошибку - \"Некорректный ID\"");
    }

    @Test
    public void deleteMovieById() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies/2");
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        Movie movie2 = new Movie(2, "1+1", 2011);
        Movie movie3 = new Movie(3, "Побег из Шоушенга", 1994);
        moviesStore.addMovie(movie1);
        moviesStore.addMovie(movie2);
        moviesStore.addMovie(movie3);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(200, resp.statusCode(), "DELETE /movies/2 должен вернуть 200");
    }

    @Test
    public void deleteNonExistentMovieById() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies/2");
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        Movie movie2 = new Movie(3, "1+1", 2011);
        Movie movie3 = new Movie(4, "Побег из Шоушенга", 1994);
        moviesStore.addMovie(movie1);
        moviesStore.addMovie(movie2);
        moviesStore.addMovie(movie3);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(204, resp.statusCode(), "DELETE /movies/2 должен вернуть 204");
    }

    @Test
    public void deleteMovieByBadId() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies/fda");
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        Movie movie2 = new Movie(3, "1+1", 2011);
        Movie movie3 = new Movie(4, "Побег из Шоушенга", 1994);
        moviesStore.addMovie(movie1);
        moviesStore.addMovie(movie2);
        moviesStore.addMovie(movie3);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(400, resp.statusCode(), "DELETE /movies/fda должен вернуть 400");
        assertEquals("Некорректный ID", resp.body(),
                "GET /movies/fda должен вернуть ошибку - \"Некорректный ID\"");
    }

    @Test
    public void getMovieByYear2014() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies?year=2014");
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        Movie movie2 = new Movie(3, "1+1", 2011);
        Movie movie3 = new Movie(4, "Побег из Шоушенга", 2014);
        moviesStore.addMovie(movie1);
        moviesStore.addMovie(movie2);
        moviesStore.addMovie(movie3);
        List<Movie> expected = new ArrayList<>();
        expected.add(movie1);
        expected.add(movie3);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        List<Movie> moviesFromJson = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(200, resp.statusCode(), "GET /movies?year=2014 должен вернуть 200");
        assertEquals(expected, moviesFromJson,
                "GET /movies?year=2014 должен вернуть список фильмов 2014 года");
    }

    @Test
    public void getMovieByYear2012() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies?year=2012");
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        Movie movie2 = new Movie(3, "1+1", 2011);
        Movie movie3 = new Movie(4, "Побег из Шоушенга", 2014);
        moviesStore.addMovie(movie1);
        moviesStore.addMovie(movie2);
        moviesStore.addMovie(movie3);


        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(400, resp.statusCode(), "GET /movies?year=2012 должен вернуть 400");
        assertEquals("Фильм с данным годом отсутствует в каталоге", resp.body(),
                "GET /movies?year=2012 должен вернуть пустой список");
    }

    @Test
    public void getMovieByBadYear() throws IOException, InterruptedException {
        uri = URI.create("http://localhost:8080/movies?year=yjt");
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        Movie movie2 = new Movie(3, "1+1", 2011);
        Movie movie3 = new Movie(4, "Побег из Шоушенга", 2014);
        moviesStore.addMovie(movie1);
        moviesStore.addMovie(movie2);
        moviesStore.addMovie(movie3);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(400, resp.statusCode(), "GET /movies?year=yjt должен вернуть 400");
        assertEquals("Некорректный параметр запроса — 'year'", resp.body(),
                "GET /movies?year=yjt должен вернуть ошибку - \"Некорректный параметр запроса — 'year'\"");
    }

    @Test
    public void getMovieByBadMethod() throws IOException, InterruptedException {
        Movie movie1 = new Movie(1, "Интерстеллар", 2014);
        String jsonMovie = gson.toJson(movie1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonMovie, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, handler);
        assertEquals(405, resp.statusCode(), "PUT /movies должен вернуть 405");
        assertEquals("Method Not Allowed", resp.body(),
                "PUT /movies должен вернуть ошибку - \"Method Not Allowed\"");
    }

}