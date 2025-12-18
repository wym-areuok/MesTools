package com.mes.system.service;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-07
 * @Description: 修改FIS账号密码的Service
 */
public interface IChangePwdService {

    boolean changePwd(String fisNumber, String password, String dbDataSource);
}