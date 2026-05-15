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
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URL;
import java.util.*;

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

    @PostConstruct
    public void init() {
        // 设置连接超时和读取超时，防止代理请求挂起后端线程
        if (restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory) {
            SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(10000);
        }
        // 强制设置 StringHttpMessageConverter 的默认字符集为 UTF-8，防止中文乱码
        restTemplate.getMessageConverters().forEach(converter -> {
            if (converter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8);
            }
        });
    }

    @Override
    public List<ApiManageItem> selectApiTree() {
        List<ApiManageItem> list = apiManageItemMapper.selectApiTreeList();
        return buildTree(list);
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
        ApiManageItem existingItem = apiManageItemMapper.selectApiManageItemById(apiManageItem.getItemId());
        if (existingItem != null && Integer.valueOf(1).equals(existingItem.getIsLocked())) {
            // 1. 锁定状态下，禁止通过此通用更新接口修改锁定状态(isLocked)，解锁必须走 toggleLock 接口
            // 2. 锁定状态下，只允许“移动(parentId)”操作，其他字段一律屏蔽
            if (apiManageItem.getParentId() == null) {
                throw new ServiceException("该接口已被锁定，无法修改其内容");
            }
            // 优化：采用白名单模式。创建一个只包含 ID 和 ParentID 的新对象进行更新，彻底杜绝其他字段被篡改的可能
            ApiManageItem moveNode = new ApiManageItem();
            moveNode.setItemId(apiManageItem.getItemId());
            moveNode.setParentId(apiManageItem.getParentId());
            moveNode.setUpdateBy(apiManageItem.getUpdateBy());
            moveNode.setUpdateTime(DateUtils.getNowDate());
            // 直接返回该精简对象的更新结果
            return apiManageItemMapper.updateApiManageItem(moveNode);
        }
        apiManageItem.setUpdateTime(DateUtils.getNowDate());
        return apiManageItemMapper.updateApiManageItem(apiManageItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteApiManageItemById(Long itemId) {
        // 1. 获取所有节点以辅助计算子树结构
        List<ApiManageItem> allItems = apiManageItemMapper.selectApiTreeList();
        Map<Long, ApiManageItem> itemMap = allItems.stream().collect(Collectors.toMap(ApiManageItem::getItemId, i -> i, (k1, k2) -> k1));
        // 2. 收集所有待删除的 ID (包含当前节点及其所有子孙节点)
        List<Long> deleteIds = new ArrayList<>();
        findAllDescendantIds(itemId, allItems, deleteIds);
        if (deleteIds.isEmpty()) {
            return 0;
        }
        // 3. 级联校验锁定状态：如果待删除节点中包含任何被锁定的节点，则中止操作并提醒用户
        for (Long id : deleteIds) {
            ApiManageItem item = itemMap.get(id);
            if (item != null && Integer.valueOf(1).equals(item.getIsLocked())) {
                throw new ServiceException("无法删除！节点 [" + item.getItemName() + "] 已被锁定，请先解锁后再执行删除操作。");
            }
        }
        // 4. 执行级联删除：先删除相关的历史记录，再删除接口/目录项
        int rows = 0;
        for (Long id : deleteIds) {
            apiManageHistoryMapper.deleteApiManageHistoryByItemId(id);
            rows += apiManageItemMapper.deleteApiManageItemById(id);
        }
        return rows;
    }

    /**
     * 递归收集所有后代节点 ID
     */
    private void findAllDescendantIds(Long parentId, List<ApiManageItem> allItems, List<Long> resultIds) {
        resultIds.add(parentId);
        for (ApiManageItem item : allItems) {
            if (parentId.equals(item.getParentId())) {
                findAllDescendantIds(item.getItemId(), allItems, resultIds);
            }
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
        String finalUrl = StringUtils.trim(dto.getUrl());
        int resStatus = 0;
        long startTime = System.currentTimeMillis();

        try {
            validateUrl(finalUrl);
            HttpHeaders headers = new HttpHeaders();
            if (dto.getHeaders() != null) {
                dto.getHeaders().forEach(headers::add);
            }

            Object body = dto.getBody();
            if ("form".equals(dto.getBodyType()) && body instanceof Map) {
                MultiValueMap<String, Object> formBody = new LinkedMultiValueMap<>();
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap = (Map<String, Object>) body;
                bodyMap.forEach(formBody::add);
                body = formBody;
                if (!headers.containsKey(HttpHeaders.CONTENT_TYPE) || MediaType.APPLICATION_JSON.equals(headers.getContentType()) || MediaType.APPLICATION_JSON_UTF8.equals(headers.getContentType())) {
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                }
            }
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            // 改进 URL 构建逻辑：使用 URI 对象避免二次编码，并正确合并 Query 参数
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(finalUrl);
            if (dto.getParams() != null && !dto.getParams().isEmpty()) {
                dto.getParams().forEach(uriBuilder::queryParam);
            }
            URI uri = uriBuilder.build().encode().toUri();
            finalUrl = uri.toString(); // 用于历史记录的完整 URL

            HttpMethod method = HttpMethod.valueOf(dto.getMethod().toUpperCase());
            ResponseEntity<String> response = restTemplate.exchange(uri, method, entity, String.class);

            resStatus = response.getStatusCode().value();
            result.put("status", resStatus);
            result.put("statusText", response.getStatusCode().name());
            result.put("headers", response.getHeaders());
            result.put("data", response.getBody());
            result.put("size", response.getBody() != null ? response.getBody().length() + " B" : "0 B");

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            resStatus = e.getStatusCode().value();
            result.put("status", resStatus);
            result.put("statusText", e.getStatusText());
            result.put("data", e.getResponseBodyAsString());
            result.put("size", StringUtils.isNotEmpty(e.getResponseBodyAsString()) ? e.getResponseBodyAsString().length() + " B" : "0 B");
        } catch (Exception e) {
            resStatus = 0;
            result.put("status", 0);
            result.put("statusText", "Error");
            result.put("data", e.getMessage());
            logger.error("API Proxy Error: {}", e.getMessage());
        } finally {
            try {
                ApiManageHistory history = new ApiManageHistory();
                history.setItemId(dto.getItemId() != null ? dto.getItemId() : 0L);
                history.setReqMethod(dto.getMethod());
                history.setReqUrl(finalUrl);
                history.setResStatus(resStatus);
                history.setDuration((int) (System.currentTimeMillis() - startTime));
                if (StringUtils.isNotEmpty(dto.getSnapshotJson())) {
                    history.setSnapshotJson(dto.getSnapshotJson());
                }
                apiManageHistoryMapper.insertApiManageHistory(history);
            } catch (Exception ex) {
                logger.error("保存接口历史记录失败: {}", ex.getMessage());
            }
        }
        return result;
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
            if (StringUtils.isEmpty(urlString)) {
                throw new ServiceException("URL 不能为空");
            }
            URL url = new URL(urlString.trim());
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                throw new ServiceException("仅支持 HTTP/HTTPS 协议");
            }
            //对于内部访问进行放行
            /*InetAddress address = InetAddress.getByName(url.getHost());
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()) {
                throw new ServiceException("禁止访问内部网络地址");
            }*/
        } catch (Exception e) {
            throw new ServiceException("URL 安全校验失败: " + e.getMessage());
        }
    }
}
