package com.application.arcagambling.service;

import com.application.arcagambling.domain.Thermo;
import com.application.arcagambling.dto.request.AddThermoValueDto;
import com.application.arcagambling.dto.request.RequestThermoStatsDto;
import com.application.arcagambling.repository.ThermoRepository;
import com.application.arcagambling.repository.TreeRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.json.JSONObject;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ThermoService {

    private final ThermoRepository thermoRepository;
    private final TreeRepository treeRepository;
    private final UrlParseService urlParseService;
    private final ConcurrentMapCacheManager cacheManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Scheduled(cron = "0 * * * * *")
    public void scheduled() {
        Future<?> future = executor.submit(() -> {
            try {
                parsing();
            } catch (Exception e) {
                System.out.println("작업 도중 예외 발생: " + e.getMessage());
            }
        });

        try {
            future.get(59, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // interrupt
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parsing() {
        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "https://rumadev-arcaparsing.hf.space/thermo";
        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            add(response);
            cacheClear();
            Pageable pageable = PageRequest.of(0, 20, Sort.by("rounds").descending());
            List<Integer> recentDegree = thermoRepository.findAll(pageable)
                    .getContent()
                    .stream()
                    .map(Thermo::getDegree)
                    .collect(Collectors.toList());
            Collections.reverse(recentDegree);

            int nextDegree = urlParseService.parse(recentDegree, 0, 100);

            String text = nextDegree + " / " + LocalDateTime.now();
            String fileName = "recentThermo.txt";
            FileWriter fw = new FileWriter(fileName);

            try {
                fw.write(text);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fw.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cacheClear() {
        Cache findSlot = cacheManager.getCache("thermoFindSlot");
        Cache findOoe = cacheManager.getCache("thermoFindOoe");
        Cache recent = cacheManager.getCache("thermoRecent");
        Cache list = cacheManager.getCache("thermoList");
        Cache paging = cacheManager.getCache("thermoPaging");

        if (findSlot != null) {
            findSlot.clear();
        }
        if (findOoe != null) {
            findOoe.clear();
        }
        if (recent != null) {
            recent.clear();
        }
        if (list != null) {
            list.clear();
        }
        if (paging != null) {
            paging.clear();
        }
    }

    public void add(String datas) throws IOException {

        JSONObject jsonObject = new JSONObject(datas);
        Long rounds = Long.parseLong(jsonObject.get("turn").toString()); // 라운드
        int v = Integer.parseInt(jsonObject.get("v").toString()); // 온도
        String ooe = "";
        if (v == 0 || v == 100) {
            ooe = "폭"; // 홀짝
        } else {
            if (v % 2 == 1) {
                ooe = "홀"; // 홀짝
            } else {
                ooe = "짝"; // 홀짝
            }
        }
        Thermo thermo = new Thermo(rounds, v, ooe);
        AddThermoValueDto dto = new AddThermoValueDto();
        thermoRepository.save(dto.toEntity(thermo));
    }

    @Cacheable(value = "thermoFindSlot", key = "{#min, #max, #select}")
    public RequestThermoStatsDto findSlot(int min, int max, Long select) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(select), Sort.by("rounds").descending());
        List<Thermo> thermos = new ArrayList<>(thermoRepository.findAll(pa).stream().map(Thermo::of).toList());
        Collections.reverse(thermos);
        long all = thermos.size();
        long find = 0;
        long notFound = 0;
        for (Thermo thermo : thermos) {
            notFound++;
            if (thermo.getDegree() >= min && thermo.getDegree() <= max) {
                find++;
                notFound = 0L;
            }
        }
        if (thermos.isEmpty()) {
            return RequestThermoStatsDto.of(0L, "0.00", 0L);
        }
        String percent = String.format("%.2f", ((double) 100 / all) * find);
        return RequestThermoStatsDto.of(find, percent, notFound);
    }

    @Cacheable(value = "thermoFindOoe", key = "{#text, #select}")
    public RequestThermoStatsDto findOoe(String text, Long select) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(select), Sort.by("rounds").descending());
        List<Thermo> thermos = new ArrayList<>(thermoRepository.findAll(pa).stream().map(Thermo::of).toList());
        Collections.reverse(thermos);
        Long all = Long.valueOf(thermos.size());
        Long find = 0L;
        Long notFound = 0L;
        for (Thermo thermo : thermos) {
            notFound++;
            if (thermo.getOoe().equals(text)) {
                find++;
                notFound = 0L;
            }
        }
        if (thermos.isEmpty() || thermos == null || thermos.size() == 0) {
            return RequestThermoStatsDto.of(0L, "0.00", 0L);
        }
        String percent = String.format("%.2f", ((double) 100 / all) * find);
        return RequestThermoStatsDto.of(find, percent, notFound);
    }

    @Cacheable(value = "thermoList", key = "{0, #number}")
    public Page<Thermo> list(Long number) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(number), Sort.by("rounds").descending());
        return thermoRepository.findAll(pa);
    }

    @Cacheable(value = "thermoPaging", key = "{0, #number}")
    public Long paging(Long number) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(number), Sort.by("rounds").descending());
        return Long.parseLong(String.valueOf(thermoRepository.findAll(pa).getTotalPages())) - 1;
    }

    @Cacheable(value = "thermoList", key = "{#page, #number}")
    public Page<Thermo> list(Long number, Long page) {
        PageRequest pa = PageRequest.of(Math.toIntExact(page), Math.toIntExact(number), Sort.by("rounds").descending());
        return thermoRepository.findAll(pa);
    }

    @Cacheable(value = "thermoPaging", key = "{#page, #number}")
    public Long paging(Long number, Long page) {
        PageRequest pa = PageRequest.of(Math.toIntExact(page), Math.toIntExact(number), Sort.by("rounds").descending());
        return Long.parseLong(String.valueOf(thermoRepository.findAll(pa).getTotalPages())) - 1;
    }

    public Map<String, Object> getPredictDegree() throws IOException {
        File file = new File("recentThermo.txt");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        List<String> content = List.of(br.readLine().split(" / "));
        Map<String, Object> predict = new HashMap<>();
        predict.put("number", content.get(0));
        predict.put("time", LocalDateTime.parse(content.get(1)));
        return predict;
    }

    @Cacheable(value = "thermoRecent")
    public Thermo getRecent() {
        PageRequest pa = PageRequest.of(0, 1, Sort.by("rounds").descending());
        Page<Thermo> thermo = thermoRepository.findAll(pa);
        return thermo.getContent().isEmpty() ? null : thermo.getContent().get(0);
    }
}
