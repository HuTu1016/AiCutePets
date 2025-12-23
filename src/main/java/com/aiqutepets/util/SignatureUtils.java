package com.aiqutepets.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名工具类
 * 实现《AI 玩具接入文档》1.4 节定义的签名算法
 * 
 * 签名规则:
 * 1. 过滤: 移除 key 为 "signature" 的参数
 * 2. 转小写: 将所有参数名(key)和参数值(value)都转换为小写字符串
 * 3. 排序: 按参数名(key)的 ASCII 升序排序
 * 4. 拼接: 遍历排序后的参数，格式为 key=URL_ENCODE(value)
 * 5. 追加密钥: 在最后追加 secretkey=密钥
 * 6. 加密: 将拼接后的字符串转小写 -> MD5 加密 -> 32位小写 Hex 字符串
 */
public class SignatureUtils {

    /**
     * 生成签名（默认使用无&符紧凑拼接模式）
     *
     * @param params    请求参数
     * @param secretKey 密钥
     * @return 32位小写 MD5 签名
     */
    public static String generateSignature(Map<String, Object> params, String secretKey) {
        return generateSignature(params, secretKey, false);
    }

    /**
     * 生成签名
     *
     * @param params       请求参数
     * @param secretKey    密钥
     * @param useAmpersand 是否使用 & 符连接参数
     *                     - false: key1=val1key2=val2secretkey=xxx
     *                     (无&符紧凑拼接，嵌入式协议常用)
     *                     - true: key1=val1&key2=val2&secretkey=xxx (有&符拼接，HTTP
     *                     协议常用)
     * @return 32位小写 MD5 签名
     */
    public static String generateSignature(Map<String, Object> params, String secretKey, boolean useAmpersand) {
        // 1. 过滤并转小写，使用 TreeMap 自动排序
        TreeMap<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();

            // 过滤 signature 参数
            if ("signature".equalsIgnoreCase(key)) {
                continue;
            }

            // 转小写
            String lowerKey = key.toLowerCase();
            String lowerValue = entry.getValue() != null
                    ? entry.getValue().toString().toLowerCase()
                    : "";

            sortedParams.put(lowerKey, lowerValue);
        }

        // 2. 拼接参数
        StringBuilder sb = new StringBuilder();
        String separator = useAmpersand ? "&" : "";
        boolean first = true;

        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (!first && useAmpersand) {
                sb.append(separator);
            }
            first = false;

            String encodedValue = urlEncode(entry.getValue());
            sb.append(entry.getKey()).append("=").append(encodedValue);
        }

        // 3. 追加密钥
        if (useAmpersand) {
            sb.append("&");
        }
        sb.append("secretkey=").append(secretKey.toLowerCase());

        // 4. 转小写并进行 MD5 加密
        String stringToSign = sb.toString().toLowerCase();
        return md5Hex(stringToSign);
    }

    /**
     * URL 编码 (UTF-8)
     */
    private static String urlEncode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 肯定支持，不会抛出异常
            return value;
        }
    }

    /**
     * MD5 加密，返回32位小写 Hex 字符串
     */
    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }

    /**
     * 验证签名
     *
     * @param params       请求参数（包含 signature）
     * @param secretKey    密钥
     * @param signatureKey 签名参数名（默认 "signature"）
     * @return 签名是否有效
     */
    public static boolean verifySignature(Map<String, Object> params, String secretKey, String signatureKey) {
        Object signature = params.get(signatureKey);
        if (signature == null) {
            return false;
        }

        String expectedSignature = generateSignature(params, secretKey);
        return expectedSignature.equalsIgnoreCase(signature.toString());
    }

    /**
     * 验证签名（使用默认签名参数名 "signature"）
     */
    public static boolean verifySignature(Map<String, Object> params, String secretKey) {
        return verifySignature(params, secretKey, "signature");
    }

    // ==================== 测试验证 ====================

    public static void main(String[] args) {
        // 测试示例
        // 根据文档提供的示例数据进行验证
        // 请根据实际文档示例替换以下数据

        Map<String, Object> testParams = new java.util.HashMap<>();
        testParams.put("deviceId", "ABC123DEF456");
        testParams.put("timestamp", "1703318400");
        testParams.put("action", "bind");

        String secretKey = "your_secret_key_here";

        System.out.println("========== 签名算法测试 ==========");
        System.out.println();

        // 无 & 符紧凑拼接模式（默认）
        String signatureCompact = generateSignature(testParams, secretKey, false);
        System.out.println("【无&符紧凑拼接模式】");
        System.out.println("拼接示例: action=binddeviceid=abc123def456timestamp=1703318400secretkey=xxx");
        System.out.println("签名结果: " + signatureCompact);
        System.out.println();

        // 有 & 符拼接模式
        String signatureWithAmp = generateSignature(testParams, secretKey, true);
        System.out.println("【有&符拼接模式】");
        System.out.println("拼接示例: action=bind&deviceid=abc123def456&timestamp=1703318400&secretkey=xxx");
        System.out.println("签名结果: " + signatureWithAmp);
        System.out.println();

        // 打印拼接过程（用于调试）
        System.out.println("========== 拼接过程详解 ==========");
        printSignatureProcess(testParams, secretKey);
    }

    /**
     * 打印签名生成过程（用于调试）
     */
    public static void printSignatureProcess(Map<String, Object> params, String secretKey) {
        System.out.println("原始参数:");
        params.forEach((k, v) -> System.out.println("  " + k + " = " + v));

        // 排序并转小写
        TreeMap<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            if ("signature".equalsIgnoreCase(key))
                continue;
            sortedParams.put(key.toLowerCase(),
                    entry.getValue() != null ? entry.getValue().toString().toLowerCase() : "");
        }

        System.out.println("\n排序后参数 (key 转小写):");
        sortedParams.forEach((k, v) -> System.out.println("  " + k + " = " + v));

        // 拼接
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            sb.append(entry.getKey()).append("=").append(urlEncode(entry.getValue()));
        }
        sb.append("secretkey=").append(secretKey.toLowerCase());

        System.out.println("\n拼接字符串 (无&符):");
        System.out.println("  " + sb.toString());

        System.out.println("\n最终待签名字符串 (转小写):");
        System.out.println("  " + sb.toString().toLowerCase());

        System.out.println("\nMD5 签名结果:");
        System.out.println("  " + md5Hex(sb.toString().toLowerCase()));
    }
}
