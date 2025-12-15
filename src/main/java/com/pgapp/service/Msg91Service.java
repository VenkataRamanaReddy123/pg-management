package com.pgapp.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class Msg91Service {

    private final String AUTH_KEY = "481643AytvS6Bl2m69346b71P1"; 
    private final String WHATSAPP_FLOW_ID = "your_whatsapp_flow_id_here";  // from MSG91 dashboard

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================
    // WhatsApp OTP Sender
    // =========================
    public void sendWhatsappOtp(String mobile, String otp) {
        try {
            String url = "https://api.msg91.com/api/v5/flow/";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", AUTH_KEY);

            // Mobile must be with country code, no '+'
            // Example: 919876543210
            String body = "{"
                    + "\"flow_id\":\"" + WHATSAPP_FLOW_ID + "\","
                    + "\"recipients\":[{\"mobiles\":\"" + mobile + "\",\"otp\":\"" + otp + "\"}]"
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String response = restTemplate.postForObject(url, entity, String.class);

            System.out.println("📲 WhatsApp Raw Response: " + response);

            JsonNode root = objectMapper.readTree(response);

            if (root.has("type") && "success".equals(root.get("type").asText())) {
                System.out.println("✅ WhatsApp OTP sent successfully to: " + mobile);
            } else {
                System.out.println("⚠️ WhatsApp API response: " + response);
            }

        } catch (Exception e) {
            System.err.println("❌ WhatsApp OTP error:");
            e.printStackTrace();
        }
    }
}
