package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private final String error;
    private final List<String> validationDetails = List.of(
            "название не должно быть пустым",
            "год должен быть между 1888 и текущим годом",
            "размер названия должен быть меньше 100 символов"
    );

    public ErrorResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public List<String> getValidationDetails() {
        return validationDetails;
    }
}