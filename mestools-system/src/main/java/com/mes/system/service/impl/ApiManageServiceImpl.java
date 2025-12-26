package com.mes.system.service.impl;

import com.mes.common.utils.DateUtils;
import com.mes.common.utils.StringUtils;
import com.mes.system.domain.ApiManageHistory;
import com.mes.system.domain.ApiManageItem;
import com.mes.system.domain.dto.ProxyRequestDto;
import com.mes.system.mapper.ApiManageHistoryMapper;
import com.mes.system.mapper.ApiManageItemMapper;
import com.mes.system.service.IApiManageService;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 接口管理
 */
@Service
public class ApiManageServiceImpl implements IApiManageService {

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
        apiManageItem.setUpdateTime(DateUtils.getNowDate());
        return apiManageItemMapper.updateApiManageItem(apiManageItem);
    }

    @Override
    public int deleteApiManageItemById(Long itemId) {
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
                // 如果前端没传 Content-Type，默认设为表单提交
                if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                }
            }
            
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            // 3. 处理 URL 参数 (Query Params)
            // 优化：如果 params 为空（前端已手动拼接），直接使用 url，避免 UriComponentsBuilder 二次解析导致特殊字符编码问题
            String url = dto.getUrl();
            if (dto.getParams() != null && !dto.getParams().isEmpty()) {
                UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
                dto.getParams().forEach(uriBuilder::queryParam);
                url = uriBuilder.build().toUriString();
            }

            // 4. 发送请求
            HttpMethod method = HttpMethod.valueOf(dto.getMethod().toUpperCase());
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

            // 5. 封装成功响应
            result.put("status", response.getStatusCodeValue());
            result.put("statusText", response.getStatusCode().name());
            result.put("headers", response.getHeaders());
            result.put("data", response.getBody());
            result.put("size", response.getBody() != null ? response.getBody().length() + " B" : "0 B");

            // 6. 自动记录历史 (可选)
            ApiManageHistory history = new ApiManageHistory();
            history.setReqMethod(dto.getMethod());
            history.setReqUrl(url);
            history.setResStatus(response.getStatusCodeValue());
            history.setDuration((int) (System.currentTimeMillis() - startTime));
            apiManageHistoryMapper.insertApiManageHistory(history);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 捕获 4xx, 5xx 错误
            result.put("status", e.getRawStatusCode());
            result.put("statusText", e.getStatusText());
            result.put("data", e.getResponseBodyAsString());
            result.put("size", e.getResponseBodyAsString().length() + " B");

            // 记录失败历史
            ApiManageHistory history = new ApiManageHistory();
            history.setReqMethod(dto.getMethod());
            history.setReqUrl(dto.getUrl());
            history.setResStatus(e.getRawStatusCode());
            history.setDuration((int) (System.currentTimeMillis() - startTime));
            apiManageHistoryMapper.insertApiManageHistory(history);

        } catch (Exception e) {
            // 其他错误
            result.put("status", 0);
            result.put("statusText", "Error");
            result.put("data", "Proxy Error: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // 递归构建树
    private List<ApiManageItem> buildTree(List<ApiManageItem> list) {
        List<ApiManageItem> returnList = new ArrayList<>();
        List<Long> tempList = new ArrayList<>();
        for (ApiManageItem t : list) {
            tempList.add(t.getItemId());
        }
        for (ApiManageItem t : list) {
            // 如果是顶级节点 (parentId 为 0 或 不在列表中)
            if (t.getParentId() == 0 || !tempList.contains(t.getParentId())) {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        if (returnList.isEmpty()) {
            returnList = list;
        }
        return returnList;
    }

    private void recursionFn(List<ApiManageItem> list, ApiManageItem t) {
        List<ApiManageItem> childList = getChildList(list, t);
        t.setChildren(childList);
        for (ApiManageItem tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    private List<ApiManageItem> getChildList(List<ApiManageItem> list, ApiManageItem t) {
        List<ApiManageItem> tlist = new ArrayList<>();
        for (ApiManageItem n : list) {
            if (n.getParentId() != null && n.getParentId().longValue() == t.getItemId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    private boolean hasChild(List<ApiManageItem> list, ApiManageItem t) {
        return getChildList(list, t).size() > 0;
    }
}
