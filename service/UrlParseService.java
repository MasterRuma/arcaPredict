package com.application.arcagambling.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlParseService {

    @Value("${url}")
    private String url;

    public Integer parse(List<Integer> numbers, Integer min, Integer max) {

        String numbersStr = URLEncoder.encode(numbers.toString(), StandardCharsets.UTF_8); // 예: %5B65%2C13%2C59%5D
        String minStr = URLEncoder.encode(String.valueOf(min), StandardCharsets.UTF_8);
        String maxStr = URLEncoder.encode(String.valueOf(max), StandardCharsets.UTF_8);

        String urlStr = url + "numbers=" + numbersStr + "&min=" + minStr + "&max=" + maxStr;

        try {
            System.out.println(urlStr);
            URL url = new URL(urlStr);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

// JSON 파싱 후 "predicted" 필드의 값을 정수로 가져오기
                JSONObject jsonObject = new JSONObject(response.toString());
                int result = jsonObject.getInt("predicted");  // getString이 아니라 getInt 사용
                return result;
            } else {
                throw new RuntimeException("HTTP 요청 실패, 응답 코드: " + responseCode);
            }
        } catch (Exception e) {
            return 999;
        }
    }

}
