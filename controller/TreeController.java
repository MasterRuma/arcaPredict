package com.application.arcagambling.controller;

import com.application.arcagambling.domain.Tree;
import com.application.arcagambling.service.TreeService;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
@RequestMapping("/tree")
public class TreeController {

    private final TreeService treeService;

    @GetMapping("/predict")
    public String predict(Model model) throws IOException {
        model.addAttribute("treeDto", treeService.getPredictNumber());
        model.addAttribute("recent", treeService.getRecent());
        return "tree/predict";
    }

    @GetMapping("/stats")
    public String stats(@RequestParam("select") Long select, Model model) {

        if (select > 1000000) select = 1000000L;

        // 슬롯
        model.addAttribute("slot1", treeService.findSlot(1, select));
        model.addAttribute("slot2", treeService.findSlot(2, select));
        model.addAttribute("slot3", treeService.findSlot(3, select));
        model.addAttribute("slot4", treeService.findSlot(4, select));
        model.addAttribute("slot5", treeService.findSlot(5, select));
        model.addAttribute("slot6", treeService.findSlot(6, select));

        // 군집
        model.addAttribute("armyL", treeService.findArmy("1~3", select));
        model.addAttribute("armyR", treeService.findArmy("4~6", select));

        // 홀짝
        model.addAttribute("ooeO", treeService.findOoe("홀", select));
        model.addAttribute("ooeE", treeService.findOoe("짝", select));

        // 처음 방향
        model.addAttribute("lorL", treeService.findLor("좌", select));
        model.addAttribute("lorR", treeService.findLor("우", select));
        return "tree/stats";
    }

    @GetMapping("/list")
    public String list(Model model) {
        return "tree/list";
    }

    @PostMapping("/list")
    @ResponseBody
    public Map<String, Object> list(@RequestParam("number") Long number) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", treeService.list(number));
        result.put("currentPage", 1);
        result.put("page", treeService.paging(number));
        return result;
    }

    @PostMapping("/listWithPage")
    @ResponseBody
    public Map<String, Object> list(@RequestParam("number") Long number, @RequestParam("page") Long page) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", treeService.list(number, page));
        result.put("currentPage", page);
        result.put("page", treeService.paging(number, page));
        return result;
    }

    @GetMapping("/downloadCSV")
    public void downloadCSV(@RequestParam("number") Long number, @RequestParam("page") Long page, HttpServletResponse response) throws IOException {
        String fileName = String.format("tree-%s-%s.csv", page, number);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        try (PrintWriter writer = response.getWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // CSV의 첫 번째 줄(Header) 작성
            csvWriter.writeNext(new String[]{"라운드", "숫자", "군집", "홀짝", "첫 방향"});
            List<Tree> dataList = treeService.list(number, page).toList();

            String[] total = new String[dataList.size()];

            for (Tree tree : dataList) {
                String[] row = {
                        String.valueOf(tree.getRounds()),
                        String.valueOf(tree.getNumber()),
                        String.valueOf(tree.getArmy()),
                        String.valueOf(tree.getOoe()),
                        String.valueOf(tree.getLor())
                };
                csvWriter.writeNext(row);
            }
        }
    }


}
