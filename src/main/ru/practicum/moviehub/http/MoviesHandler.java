package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.Year;
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
            switch (method) {
                case "GET":
                    if (path.equals("/movies")) {
                        handleGetAll(exchange, query);
                    } else if (path.startsWith("/movies/")) {
                        handleGetById(exchange, path);
                    } else {
                        sendResponse(exchange, 404, "Путь не найден");
                    }
                    break;
                case "POST":
                    if (path.equals("/movies")) {
                        handlePost(exchange);
                    } else {
                        sendResponse(exchange, 404, "Путь не найден");
                    }
                    break;
                case "DELETE":
                    if (path.startsWith("/movies/")) {
                        handleDelete(exchange, path);
                    } else {
                        sendResponse(exchange, 404, "Путь не найден");
                    }
                    break;
                default:
                    sendResponse(exchange, 405, "Метод не поддерживается");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "Ошибка сервера");
        } finally {
            exchange.close();
        }
    }

    private void handleGetAll(HttpExchange exchange, String query) throws IOException {
        try {
            Object responseData = (query != null && query.contains("year="))
                    ? store.getByYear(Integer.parseInt(query.split("year=")[1].split("&")[0]))
                    : store.getAll();
            sendJsonResponse(exchange, 200, responseData);
        } catch (Exception e) {
            sendResponse(exchange, 400, "Некорректный параметр запроса — 'year'");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            sendResponse(exchange, 415, "Неподдерживаемый тип");
            return;
        }
        String body = readText(exchange);
        Movie movie;
        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (Exception e) {
            sendJsonResponse(exchange, 422, new ErrorResponse("Ошибка валидации", List.of("Некорректный синтаксис JSON")));
            return;
        }

        // Автоматическая проверка @NotNull (id и title)
        List<String> errors = validate(movie);

        // Дополнительные проверки
        if (movie != null) {
            if (movie.getTitle() != null && movie.getTitle().length() > 100) {
                errors.add("Поле 'title': длина > 100");
            }
            if (movie.getYear() != 0) {
                int currentYear = Year.now().getValue();
                if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
                    errors.add("Поле 'year': вне диапазона");
                }
            }
        }
        if (!errors.isEmpty()) {
            sendJsonResponse(exchange, 422, new ErrorResponse("Ошибка валидации", errors));
            return;
        }
        sendJsonResponse(exchange, 201, movie);
    }


    private void handleGetById(HttpExchange exchange, String path) throws IOException {
        try {
            int id = Integer.parseInt(path.substring("/movies/".length()));
            Optional<Movie> movie = store.getById(id);
            int status = movie.isPresent() ? 200 : 404;
            if (status == 200) {
                sendJsonResponse(exchange, status, movie.get());
            } else {
                sendResponse(exchange, status, "Фильм не найден");
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
}