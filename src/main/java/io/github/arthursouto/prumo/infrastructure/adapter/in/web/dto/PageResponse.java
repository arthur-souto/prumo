package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(description = "One page of results")
public record PageResponse<T>(
        List<T> items,
        @Schema(description = "Zero-based") int page,
        int size,
        long totalItems,
        int totalPages
) {

    public static <S, T> PageResponse<T> from(PageResult<S> result, Function<S, T> mapper) {
        return new PageResponse<>(
                result.items().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages()
        );
    }
}
