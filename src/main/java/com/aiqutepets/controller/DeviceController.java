package com.aiqutepets.controller;

import com.aiqutepets.common.Result;
import com.aiqutepets.dto.*;
import com.aiqutepets.interceptor.JwtInterceptor;
import com.aiqutepets.service.DeviceManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 设备管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
@Tag(name = "设备接口", description = "设备校验、绑定、解绑、详情、列表等设备管理接口")
public class DeviceController {

    @Autowired
    private DeviceManageService deviceManageService;

    /**
     * 校验设备合法性
     * 无需 JWT 鉴权（蓝牙配网前调用）
     */
    @Operation(summary = "校验设备合法性", description = "蓝牙配网前校验设备是否合法，无需 JWT 鉴权")
    @GetMapping("/check-valid")
    public Result<DeviceCheckResponse> checkDeviceValid(
            @Parameter(description = "设备唯一标识", required = true, example = "ABC123") @RequestParam("deviceUid") String deviceUid) {
        log.info("收到设备校验请求: deviceUid={}", deviceUid);

        if (deviceUid == null || deviceUid.isEmpty()) {
            return Result.error(400, "deviceUid 不能为空");
        }

        try {
            DeviceCheckResponse response = deviceManageService.checkDeviceValid(deviceUid);
            return Result.success(response);
        } catch (Exception e) {
            log.error("设备校验失败", e);
            return Result.error("设备校验失败: " + e.getMessage());
        }
    }

