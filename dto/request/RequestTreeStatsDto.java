package com.application.arcagambling.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequestTreeStatsDto {

    private Long count;
    private String percent;
    private Long notFound;

    public static RequestTreeStatsDto of(Long count, String percent, Long notFound) {
        return RequestTreeStatsDto.builder()
                .count(count)
                .percent(percent)
                .notFound(notFound)
                .build();
    }

}
