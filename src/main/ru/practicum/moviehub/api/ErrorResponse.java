package ru.practicum.moviehub.api;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ErrorResponse {
    private final String error;
    private final List<String> details;

    public ErrorResponse(String error) {
        this(error, Collections.emptyList());
    }

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public String toJson() {
        if (details.isEmpty()) {
            return String.format("{\"error\":\"%s\"}", error);
        }
        String detailsJson = details.stream()
                .map(d -> "\"" + d + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        return String.format("{\"error\":\"%s\",\"details\":%s}", error, detailsJson);
    }
}