    /**
     * 绑定设备
     * 需要 JWT 鉴权
     */
    @Operation(summary = "绑定设备", description = "将设备绑定到当前用户，需要 JWT 鉴权")
    @PostMapping("/bind")
    public Result<DeviceBindResponse> bindDevice(@RequestBody DeviceBindRequest request,
            HttpServletRequest httpRequest) {
        log.info("收到设备绑定请求: deviceUid={}", request.getDeviceUid());

        if (request.getDeviceUid() == null || request.getDeviceUid().isEmpty()) {
            return Result.error(400, "deviceUid 不能为空");
        }

        try {
            Long userId = (Long) httpRequest.getAttribute(JwtInterceptor.USER_ID_KEY);
            DeviceBindResponse response = deviceManageService.bindDevice(userId, request);

            if (response.getSuccess()) {
                return Result.success(response);
            } else {
                return Result.error(400, response.getMessage());
            }
        } catch (Exception e) {
            log.error("设备绑定失败", e);
            return Result.error("设备绑定失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备详情
     * 需要 JWT 鉴权
     */
    @Operation(summary = "获取设备详情", description = "获取指定设备的详细信息，需要 JWT 鉴权")
    @GetMapping("/detail")
    public Result<MyDeviceDTO> getDeviceDetail(
            @Parameter(description = "设备唯一标识", required = true, example = "ABC123") @RequestParam("deviceUid") String deviceUid,
            HttpServletRequest httpRequest) {
        log.info("收到获取设备详情请求: deviceUid={}", deviceUid);

        if (deviceUid == null || deviceUid.isEmpty()) {
            return Result.error(400, "deviceUid 不能为空");
        }

        try {
            Long userId = (Long) httpRequest.getAttribute(JwtInterceptor.USER_ID_KEY);
            MyDeviceDTO deviceDetail = deviceManageService.getDeviceDetail(userId, deviceUid);

            if (deviceDetail == null) {
                return Result.error(403, "无权访问该设备或设备不存在");
            }

            return Result.success(deviceDetail);
        } catch (Exception e) {
            log.error("获取设备详情失败", e);
            return Result.error("获取设备详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新设备信息（昵称、头像）
     * 需要 JWT 鉴权
     */
    @Operation(summary = "更新设备信息", description = "更新设备昵称、头像等信息，需要 JWT 鉴权")
    @PostMapping("/update")
    public Result<Void> updateDevice(@RequestBody DeviceUpdateRequest request,
            HttpServletRequest httpRequest) {
        log.info("收到更新设备信息请求: deviceUid={}", request.getDeviceUid());

        if (request.getDeviceUid() == null || request.getDeviceUid().isEmpty()) {
            return Result.error(400, "deviceUid 不能为空");
        }

        try {
            Long userId = (Long) httpRequest.getAttribute(JwtInterceptor.USER_ID_KEY);
            boolean success = deviceManageService.updateDevice(userId, request);

            if (success) {
                return Result.success();
            } else {
                return Result.error(403, "无权修改该设备或设备不存在");
            }
        } catch (Exception e) {
            log.error("更新设备信息失败", e);
            return Result.error("更新设备信息失败: " + e.getMessage());
        }
    }

    /**
     * 解除设备绑定
     * 需要 JWT 鉴权
     */
    @Operation(summary = "解绑设备", description = "解除设备与当前用户的绑定关系，需要 JWT 鉴权")
    @PostMapping("/unbind")
    public Result<String> unbindDevice(
            @Parameter(description = "设备唯一标识", required = true, example = "ABC123") @RequestParam("deviceUid") String deviceUid,
            HttpServletRequest httpRequest) {
        log.info("收到解除设备绑定请求: deviceUid={}", deviceUid);

        if (deviceUid == null || deviceUid.isEmpty()) {
            return Result.error(400, "deviceUid 不能为空");
        }

        try {
            Long userId = (Long) httpRequest.getAttribute(JwtInterceptor.USER_ID_KEY);
            String message = deviceManageService.unbindDevice(userId, deviceUid);

            if ("您未绑定该设备".equals(message)) {
                return Result.error(403, message);
            }

            return Result.success(message);
        } catch (Exception e) {
            log.error("解除设备绑定失败", e);
            return Result.error("解除设备绑定失败: " + e.getMessage());
        }
    }

    /**
     * 检查固件更新
     * 需要 JWT 鉴权
     */
    @Operation(summary = "检查固件更新", description = "检查设备是否有新固件版本，需要 JWT 鉴权")
    @GetMapping("/firmware/check")
    public Result<FirmwareCheckResponse> checkFirmwareUpdate(
            @Parameter(description = "设备唯一标识", required = true, example = "ABC123") @RequestParam("deviceUid") String deviceUid,
            HttpServletRequest httpRequest) {
        log.info("收到检查固件更新请求: deviceUid={}", deviceUid);

        if (deviceUid == null || deviceUid.isEmpty()) {
            return Result.error(400, "deviceUid 不能为空");
        }

        try {
            Long userId = (Long) httpRequest.getAttribute(JwtInterceptor.USER_ID_KEY);
            FirmwareCheckResponse response = deviceManageService.checkFirmwareUpdate(userId, deviceUid);

            if (response == null) {
                return Result.error(403, "无权访问该设备或设备不存在");
            }

            return Result.success(response);
        } catch (Exception e) {
            log.error("检查固件更新失败", e);
            return Result.error("检查固件更新失败: " + e.getMessage());
        }
    }

    /**
     * 刷新设备在线状态
     * 需要 JWT 鉴权
     */
    @Operation(summary = "刷新设备状态", description = "手动刷新设备在线状态、电量等实时信息，需要 JWT 鉴权")
    @PostMapping("/refresh-status")
    public Result<DeviceStatusResponse> refreshDeviceStatus(
            @Parameter(description = "设备唯一标识", required = true, example = "ABC123") @RequestParam("deviceUid") String deviceUid,
            HttpServletRequest httpRequest) {
        log.info("收到刷新设备状态请求: deviceUid={}", deviceUid);

        if (deviceUid == null || deviceUid.isEmpty()) {
            return Result.error(400, "deviceUid 不能为空");
        }

        try {
            Long userId = (Long) httpRequest.getAttribute(JwtInterceptor.USER_ID_KEY);
            DeviceStatusResponse response = deviceManageService.refreshDeviceStatus(userId, deviceUid);

            if (response == null) {
                return Result.error(403, "无权访问该设备或设备不存在");
            }

            return Result.success(response);
        } catch (Exception e) {
            log.error("刷新设备状态失败", e);
            return Result.error("刷新设备状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备列表（用于首页设备切换）
     * 需要 JWT 鉴权
     */
    @Operation(summary = "获取设备列表", description = "获取当前用户绑定的所有设备列表，需要 JWT 鉴权")
    @GetMapping("/list")
    public Result<List<DeviceListDTO>> getDeviceList(HttpServletRequest httpRequest) {
        log.info("收到获取设备列表请求");

        try {
            Long userId = (Long) httpRequest.getAttribute(JwtInterceptor.USER_ID_KEY);
            List<DeviceListDTO> list = deviceManageService.getDeviceList(userId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            return Result.error("获取设备列表失败: " + e.getMessage());
        }
    }

    /**
     * 切换当前设备（立即要玩）
     * 需要 JWT 鉴权
     */
    @Operation(summary = "切换当前设备", description = "切换当前选中的设备，需要 JWT 鉴权")
    @PostMapping("/switch")
    public Result<Void> switchDevice(
            @Parameter(description = "设备唯一标识", required = true, example = "ABC123") @RequestParam("deviceUid") String deviceUid,
            HttpServletRequest httpRequest) {
        log.info("收到切换设备请求: deviceUid={}", deviceUid);

        if (deviceUid == null || deviceUid.isEmpty()) {
            return Result.error(400, "deviceUid 不能为空");
        }

        try {
            Long userId = (Long) httpRequest.getAttribute(JwtInterceptor.USER_ID_KEY);
            boolean success = deviceManageService.switchDevice(userId, deviceUid);

            if (!success) {
                return Result.error(403, "无权操作该设备或设备不存在");
            }

            return Result.success();
        } catch (Exception e) {
            log.error("切换设备失败", e);
            return Result.error("切换设备失败: " + e.getMessage());
        }
    }
}
