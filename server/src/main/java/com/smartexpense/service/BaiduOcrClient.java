package com.smartexpense.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.smartexpense.config.BaiduOcrProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 百度增值税发票识别客户端（HTTP 直连，不引 SDK）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduOcrClient {

    private final BaiduOcrProperties properties;

    private volatile String accessToken;
    private volatile long tokenExpireTime = 0L;

    /**
     * 识别增值税发票，返回字段：
     * invoiceType / invoiceCode / invoiceNo / invoiceDate / amount / taxAmount / sellerName / buyerName
     */
    public Map<String, String> recognizeVatInvoice(byte[] imageBytes) {
        String token = getAccessToken();
        String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/vat_invoice?access_token=" + token;
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);

        HttpResponse resp = HttpUtil.createPost(url)
                .form("image", base64)
                .timeout(20000)
                .execute();

        JSONObject json = JSONUtil.parseObj(resp.body());
        if (json.containsKey("error_code")) {
            throw new RuntimeException("百度OCR识别失败: " + json.getStr("error_msg"));
        }
        JSONObject words = json.getJSONObject("words_result");
        if (words == null) {
            throw new RuntimeException("百度OCR未识别到发票信息");
        }

        Map<String, String> result = new HashMap<>();
        result.put("invoiceType", words.getStr("InvoiceType"));
        result.put("invoiceCode", words.getStr("InvoiceCode"));
        result.put("invoiceNo", words.getStr("InvoiceNum"));
        result.put("invoiceDate", normalizeDate(words.getStr("InvoiceDate")));
        result.put("amount", words.getStr("AmountInFiguers"));
        result.put("taxAmount", words.getStr("TotalTax"));
        result.put("sellerName", words.getStr("SellerName"));
        result.put("buyerName", words.getStr("PurchaserName"));
        return result;
    }

    /**
     * 智能票据识别（multiple_invoice），覆盖火车票/行程单/定额发票/出租车票等非增值税发票。
     * 返回与 recognizeVatInvoice 相同字段 key，额外返回 ticketType（票据类型）。
     */
    public Map<String, String> recognizeMultipleInvoice(byte[] imageBytes) {
        String token = getAccessToken();
        String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/multiple_invoice?access_token=" + token;
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);

        HttpResponse resp = HttpUtil.createPost(url)
                .form("image", base64)
                .timeout(20000)
                .execute();

        JSONObject json = JSONUtil.parseObj(resp.body());
        if (json.containsKey("error_code")) {
            throw new RuntimeException("百度OCR识别失败: " + json.getStr("error_msg"));
        }
        JSONArray words = json.getJSONArray("words_result");
        if (words == null || words.isEmpty()) {
            throw new RuntimeException("百度OCR未识别到票据信息");
        }

        JSONObject ticket = words.getJSONObject(0);
        String type = ticket.getStr("type");
        JSONObject r = ticket.getJSONObject("result");

        Map<String, String> result = new HashMap<>();
        result.put("ticketType", type);
        result.put("invoiceNo", firstWord(r, "ticket_num", "ticket_number", "invoice_number", "serial_number"));
        result.put("amount", cleanAmount(amountWord(type, r)));
        result.put("invoiceDate", normalizeDate(firstWord(r, "date", "start_date", "invoice_date")));
        result.put("sellerName", firstWord(r, "carrier", "purchaser_name", "seller_name"));
        return result;
    }

    /** 从 result 中取第一个非空的 word 字段 */
    private String firstWord(JSONObject r, String... keys) {
        if (r == null) {
            return null;
        }
        for (String key : keys) {
            String v = getWord(r, key);
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return null;
    }

    /** multiple_invoice 的 result 字段值为 [{word: "..."}] 数组，取第一个 word */
    private String getWord(JSONObject r, String key) {
        Object v = r.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof JSONArray) {
            JSONArray arr = (JSONArray) v;
            if (arr.isEmpty()) {
                return null;
            }
            return arr.getJSONObject(0).getStr("word");
        }
        return v.toString();
    }

    /** 不同票据类型金额字段名不同，按 type 取对应字段 */
    private String amountWord(String type, JSONObject r) {
        switch (type == null ? "" : type) {
            case "train_ticket":
                return firstWord(r, "ticket_rates", "fare");
            case "air_ticket":
                return firstWord(r, "fare", "ticket_rates");
            case "taxi_receipt":
                return firstWord(r, "fare");
            case "quota_invoice":
                return firstWord(r, "invoice_rate_in_figure", "invoice_rate");
            default:
                return firstWord(r, "amount", "fare", "ticket_rates", "invoice_rate");
        }
    }

    /** 清洗金额：从"¥134.5元"提取"134.5" */
    private String cleanAmount(String s) {
        if (s == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+(\\.\\d+)?").matcher(s);
        return m.find() ? m.group() : null;
    }

    /** 把百度返回的中文日期（如"2016年04月11日"）转成 ISO 格式（"2016-04-11"） */
    private String normalizeDate(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{4})\\D+(\\d{1,2})\\D+(\\d{1,2})")
                .matcher(date);
        if (m.find()) {
            return m.group(1) + "-" + String.format("%02d", Integer.parseInt(m.group(2)))
                    + "-" + String.format("%02d", Integer.parseInt(m.group(3)));
        }
        return date;
    }

    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }
        String url = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials"
                + "&client_id=" + properties.getApiKey()
                + "&client_secret=" + properties.getSecretKey();

        HttpResponse resp = HttpRequest.get(url).timeout(10000).execute();
        JSONObject json = JSONUtil.parseObj(resp.body());
        if (!json.containsKey("access_token")) {
            throw new RuntimeException("获取百度OCR access_token失败: " + resp.body());
        }
        accessToken = json.getStr("access_token");
        long expiresIn = json.getLong("expires_in", 2592000L);
        tokenExpireTime = System.currentTimeMillis() + (expiresIn - 60) * 1000;
        return accessToken;
    }
}
