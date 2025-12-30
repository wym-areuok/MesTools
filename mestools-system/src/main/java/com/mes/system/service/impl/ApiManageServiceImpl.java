package com.mes.system.service.impl;

import com.mes.common.utils.DateUtils;
import com.mes.common.exception.ServiceException;
import com.mes.common.utils.SecurityUtils;
import com.mes.common.utils.StringUtils;
import com.mes.system.domain.ApiManageHistory;
import com.mes.system.domain.ApiManageItem;
import com.mes.system.domain.dto.ProxyRequestDto;
import com.mes.system.mapper.ApiManageHistoryMapper;
import com.mes.system.mapper.ApiManageItemMapper;
import com.mes.system.service.IApiManageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 接口管理
 */
@Service
public class ApiManageServiceImpl implements IApiManageService {

    private static final Logger logger = LoggerFactory.getLogger(ApiManageServiceImpl.class);

    @Autowired
    private ApiManageItemMapper apiManageItemMapper;

    @Autowired
    private ApiManageHistoryMapper apiManageHistoryMapper;

    @Autowired
    private RestTemplate restTemplate; // 需确保 Spring 容器中有 RestTemplate Bean

    @Override
    public List<ApiManageItem> selectApiTree() {
        List<ApiManageItem> list = apiManageItemMapper.selectApiTreeList();
        return buildTree(list);
    }

    @Override
    public List<ApiManageItem> selectEnvList() {
        return apiManageItemMapper.selectEnvList();
    }

    @Override
    public ApiManageItem selectApiManageItemById(Long itemId) {
        return apiManageItemMapper.selectApiManageItemById(itemId);
    }

    @Override
    public int insertApiManageItem(ApiManageItem apiManageItem) {
        apiManageItem.setCreateTime(DateUtils.getNowDate());
        return apiManageItemMapper.insertApiManageItem(apiManageItem);
    }

    @Override
    public int updateApiManageItem(ApiManageItem apiManageItem) {
        // 检查是否被锁定
        ApiManageItem existingItem = apiManageItemMapper.selectApiManageItemById(apiManageItem.getItemId());
        if (existingItem != null && existingItem.getIsLocked() != null && existingItem.getIsLocked() == 1) {
            // 允许更新 parentId (拖拽) 和 isLocked (解锁操作会走 toggleLock，但防止意外)
            if (apiManageItem.getParentId() == null && apiManageItem.getIsLocked() == null) {
                throw new ServiceException("该接口已被锁定，无法修改");
            }
        }
        apiManageItem.setUpdateTime(DateUtils.getNowDate());
        return apiManageItemMapper.updateApiManageItem(apiManageItem);
    }

    @Override
    public int deleteApiManageItemById(Long itemId) {
        ApiManageItem existingItem = apiManageItemMapper.selectApiManageItemById(itemId);
        if (existingItem != null && existingItem.getIsLocked() != null && existingItem.getIsLocked() == 1) {
            throw new ServiceException("该接口已被锁定，无法删除");
        }
        return apiManageItemMapper.deleteApiManageItemById(itemId);
    }

    @Override
    @Transactional
    public void saveEnvList(List<ApiManageItem> envList) {
        // 简单策略：先删除所有环境，再重新插入
        apiManageItemMapper.deleteEnvItems();
        for (ApiManageItem item : envList) {
            item.setItemId(null); // 确保是插入
            item.setItemType("env");
            item.setCreateTime(DateUtils.getNowDate());
            apiManageItemMapper.insertApiManageItem(item);
        }
    }

    @Override
    public List<ApiManageHistory> selectHistoryList(ApiManageHistory history) {
        return apiManageHistoryMapper.selectApiManageHistoryList(history);
    }

    @Override
    public int insertHistory(ApiManageHistory history) {
        history.setCreateTime(DateUtils.getNowDate());
        return apiManageHistoryMapper.insertApiManageHistory(history);
    }

    @Override
    public Map<String, Object> proxyRequest(ProxyRequestDto dto) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        // 用于记录历史的最终URL和状态码
        String finalUrl = dto.getUrl();
        int resStatus = 0;

        // 0. 安全校验：防止 SSRF 攻击
        validateUrl(finalUrl);

        try {
            // 1. 构造 Headers
            HttpHeaders headers = new HttpHeaders();
            if (dto.getHeaders() != null) {
                dto.getHeaders().forEach(headers::add);
            }

            // 2. 构造 Body
            Object body = dto.getBody();
            // 处理 form-data / x-www-form-urlencoded
            if ("form".equals(dto.getBodyType()) && body instanceof Map) {
                MultiValueMap<String, Object> formBody = new LinkedMultiValueMap<>();
                Map<String, Object> map = (Map<String, Object>) body;
                map.forEach(formBody::add);
                body = formBody;

                // 智能修正 Content-Type:
                // 1. 如果前端没传，默认为表单提交
                // 2. 如果前端传了 application/json (默认值)，但 bodyType 是 form，强制修正为表单提交，防止 RestTemplate 发送 JSON
                if (!headers.containsKey(HttpHeaders.CONTENT_TYPE) || MediaType.APPLICATION_JSON.equals(headers.getContentType()) || MediaType.APPLICATION_JSON_UTF8.equals(headers.getContentType())) {
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                }
            }

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            // 3. 处理 URL 参数 (Query Params)
            if (dto.getParams() != null && !dto.getParams().isEmpty()) {
                UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(finalUrl);
                dto.getParams().forEach(uriBuilder::queryParam);
                finalUrl = uriBuilder.build().toUriString();
            }

            // 4. 发送请求
            HttpMethod method = HttpMethod.valueOf(dto.getMethod().toUpperCase());
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, method, entity, String.class);

