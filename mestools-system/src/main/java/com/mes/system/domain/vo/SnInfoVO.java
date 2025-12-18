package com.mes.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;


import java.util.Date;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-02
 * @Description: SN相关信息的VO
 */
@Data
public class SnInfoVO {
    /*SN ID*/
    private int snoId;
    /*SN序列号*/
    private String mcbSno;
    /*工单号*/
    private String wkNo;
    /*机型*/
    private String model;
    /*线别*/
    private String pdLine;
    /*站点code*/
    private String wc;
    /*版本号*/
    private String rev;
    /*PCB版本号*/
    private String pcb;
    /*是否过站*/
    private String isPass;
    /*下一站*/
    private String nwc;
    /*状态*/
    private String status;
    /*PEN版本号*/
    private String penNo;
    /*创建时间*/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cdt;
    /*修改时间*/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date udt;
    /*途程SFC*/
    private String sfc;
}
