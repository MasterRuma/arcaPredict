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
        String prompt = "Given the sequence " + numbers.toString() + ", predict the next number. Valid outputs are " + min + " and " + max + ". Output exactly one number with no extra characters, spaces, or newlines. No explanation required.";

        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            String urlStr = url + encodedPrompt;
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

                // JSON 파싱 후 "response" 필드의 값을 가져와 개행 제거 및 공백 정리
                JSONObject jsonObject = new JSONObject(response.toString());
                String result = jsonObject.getString("response").replaceAll("\\n", "").trim();
                return Integer.parseInt(result) % (max + 1);
            } else {
                throw new RuntimeException("HTTP 요청 실패, 응답 코드: " + responseCode);
            }
        } catch (Exception e) {
            return 999;
        }
    }

}