            // 5. 封装成功响应
            resStatus = response.getStatusCodeValue();
            result.put("status", resStatus);
            result.put("statusText", response.getStatusCode().name());
            result.put("headers", response.getHeaders());
            result.put("data", response.getBody());
            result.put("size", response.getBody() != null ? response.getBody().length() + " B" : "0 B");

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 捕获 4xx, 5xx 错误
            resStatus = e.getRawStatusCode();
            result.put("status", resStatus);
            result.put("statusText", e.getStatusText());
            result.put("data", e.getResponseBodyAsString());
            result.put("size", e.getResponseBodyAsString().length() + " B");
        } catch (Exception e) {
            // 其他错误
            resStatus = 0;
            result.put("status", 0);
            result.put("statusText", "Error");
            result.put("data", "Proxy Error: " + e.getMessage());
            logger.error("代理请求异常", e);
        } finally {
            // 6. 统一记录历史 (放在 finally 块中确保无论成功失败都记录)
            try {
                ApiManageHistory history = new ApiManageHistory();
                history.setItemId(dto.getItemId());
                history.setReqMethod(dto.getMethod());
                history.setReqUrl(finalUrl); // 使用包含参数的完整URL
                history.setResStatus(resStatus);
                history.setDuration((int) (System.currentTimeMillis() - startTime));
                if (StringUtils.isNotEmpty(dto.getSnapshotJson())) {
                    history.setSnapshotJson(dto.getSnapshotJson());
                }
                apiManageHistoryMapper.insertApiManageHistory(history);
            } catch (Exception ex) {
                // 忽略历史记录保存失败，避免影响主流程
                logger.error("保存接口历史记录失败: {}", ex.getMessage());
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> exportData() {
        Map<String, Object> data = new HashMap<>();
        data.put("tree", selectApiTree());
        data.put("envs", selectEnvList());
        return data;
    }

    @Override
    @Transactional
    public void importData(Map<String, Object> data) {
        String username = SecurityUtils.getUsername();
        // 1. 导入环境
        if (data.containsKey("envs")) {
            String json = com.alibaba.fastjson2.JSON.toJSONString(data.get("envs"));
            List<ApiManageItem> envList = com.alibaba.fastjson2.JSON.parseArray(json, ApiManageItem.class);
            if (envList != null) {
                for (ApiManageItem item : envList) {
                    item.setItemId(null); // 重置ID，作为新数据插入
                    item.setCreateBy(username);
                    item.setCreateTime(DateUtils.getNowDate());
                    apiManageItemMapper.insertApiManageItem(item);
                }
            }
        }

        // 2. 导入接口树 (递归)
        if (data.containsKey("tree")) {
            String json = com.alibaba.fastjson2.JSON.toJSONString(data.get("tree"));
            List<ApiManageItem> treeList = com.alibaba.fastjson2.JSON.parseArray(json, ApiManageItem.class);
            if (treeList != null) {
                importTreeNodes(treeList, 0L, username);
            }
        }
    }

    @Override
    public int toggleLock(Long itemId, Integer isLocked) {
        ApiManageItem item = new ApiManageItem();
        item.setItemId(itemId);
        item.setIsLocked(isLocked);
        item.setUpdateBy(SecurityUtils.getUsername());
        item.setUpdateTime(DateUtils.getNowDate());
        return apiManageItemMapper.updateApiManageItem(item);
    }

    private void importTreeNodes(List<ApiManageItem> nodes, Long parentId, String username) {
        for (ApiManageItem node : nodes) {
            node.setItemId(null);
            node.setParentId(parentId);
            node.setCreateBy(username);
            node.setCreateTime(DateUtils.getNowDate());
            apiManageItemMapper.insertApiManageItem(node);

            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                importTreeNodes(node.getChildren(), node.getItemId(), username);
            }
        }
    }

    // 递归构建树
    // 优化：使用 Map 将时间复杂度从 O(n^2) 降低到 O(n)
    private List<ApiManageItem> buildTree(List<ApiManageItem> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        List<ApiManageItem> returnList = new ArrayList<>();
        Map<Long, ApiManageItem> map = new HashMap<>();

        // 1. 将所有节点放入 map 中，以 itemId 为 key
        for (ApiManageItem node : list) {
            map.put(node.getItemId(), node);
        }

        // 2. 遍历列表，将节点放入其父节点的 children 列表中
        for (ApiManageItem node : list) {
            Long parentId = node.getParentId();
            if (parentId != null && parentId != 0 && map.containsKey(parentId)) {
                map.get(parentId).getChildren().add(node);
            } else {
                // 如果没有父节点或父节点不存在，则为顶级节点
                returnList.add(node);
            }
        }
        return returnList;
    }

    /**
     * URL 安全校验，防止 SSRF 攻击
     */
    private void validateUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                throw new ServiceException("仅支持 HTTP/HTTPS 协议");
            }
            InetAddress address = InetAddress.getByName(url.getHost());
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()) {
                throw new ServiceException("禁止访问内部网络地址");
            }
        } catch (Exception e) {
            throw new ServiceException("URL 安全校验失败: " + e.getMessage());
        }
    }
}
