package com.aiqutepets.controller;

import com.aiqutepets.common.Result;
import com.aiqutepets.dto.MemoryCalendarDTO;
import com.aiqutepets.entity.DeviceDiary;
import com.aiqutepets.entity.DeviceInfo;
import com.aiqutepets.entity.UserDeviceRel;
import com.aiqutepets.mapper.DeviceDiaryMapper;
import com.aiqutepets.mapper.DeviceInfoMapper;
import com.aiqutepets.mapper.UserDeviceRelMapper;
import com.aiqutepets.util.ThirdPartyClient;
import com.aiqutepets.vo.AiBadgeListVO;
import com.aiqutepets.vo.AiDiaryDateVO;
import com.aiqutepets.vo.AiDiaryDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 记忆页面控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/memory")
@Tag(name = "记忆页面接口", description = "记忆日历、日记详情、徽章列表等接口")
public class MemoryController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private UserDeviceRelMapper userDeviceRelMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DeviceDiaryMapper deviceDiaryMapper;

    @Autowired
    private ThirdPartyClient thirdPartyClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取记忆日历
     *
     * @param userId 当前登录用户ID (JWT 解析)
     * @param year   年份
     * @param month  月份
     * @return 日历数据列表
     */
    @Operation(summary = "获取记忆日历", description = "获取指定月份的记忆日历，展示日记点标记和状态")
    @GetMapping("/calendar")
    public Result<List<MemoryCalendarDTO>> getMemoryCalendar(
            @RequestAttribute("currentUserId") Long userId,
            @Parameter(description = "年份", example = "2025") @RequestParam int year,
            @Parameter(description = "月份 (1-12)", example = "12") @RequestParam int month) {

        log.info("获取记忆日历: userId={}, year={}, month={}", userId, year, month);

        // ============ 步骤 A: 获取当前设备 ============
        UserDeviceRel currentDevice = userDeviceRelMapper.selectCurrentDevice(userId);
        if (currentDevice == null) {
            log.warn("用户没有当前选中的设备: userId={}", userId);
            return Result.error("请先绑定并选择一台设备");
        }

        String deviceUid = currentDevice.getDeviceUid();

        // 获取设备 secretKey
        DeviceInfo deviceInfo = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (deviceInfo == null) {
            log.error("设备信息不存在: deviceUid={}", deviceUid);
            return Result.error("设备信息异常");
        }
        String secretKey = deviceInfo.getSecretKey();

        // ============ 步骤 B: 计算月份的开始和结束日期 ============
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        String startDateStr = startDate.format(DATE_FORMATTER);
        String endDateStr = endDate.format(DATE_FORMATTER);

        log.info("查询日期范围: startDate={}, endDate={}", startDateStr, endDateStr);

        // ============ 步骤 C: 调用 AI 接口获取日记日期列表 ============
        List<MemoryCalendarDTO> result = new ArrayList<>();

        try {
            AiDiaryDateVO[] diaryDates = thirdPartyClient.getDiaryDates(deviceUid, secretKey, startDateStr, endDateStr);

            if (diaryDates != null) {
                for (AiDiaryDateVO vo : diaryDates) {
                    MemoryCalendarDTO dto = new MemoryCalendarDTO();
                    dto.setDate(vo.getDate());
                    dto.setHasDot(vo.getHasDiary());
                    dto.setStatus(vo.getDiaryStatus());
                    result.add(dto);
                }
            }

            log.info("记忆日历获取成功: deviceUid={}, count={}", deviceUid, result.size());

        } catch (Exception e) {
            log.error("获取记忆日历失败: deviceUid={}", deviceUid, e);
            return Result.error("获取日历数据失败");
        }

        return Result.success(result);
    }

    /**
     * 获取日记详情
     *
     * @param userId 当前登录用户ID (JWT 解析)
     * @param date   日期 (yyyy-MM-dd)
     * @return 日记详情
     */
    @Operation(summary = "获取日记详情", description = "获取指定日期的日记详情，使用 Cache-Aside 策略")
    @GetMapping("/detail")
    public Result<AiDiaryDetailVO> getDiaryDetail(
            @RequestAttribute("currentUserId") Long userId,
            @Parameter(description = "日期 (yyyy-MM-dd)", example = "2025-12-22") @RequestParam String date) {

        log.info("获取日记详情: userId={}, date={}", userId, date);

        // ============ 步骤 A: 获取当前设备 ============
        UserDeviceRel currentDevice = userDeviceRelMapper.selectCurrentDevice(userId);
        if (currentDevice == null) {
            log.warn("用户没有当前选中的设备: userId={}", userId);
            return Result.error("请先绑定并选择一台设备");
        }

        String deviceUid = currentDevice.getDeviceUid();

        // 获取设备 secretKey
        DeviceInfo deviceInfo = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (deviceInfo == null) {
            log.error("设备信息不存在: deviceUid={}", deviceUid);
            return Result.error("设备信息异常");
        }
        String secretKey = deviceInfo.getSecretKey();

        // 解析日期字符串为 LocalDate
        LocalDate diaryLocalDate = LocalDate.parse(date, DATE_FORMATTER);

        // ============ 步骤 B: 先查询数据库缓存 (Cache-Aside) ============
        DeviceDiary cachedDiary = deviceDiaryMapper.selectByDeviceUidAndDate(deviceUid, date);

        if (cachedDiary != null) {
            // 命中缓存：将数据库记录转换为 VO 返回
            log.info("日记详情命中缓存: deviceUid={}, date={}", deviceUid, date);

            AiDiaryDetailVO vo = new AiDiaryDetailVO();
            vo.setDiaryDate(cachedDiary.getDiaryDate().format(DATE_FORMATTER));
            vo.setDiaryContent(cachedDiary.getContent());

            // 格式化 AI 生成时间
            if (cachedDiary.getAiGeneratedTime() != null) {
                vo.setGeneratedAt(cachedDiary.getAiGeneratedTime().toString());
            }

            // 将 JSON 字符串转换回 List<String>
            if (cachedDiary.getEmotionTags() != null && !cachedDiary.getEmotionTags().isEmpty()) {
                try {
                    List<String> emotionTags = objectMapper.readValue(
                            cachedDiary.getEmotionTags(),
                            new TypeReference<List<String>>() {
                            });
                    vo.setEmotionTags(emotionTags);
                } catch (JsonProcessingException e) {
                    log.warn("解析情绪标签失败，使用空列表: {}", e.getMessage());
                    vo.setEmotionTags(new ArrayList<>());
                }
            } else {
                vo.setEmotionTags(new ArrayList<>());
            }

            return Result.success(vo);
        }

        // ============ 步骤 C: 未命中缓存，调用 AI 接口 ============
        log.info("日记详情未命中缓存，调用 AI 接口: deviceUid={}, date={}", deviceUid, date);

        try {
            AiDiaryDetailVO vo = thirdPartyClient.getDiaryDetail(deviceUid, secretKey, date);

            if (vo != null && vo.getDiaryContent() != null) {
                // AI 返回成功，插入数据库缓存
                DeviceDiary diary = new DeviceDiary();
                diary.setDeviceUid(deviceUid);
                diary.setDiaryDate(diaryLocalDate);
                diary.setContent(vo.getDiaryContent());

                // 解析 AI 返回的生成时间
                if (vo.getGeneratedAt() != null) {
                    try {
                        diary.setAiGeneratedTime(LocalDateTime.parse(vo.getGeneratedAt()));
                    } catch (Exception e) {
                        diary.setAiGeneratedTime(LocalDateTime.now());
                    }
                } else {
                    diary.setAiGeneratedTime(LocalDateTime.now());
                }

                // 将 List<String> 转换为 JSON 字符串存储
                try {
                    String emotionTagsJson = objectMapper.writeValueAsString(
                            vo.getEmotionTags() != null ? vo.getEmotionTags() : new ArrayList<>());
                    diary.setEmotionTags(emotionTagsJson);
                } catch (JsonProcessingException e) {
                    log.warn("序列化情绪标签失败: {}", e.getMessage());
                    diary.setEmotionTags("[]");
                }

                deviceDiaryMapper.insert(diary);
                log.info("日记详情已缓存到数据库: deviceUid={}, date={}", deviceUid, date);

                return Result.success(vo);
            } else {
                // AI 返回空数据
                log.warn("AI 返回空日记数据: deviceUid={}, date={}", deviceUid, date);
                return Result.error("该日期暂无日记记录");
            }

        } catch (RuntimeException e) {
            // 处理 AI 接口返回 4004 (无记录) 等异常
            String message = e.getMessage();
            if (message != null && message.contains("4004")) {
                log.info("AI 返回无记录 (4004): deviceUid={}, date={}", deviceUid, date);
                return Result.error("该日期暂无日记记录");
            }

            log.error("获取日记详情失败: deviceUid={}, date={}", deviceUid, date, e);
            return Result.error("获取日记详情失败");
        }
    }

    /**
     * 获取徽章墙
     *
     * @param userId 当前登录用户ID (JWT 解析)
     * @return 徽章列表
     */
    @Operation(summary = "获取徽章墙", description = "获取已解锁和未解锁的徽章列表")
    @GetMapping("/badges")
    public Result<AiBadgeListVO> getBadgeList(@RequestAttribute("currentUserId") Long userId) {

        log.info("获取徽章墙: userId={}", userId);

        // ============ 步骤 A: 获取当前设备 ============
        UserDeviceRel currentDevice = userDeviceRelMapper.selectCurrentDevice(userId);
        if (currentDevice == null) {
            log.warn("用户没有当前选中的设备: userId={}", userId);
            return Result.error("请先绑定并选择一台设备");
        }

        String deviceUid = currentDevice.getDeviceUid();

        // 获取设备 secretKey
        DeviceInfo deviceInfo = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (deviceInfo == null) {
            log.error("设备信息不存在: deviceUid={}", deviceUid);
            return Result.error("设备信息异常");
        }
        String secretKey = deviceInfo.getSecretKey();

        // ============ 步骤 B: 调用 AI 接口获取徽章列表 ============
        try {
            AiBadgeListVO badgeList = thirdPartyClient.getBadgeList(deviceUid, secretKey);

            if (badgeList != null) {
                log.info("徽章墙获取成功: deviceUid={}, unlocked={}, locked={}",
                        deviceUid,
                        badgeList.getUnlockedBadges() != null ? badgeList.getUnlockedBadges().size() : 0,
                        badgeList.getLockedBadges() != null ? badgeList.getLockedBadges().size() : 0);
                return Result.success(badgeList);
            } else {
                log.warn("AI 返回空徽章数据: deviceUid={}", deviceUid);
                return Result.error("获取徽章数据失败");
            }

        } catch (Exception e) {
            log.error("获取徽章墙失败: deviceUid={}", deviceUid, e);
            return Result.error("获取徽章数据失败");
        }
    }

    /**
     * 确认徽章已展示
     *
     * @param userId    当前登录用户ID (JWT 解析)
     * @param badgeCode 徽章代码
     * @return 操作结果
     */
    @Operation(summary = "确认徽章已展示", description = "标记徽章为已展示，用于前端弹窗确认后调用")
    @PostMapping("/badges/ack")
    public Result<Boolean> acknowledgeBadge(
            @RequestAttribute("currentUserId") Long userId,
            @Parameter(description = "徽章代码", example = "first_chat") @RequestParam String badgeCode) {

        log.info("确认徽章已展示: userId={}, badgeCode={}", userId, badgeCode);

        // ============ 步骤 A: 获取当前设备 ============
        UserDeviceRel currentDevice = userDeviceRelMapper.selectCurrentDevice(userId);
        if (currentDevice == null) {
            log.warn("用户没有当前选中的设备: userId={}", userId);
            return Result.error("请先绑定并选择一台设备");
        }

        String deviceUid = currentDevice.getDeviceUid();

        // 获取设备 secretKey
        DeviceInfo deviceInfo = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (deviceInfo == null) {
            log.error("设备信息不存在: deviceUid={}", deviceUid);
            return Result.error("设备信息异常");
        }
        String secretKey = deviceInfo.getSecretKey();

        // ============ 步骤 B: 调用 AI 接口标记徽章已展示 ============
        try {
            boolean success = thirdPartyClient.markBadgeAsShown(deviceUid, secretKey, badgeCode);

            if (success) {
                log.info("徽章标记成功: deviceUid={}, badgeCode={}", deviceUid, badgeCode);
                return Result.success(true);
            } else {
                log.warn("徽章标记失败: deviceUid={}, badgeCode={}", deviceUid, badgeCode);
                return Result.error("标记失败");
            }

        } catch (Exception e) {
            log.error("徽章标记异常: deviceUid={}, badgeCode={}", deviceUid, badgeCode, e);
            return Result.error("标记失败");
        }
    }
}
