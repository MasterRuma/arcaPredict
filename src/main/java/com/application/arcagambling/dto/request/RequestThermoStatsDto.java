package com.application.arcagambling.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequestThermoStatsDto {

    private Long count;
    private String percent;
    private Long notFound;

    public static RequestThermoStatsDto of(Long count, String percent, Long notFound) {
        return RequestThermoStatsDto.builder()
                .count(count)
                .percent(percent)
                .notFound(notFound)
                .build();
    }

}
