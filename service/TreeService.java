package com.application.arcagambling.service;

import com.application.arcagambling.domain.Tree;
import com.application.arcagambling.dto.request.AddTreeValueDto;
import com.application.arcagambling.dto.request.RequestTreeStatsDto;
import com.application.arcagambling.repository.TreeRepository;
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
public class TreeService {

    private final TreeRepository treeRepository;
    private final UrlParseService urlParseService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ConcurrentMapCacheManager cacheManager;

    @Scheduled(cron = "0 0/5 * * * *")
    public void scheduled() {
        Future<?> future = executor.submit(() -> {
            try {
                parsing();
            } catch (Exception e) {
                System.out.println("작업 도중 예외 발생: " + e.getMessage());
            }
        });

        try {
            future.get(299, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // interrupt
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parsing() throws IOException {

        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "https://rumadev-arcaparsing.hf.space/tree";
        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            add(response);
            cacheClear();

            Pageable pageable = PageRequest.of(0, 20, Sort.by("rounds").descending());
            List<Integer> recentNumber = treeRepository.findAll(pageable)
                    .getContent()
                    .stream()
                    .map(Tree::getNumber)
                    .collect(Collectors.toList());
            Collections.reverse(recentNumber);

            List<String> recentLor = treeRepository.findAll(pageable)
                    .getContent()
                    .stream()
                    .map(Tree::getLor)
                    .collect(Collectors.toList());
            Collections.reverse(recentLor);

            List<Integer> newRecentLor = recentLor
                    .stream()
                    .map(lor -> "좌".equals(lor) ? 1 : 2)
                    .collect(Collectors.toList());

            int nextNumber = urlParseService.parse(recentNumber, 1, 6);
            int nextLor = urlParseService.parse(newRecentLor, 1, 2);

            String text = nextNumber + " / " + nextLor + " / " + LocalDateTime.now();
            String fileName = "recentTree.txt";
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
        Cache findSlot = cacheManager.getCache("treeFindSlot");
        if (findSlot != null) {
            findSlot.clear();
        }

        Cache findOoe = cacheManager.getCache("treeFindOoe");
        if (findOoe != null) {
            findOoe.clear();
        }

        Cache findArmy = cacheManager.getCache("treeFindArmy");
        if (findArmy != null) {
            findArmy.clear();
        }

        Cache findLor = cacheManager.getCache("treeFindLor");
        if (findLor != null) {
            findLor.clear();
        }

        Cache recent = cacheManager.getCache("treeRecent");
        if (recent != null) {
            recent.clear();
        }

        Cache list = cacheManager.getCache("treeList");
        if (list != null) {
            list.clear();
        }

        Cache paging = cacheManager.getCache("treePaging");
        if (paging != null) {
            paging.clear();
        }
    }

    public void add(String datas) throws IOException {
        double init = 3.5;
        JSONObject jsonObject = new JSONObject(datas);
        Long rounds = Long.parseLong(jsonObject.get("turn").toString()); // 라운드
        List<Double> way = Arrays.stream(jsonObject.get("way").toString()
                        .replaceAll("[\\[\\]]", "")
                        .split("\\s*,\\s*"))
                .map(Double::parseDouble)
                .toList();
        for (int i = 0; i < way.size(); i++) {
            double temp = Double.parseDouble(String.valueOf(way.get(i))) / 2;
            init = init + temp;
        }

        String army = "";
        String ooe = "";
        String lor = "";

        int number = (int) init; // 계수
        if (number >= 1 && number <= 3) {
            army = "1~3"; // 군집
        } else if (number >= 4 && number <= 6) {
            army = "4~6"; // 군집
        }

        if (number % 2 == 0) {
            ooe = "짝"; // 홀짝
        } else {
            ooe = "홀"; // 홀짝
        }

        if (way.get(0) == 1) {
            lor = "우"; // 첫번째 이동
        } else if (way.get(0) == -1) {
            lor = "좌"; // 첫번째 이동
        }
        Tree tree = new Tree(rounds, number, army, ooe, lor);
        AddTreeValueDto addTreeValueDto = new AddTreeValueDto();
        treeRepository.save(addTreeValueDto.toEntity(tree));
    }

    @Cacheable(value = "treeFindSlot", key = "{#number, #select}")
    public RequestTreeStatsDto findSlot(int number, Long select) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(select), Sort.by("rounds").descending());
        List<Tree> trees = new ArrayList<>(treeRepository.findAll(pa).stream().map(Tree::of).toList());
        Collections.reverse(trees);
        Long all = Long.parseLong(String.valueOf(trees.size()));
        Long find = 0L;
        Long notFound = 0L;
        for (Tree tree : trees) {
            notFound++;
            if (tree.getNumber() == number) {
                find++;
                notFound = 0L;
            }
        }
        if (trees.isEmpty() || trees == null || trees.size() == 0) {
            return RequestTreeStatsDto.of(0L, "0.00", 0L);
        }
        String percent = String.format("%.2f", ((double) 100 / all) * find);
        return RequestTreeStatsDto.of(find, percent, notFound);
    }

    @Cacheable(value = "treeFindArmy", key = "{#value, #select}")
    public RequestTreeStatsDto findArmy(String value, Long select) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(select), Sort.by("rounds").descending());
        List<Tree> trees = new ArrayList<>(treeRepository.findAll(pa).stream().map(Tree::of).toList());
        Collections.reverse(trees);
        Long all = Long.valueOf(trees.size());
        Long find = 0L;
        Long notFound = 0L;
        for (Tree tree : trees) {
            notFound++;
            if (tree.getArmy().equals(value)) {
                find++;
                notFound = 0L;
            }
        }
        if (trees.isEmpty() || trees == null || trees.size() == 0) {
            return RequestTreeStatsDto.of(0L, "0.00", 0L);
        }
        String percent = String.format("%.2f", ((double) 100 / all) * find);
        return RequestTreeStatsDto.of(find, percent, notFound);
    }

    @Cacheable(value = "treeFindOoe", key = "{#value, #select}")
    public RequestTreeStatsDto findOoe(String value, Long select) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(select), Sort.by("rounds").descending());
        List<Tree> trees = new ArrayList<>(treeRepository.findAll(pa).stream().map(Tree::of).toList());
        Collections.reverse(trees);
        Long all = Long.valueOf(trees.size());
        Long find = 0L;
        Long notFound = 0L;
        for (Tree tree : trees) {
            notFound++;
            if (tree.getOoe().equals(value)) {
                find++;
                notFound = 0L;
            }
        }
        if (trees.isEmpty() || trees == null || trees.size() == 0) {
            return RequestTreeStatsDto.of(0L, "0.00", 0L);
        }
        String percent = String.format("%.2f", ((double) 100 / all) * find);
        return RequestTreeStatsDto.of(find, percent, notFound);
    }

    @Cacheable(value = "treeFindLor", key = "{#value, #select}")
    public RequestTreeStatsDto findLor(String value, Long select) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(select), Sort.by("rounds").descending());
        List<Tree> trees = new ArrayList<>(treeRepository.findAll(pa).stream().map(Tree::of).toList());
        Collections.reverse(trees);
        Long all = Long.valueOf(trees.size());
        Long find = 0L;
        Long notFound = 0L;
        for (Tree tree : trees) {
            notFound++;
            if (tree.getLor().equals(value)) {
                find++;
                notFound = 0L;
            }
        }
        if (trees.isEmpty() || trees == null || trees.size() == 0) {
            return RequestTreeStatsDto.of(0L, "0.00", 0L);
        }
        String percent = String.format("%.2f", ((double) 100 / all) * find);
        return RequestTreeStatsDto.of(find, percent, notFound);
    }

    @Cacheable(value = "treeList", key = "{0, #number}")
    public Page<Tree> list(Long number) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(number), Sort.by("rounds").descending());
        return treeRepository.findAll(pa);
    }

    @Cacheable(value = "treePaging", key = "{0, #number}")
    public Long paging(Long number) {
        PageRequest pa = PageRequest.of(0, Math.toIntExact(number), Sort.by("rounds").descending());
        return Long.parseLong(String.valueOf(treeRepository.findAll(pa).getTotalPages())) - 1;
    }

    @Cacheable(value = "treeList", key = "{#page, #number}")
    public Page<Tree> list(Long number, Long page) {
        PageRequest pa = PageRequest.of(Math.toIntExact(page), Math.toIntExact(number), Sort.by("rounds").descending());
        return treeRepository.findAll(pa);
    }

    @Cacheable(value = "treePaging", key = "{#page, #number}")
    public Long paging(Long number, Long page) {
        PageRequest pa = PageRequest.of(Math.toIntExact(page), Math.toIntExact(number), Sort.by("rounds").descending());
        return Long.parseLong(String.valueOf(treeRepository.findAll(pa).getTotalPages())) - 1;
    }

    public Map<String, Object> getPredictNumber() throws IOException {
        File file = new File("recentTree.txt");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        List<String> content = List.of(br.readLine().split(" / "));
        Map<String, Object> predict = new HashMap<>();
        predict.put("number", content.get(0));
        predict.put("direction", content.get(1).equals("1") ? "좌" : "우");
        predict.put("time", LocalDateTime.parse(content.get(2)));
        return predict;
    }

    @Cacheable(value = "treeRecent")
    public Tree getRecent() {
        PageRequest pa = PageRequest.of(0, 1, Sort.by("rounds").descending());
        Page<Tree> tree = treeRepository.findAll(pa);
        return tree.getContent().isEmpty() ? null : tree.getContent().get(0);
    }

}
