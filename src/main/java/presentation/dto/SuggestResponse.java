package presentation.dto;

import java.util.List;

/**
 * DTO de respuesta para el endpoint de autocompletado/sugerencias.
 */
public record SuggestResponse(
    String query,
    List<String> suggestions,
    int total
) {}
