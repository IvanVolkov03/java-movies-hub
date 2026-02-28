package ru.practicum.moviehub.http;

import com.google.gson.Gson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// Здесь 20 тестов по каждому из пунктов ТЗ!

class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080/movies";
    private static HttpClient client;
    private final Gson gson = new Gson();
    private static MoviesServer server;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(MoviesStore.getInstance(), 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        MoviesStore.getInstance().clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    // GET /movies

    @Test
    @DisplayName("1. GET /movies: пустой список")
    void getMovies_empty_returnsEmptyList() throws Exception {
        HttpResponse<String> response = sendGet(BASE_URL);
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    @DisplayName("2. GET /movies: список с данными")
    void getMovies_withData_returnsList() throws Exception {
        MoviesStore.getInstance().add(new Movie(678, "Титаник", 2010));
        HttpResponse<String> response = sendGet(BASE_URL);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Титаник"));
    }

    // POST /movies

    @Test
    @DisplayName("3. POST /movies: корректные данные")
    void postMovie_valid_returnsOk() throws Exception {
        String json = "{\"id\": 1, \"title\": \"Начало\", \"year\": 1999}";
        HttpResponse<String> response = sendPost(BASE_URL, json);
        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("4. POST /movies: ошибка пустой title")
    void postMovie_emptyTitle_returnsError() throws Exception {
        String json = "{\"title\": \"\", \"year\": 2010}";
        HttpResponse<String> response = sendPost(BASE_URL, json);
        assertEquals(422, response.statusCode());
    }

    @Test
    @DisplayName("5. POST /movies: ошибка длинный title (>100)")
    void postMovie_longTitle_returnsError() throws Exception {
        String longTitle = "A".repeat(101);
        String json = "{\"title\": \"" + longTitle + "\", \"year\": 2010}";
        HttpResponse<String> response = sendPost(BASE_URL, json);
        assertEquals(422, response.statusCode());
    }

    @Test
    @DisplayName("6. POST /movies: ошибка год < 1888")
    void postMovie_yearTooOld_returnsError() throws Exception {
        String json = "{\"title\": \"Old\", \"year\": 1800}";
        HttpResponse<String> response = sendPost(BASE_URL, json);
        assertEquals(422, response.statusCode());
    }

    @Test
    @DisplayName("7. POST /movies: ошибка год из будущего")
    void postMovie_futureYear_returnsError() throws Exception {
        int year = LocalDate.now().getYear() + 2;
        String json = "{\"title\": \"Future\", \"year\": " + year + "}";
        HttpResponse<String> response = sendPost(BASE_URL, json);
        assertEquals(422, response.statusCode());
    }

    @Test
    @DisplayName("8. POST /movies: ошибка неверный Content-Type")
    void postMovie_wrongContentType_returnsError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(415, response.statusCode());
    }

    @Test
    @DisplayName("9. POST /movies: ошибка кривой JSON")
    void postMovie_badJson_returnsError() throws Exception {
        String json = "{\"title\": \"Fail\", \"year\": ";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("Ошибка валидации"));
    }

    // GET /movies/{id}

    @Test
    @DisplayName("10. GET /movies/{id}: поиск существующего")
    void getMovie_byId_returnsMovie() throws Exception {
        Movie m = MoviesStore.getInstance().add(new Movie(789, "Аватар", 2009));
        HttpResponse<String> response = sendGet(BASE_URL + "/" + m.getId());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Аватар"));
    }

    @Test
    @DisplayName("11. GET /movies/{id}: 404 если нет фильма")
    void getMovie_notFound_returns404() throws Exception {
        HttpResponse<String> response = sendGet(BASE_URL + "/9999");
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("12. GET /movies/{id}: ошибка если ID не число")
    void getMovie_badId_returnsError() throws Exception {
        HttpResponse<String> response = sendGet(BASE_URL + "/not-a-number");
        assertEquals(400, response.statusCode());
    }

    // DELETE /movies/{id}

    @Test
    @DisplayName("13. DELETE /movies/{id}: успешное удаление")
    void deleteMovie_valid_returnsOk() throws Exception {
        Movie m = MoviesStore.getInstance().add(new Movie(123, "Delete Me", 2020));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + m.getId()))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode());
    }

    @Test
    @DisplayName("14. DELETE /movies/{id}: 404 если не найден")
    void deleteMovie_notFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/8888"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("15. DELETE /movies/{id}: ошибка если ID не число")
    void deleteMovie_badId_returnsError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/abc"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    // GET /movies?year=YYYY

    @Test
    @DisplayName("16. GET /movies?year=YYYY: фильтрация года")
    void getMovies_filterYear_returnsMatches() throws Exception {
        MoviesStore.getInstance().add(new Movie(234, "2015 Movie", 2015));
        MoviesStore.getInstance().add(new Movie(345, "2020 Movie", 2020));
        HttpResponse<String> response = sendGet(BASE_URL + "?year=2015");
        assertTrue(response.body().contains("2015 Movie"));
        assertFalse(response.body().contains("2020 Movie"));
    }

    @Test
    @DisplayName("17. GET /movies?year=YYYY: пустой список если нет совпадений")
    void getMovies_filterYear_empty_returnsEmpty() throws Exception {
        MoviesStore.getInstance().add(new Movie(456, "Any", 2015));
        HttpResponse<String> response = sendGet(BASE_URL + "?year=1990");
        assertEquals("[]", response.body());
    }

    @Test
    @DisplayName("18. GET /movies?year=YYYY: ошибка если год не число")
    void getMovies_filterYear_badFormat_returnsError() throws Exception {
        HttpResponse<String> response = sendGet(BASE_URL + "?year=year");
        assertEquals(400, response.statusCode());
    }

    // Общие

    @Test
    @DisplayName("19. Проверка заголовка Content-Type")
    void check_contentType() throws Exception {
        HttpResponse<String> response = sendGet(BASE_URL);
        String ct = response.headers().firstValue("Content-Type").orElse("");
        assertTrue(ct.contains("application/json"));
        assertTrue(ct.contains("charset=UTF-8"));
    }

    @Test
    @DisplayName("20. Ошибка 405 для неверного метода (PUT)")
    void check_methodNotAllowed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    private HttpResponse<String> sendGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String url, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}