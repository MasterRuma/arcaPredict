package com.application.arcagambling.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Tree {

    @Id
    private Long rounds;
    private Integer number;
    private String army;
    private String ooe;
    private String lor;

    public static Tree of(Tree tree) {
        return Tree.builder()
                .rounds(tree.getRounds())
                .number(tree.getNumber())
                .lor(tree.getLor())
                .army(tree.getArmy())
                .ooe(tree.getOoe())
                .build();
    }

}
