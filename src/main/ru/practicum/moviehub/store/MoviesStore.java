package ru.practicum.moviehub.store;
import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MoviesStore {

    private static final MoviesStore instance = new MoviesStore();
    private final Map<Integer, Movie> movies = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> getById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public static MoviesStore getInstance() {
        return instance;
    }

    public Movie add(Movie movie) {
        int id = idGenerator.incrementAndGet();
        movie.setId(id);
        movies.put(id, movie);
        return movie;
    }

    public boolean remove(int id) {
        return movies.remove(id) != null;
    }

    public List<Movie> getByYear(int year) {
        return movies.values().stream().filter(m -> m.getYear() == year).collect(Collectors.toList());
    }

    public boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        idGenerator.set(0);
    }
}