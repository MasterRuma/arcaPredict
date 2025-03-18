package com.application.arcagambling.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Thermo {

    @Id
    private Long rounds;
    private Integer degree;
    private String ooe;

    public static Thermo of(Thermo thermo) {
        return Thermo.builder()
                .rounds(thermo.getRounds())
                .degree(thermo.getDegree())
                .ooe(thermo.getOoe())
                .build();
    }

}
