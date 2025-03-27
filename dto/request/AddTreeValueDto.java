package com.application.arcagambling.dto.request;

import com.application.arcagambling.domain.Tree;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AddTreeValueDto {

    private Long rounds;
    private Integer number;
    private String army;
    private String ooe;
    private String lor;

    @Builder
    public Tree toEntity(Tree tree) {
        return Tree.builder()
                .rounds(tree.getRounds())
                .number(tree.getNumber())
                .army(tree.getArmy())
                .ooe(tree.getOoe())
                .lor(tree.getLor())
                .build();
    }

}
