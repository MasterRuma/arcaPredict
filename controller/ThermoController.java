package com.application.arcagambling.controller;

import com.application.arcagambling.domain.Thermo;
import com.application.arcagambling.service.ThermoService;
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
@RequestMapping("/thermo")
public class ThermoController {
    private final ThermoService thermoService;
    private final TreeService treeService;

    @GetMapping("/predict")
    public String predict(Model model) throws IOException {
        model.addAttribute("thermoDto", thermoService.getPredictDegree());
        model.addAttribute("recent", thermoService.getRecent());
        return "thermo/predict";
    }

    @GetMapping("/stats")
    public String stats(@RequestParam("select") Long select, Model model) {

        if (select > 1000000) select = 1000000L;

        // 슬롯
        model.addAttribute("slot1", thermoService.findSlot(0, 0, select));
        model.addAttribute("slot2", thermoService.findSlot(1, 24, select));
        model.addAttribute("slot3", thermoService.findSlot(25, 36, select));
        model.addAttribute("slot4", thermoService.findSlot(37, 49, select));
        model.addAttribute("slot5", thermoService.findSlot(50, 50, select));
        model.addAttribute("slot6", thermoService.findSlot(51, 62, select));
        model.addAttribute("slot7", thermoService.findSlot(63, 74, select));
        model.addAttribute("slot8", thermoService.findSlot(75, 99, select));
        model.addAttribute("slot9", thermoService.findSlot(100, 100, select));

        // 홀짝
        model.addAttribute("ooeO", thermoService.findOoe("홀", select));
        model.addAttribute("ooeE", thermoService.findOoe("짝", select));
        model.addAttribute("ooeX", thermoService.findOoe("폭", select));
        return "thermo/stats";
    }

    @GetMapping("/list")
    public String list(Model model) {
        return "thermo/list";
    }

    @PostMapping("/list")
    @ResponseBody
    public Map<String, Object> list(@RequestParam("number") Long number) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", thermoService.list(number));
        result.put("currentPage", 1);
        result.put("page", thermoService.paging(number));
        return result;
    }

    @PostMapping("/listWithPage")
    @ResponseBody
    public Map<String, Object> list(@RequestParam("number") Long number, @RequestParam("page") Long page) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", thermoService.list(number, page));
        result.put("currentPage", page);
        result.put("page", thermoService.paging(number, page));
        return result;
    }

    @GetMapping("/downloadCSV")
    public void downloadCSV(@RequestParam("number") Long number, @RequestParam("page") Long page, HttpServletResponse response) throws IOException {
        String fileName = String.format("thermo-%s-%s.csv", page, number);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        try (PrintWriter writer = response.getWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // CSV의 첫 번째 줄(Header) 작성
            csvWriter.writeNext(new String[]{"라운드", "온도", "홀짝"});
            List<Thermo> dataList = thermoService.list(number, page).toList();

            String[] total = new String[dataList.size()];

            for (Thermo thermo : dataList) {
                String[] row = {
                        String.valueOf(thermo.getRounds()),
                        String.valueOf(thermo.getDegree()),
                        String.valueOf(thermo.getOoe())
                };
                csvWriter.writeNext(row);
            }
        }
    }
}
