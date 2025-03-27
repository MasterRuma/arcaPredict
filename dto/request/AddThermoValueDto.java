package com.application.arcagambling.dto.request;

import com.application.arcagambling.domain.Thermo;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AddThermoValueDto {

    private Long rounds;
    private Integer degree;
    private String ooe;

    @Builder
    public Thermo toEntity(Thermo thermo) {
        return Thermo.builder()
                .rounds(thermo.getRounds())
                .degree(thermo.getDegree())
                .ooe(thermo.getOoe())
                .build();
    }

}
