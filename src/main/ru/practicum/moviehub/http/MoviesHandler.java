package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        try {
            if (method.equals("GET")) {
                if (path.equals("/movies")) {
                    handleGetAll(exchange, query);
                } else if (path.startsWith("/movies/")) {
                    handleGetById(exchange, path);
                }
            } else if (method.equals("POST") && path.equals("/movies")) {
                handlePost(exchange);
            } else if (method.equals("DELETE") && path.startsWith("/movies/")) {
                handleDelete(exchange, path);
            } else {
                sendResponse(exchange, 405, "Метод не поддерживается");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "Ошибка сервера");
        } finally {
            exchange.close();
        }
    }

    private void handleGetAll(HttpExchange exchange, String query) throws IOException {
        if (query != null && query.contains("year=")) {
            try {
                int year = Integer.parseInt(query.split("year=")[1].split("&")[0]);
                sendJsonResponse(exchange, 200, store.getByYear(year));
            } catch (Exception e) {
                sendResponse(exchange, 400, "Некорректный параметр запроса — 'year'");
            }
        } else {
            sendJsonResponse(exchange, 200, store.getAll());
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            sendResponse(exchange, 415, "Неподдерживаемый тип");
            return;
        }

        String body = readText(exchange);
        try {
            Movie movie = gson.fromJson(body, Movie.class);
            List<String> errors = validateMovie(movie);

            if (!errors.isEmpty()) {
                sendJsonResponse(exchange, 422, new ErrorResponse("Ошибка валидации", errors));
                return;
            }

            Movie created = store.add(movie);
            sendJsonResponse(exchange, 201, created);

        } catch (com.google.gson.JsonSyntaxException e) {
            List<String> errors = List.of("Некорректный синтаксис JSON: " + e.getMessage());
            sendJsonResponse(exchange, 422, new ErrorResponse("Ошибка валидации", errors));
        }
    }

    private void handleGetById(HttpExchange exchange, String path) throws IOException {
        String idPart = path.substring("/movies/".length());
        try {
            int id = Integer.parseInt(idPart);
            Optional<Movie> movie = store.getById(id);
            if (movie.isPresent()) {
                sendJsonResponse(exchange, 200, movie.get());
            } else {
                sendResponse(exchange, 404, "Фильм не найден");
            }
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "Некорректный ID");
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String idPart = path.substring("/movies/".length());
        try {
            int id = Integer.parseInt(idPart);
            if (store.delete(id)) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                sendResponse(exchange, 404, "Фильм не найден");
            }
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "Некорректный ID");
        }
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        if (movie == null) {
            errors.add("тело запроса не может быть пустым");
            return errors;
        }

        if (movie.getTitle() == null || movie.getTitle().isBlank() || movie.getTitle().length() > 100) {
            errors.add("название не должно быть пустым, длина ≤ 100 символов");
        }

        int currentYear = Year.now().getValue();
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между 1888 и " + (currentYear + 1));
        }
        return errors;
    }
}