package ru.practicum.moviehub.model;

public class Movie {
    @NotNull
    private Integer id;

    @NotNull
    private String title;

    private int year;

    public Movie(Integer id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String toJson() {
        return String.format("{\"id\":%d,\"title\":\"%s\",\"year\":%d}", id, title, year);
    }
}