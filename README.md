## 平台简介
* 前端采用Vue、Element UI。
* 后端采用Spring Boot、Spring Security、Redis & Jwt。
* 权限认证使用Jwt，支持多终端认证系统。
* 支持加载动态权限菜单，多方式权限控制。
* 高效率开发，使用代码生成器可以一键生成前后端代码。

## 内置功能
1.  用户管理：用户是系统操作者，该功能主要完成系统用户配置。
2.  部门管理：配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。
3.  岗位管理：配置系统用户所属担任职务。
4.  菜单管理：配置系统菜单，操作权限，按钮权限标识等。
5.  角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
6.  字典管理：对系统中经常使用的一些较为固定的数据进行维护。
7.  参数管理：对系统动态配置常用参数。
8.  通知公告：系统通知公告信息发布维护。
9.  操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
10. 登录日志：系统登录日志记录查询包含登录异常。
11. 在线用户：当前系统中活跃用户状态监控。
12. 定时任务：在线（添加、修改、删除)任务调度包含执行结果日志。
13. 代码生成：前后端代码的生成（java、html、xml、sql）支持CRUD下载 。
14. 系统接口：根据业务代码自动生成相关的api接口文档。
15. 服务监控：监视当前系统CPU、内存、磁盘、堆栈等相关信息。
16. 缓存监控：对系统的缓存信息查询，命令统计等。
17. 在线构建器：拖动表单元素生成相应的HTML代码。
18. 连接池监视：监视当前系统数据库连接池状态，可进行分析SQL。

## 简介
> 通过配置多数据源加上开关，适用不同环境。所有的 `db`, `database`, `table` 等信息都通过字典维护，方便后期修改。
<br>权限分配最小给到按钮,适用不同用户的操作需求且用户所有操作都记录到操作日志中。
### 板卡跳站
1. **SN格式简单处理**：剔除前后空格以及行内空格，按照换行符分割。
2. **防止误操作**：为防止用户在不查询结果的情况下直接跳站，页面有限制，必须先查询再跳站。
3. **类型选择**：用户选择类型后，后端利用字典获取对应类型相关的表，并执行相应站点的处理方法。
4. **途程信息追加**：后端查询完SN结果后，会再次利用SN查询字典中配置的SFC表，并将途程信息追加到每条结果中。前端获取第一个SN的 `sfc` 字段回填显示，并加工成 `code-name` 形式，方便查看对应站点信息。
5. **安全限制**：
    *   目前只支持同机种的SN跳站，不同机种跳站因风险较高未增加此功能。
    *   撤销跳站功能也因风险和安全性考虑被移除。
6. **后端校验**：
    *   验证所有SN必须是同一个 `model`。
    *   每个SN必须有且只有一个SFC信息。
7. **后端实现**：
    * 其中四个跳站的类型PCA RMA LR MDS需要考虑的差异性，
      * 站点：LR-->PROCESS_CODE PCA/RMA/MDS-->WC
      * SN：MDS-->Sno PCA/RMA/LR-->McbSno
      * NWC：MDS-->NextWc PCA/RMA/LR-->NWC
      * 因只找到PCA和RMA的跳站记录log，且表结构不一致，插入数据时需要区别处理。
      * 所有类型都使用List<Map<String, Object>>格式返回数据，前端设计成动态表格方便字段展示。

### 字符串处理
1.  **输入处理**：对于字符串输入，按照换行符分割，并剔除前后和行内的空格。
2.  **性能优化**：
    *   当处理行数<1w行时，直接使用浏览器处理。
    *   当处理行数>1w行时，自动调用后端接口。前端采用分块处理技术，对输入内容和结果加载进行分块，防止浏览器因一次性加载大量DOM而卡死。
3.  **复制功能**：考虑到浏览器的版本兼容性问题。
4.  **大数据处理**：
    *   使用代码生成模板，减少不必要的模板文件上传。
    *   在处理大数据<100w时，采用 `Excel` + 表的形式(个人电脑8G内存处理100w测试耗时30~60s)。
    *   在存储新数据前，先查询并删除当前用户的旧数据，完成后再执行新任务的插入，最后建立索引。
5.  **系统稳定性**：
    *   采用 `StringBuilder` 并预估分配空间，以减少资源消耗。
    *   添加资源保护机制，增加了最大执行时间和最大处理行数限制。
    *   流式处理 `Excel`，并根据内存大小动态设置批量插入数据库的数量。

### FisWeb改密
1.  **用户体验优化**：修改了用户模块，增加了工号和FIS账号字段。当用户修改自己的账号时，系统会自动带出这些信息，省去输入的步骤(后端自动获取当前用户)。
2.  **配置化**：相关的表在字典中进行配置。
3.  **安全校验**：修改其他工号时，对输入的字符串有长度限制和空格处理。修改前会进行查询，必须保证查询结果有且只有一个才满足修改条件。

### 资料查询
1.  **查询方式**：
    *   对资料维护引入“标签”和“类型”的概念，标签支持多选，方便查询。
    *   资料标题作为唯一区分标记，支持模糊查询。
2.  **结果排序**：增加“搜索次数”字段，每次查询到的结果该字段+1。查询结果根据“搜索次数”和“时间”倒序排列，以便优先展示常用数据。
3.  **标签逻辑**：标签查询可以多选，结果只需要满足其中一个标签属性即可展示（OR逻辑）。
4.  **内容维护**：资料内容维护前端采用富文本编辑器，支持图片以及文字的格式排版等。

### 执行SQL
1.  **编辑器功能**：
    *   使用 `CodeMirror` 创建SQL编辑器，支持语法高亮和关键字补全。
    *   模拟SSMS界面执行SQL，支持鼠标选中部分语句，以及 `Ctrl+Enter` 快捷键执行,但为了安全性考虑,限制每次只能执行一条SQL。
2.  **超时保护**：增加保护机制，执行时间限制在5秒内，超时则中断后台线程以取消数据库查询。
3.  **SQL语句验证**（前后端均有）：
    *   **危险关键字**：黑名单机制，禁止执行 `DROP`, `TRUNCATE`, `ALTER`, `CREATE`, `RENAME` 等危险操作。
    *   **批量操作**：禁止批量操作,可以通过分号或关键字自动识别是否是批量操作。
    *   **过滤掉注释**：注释不影响sql执行。
    *   **查询限制**：查询语句必须带 `TOP N` 且 `N < 1000`，防止数据库被长时间锁定。
    *   **更新/删除限制**：
        *   移除 `ORDER BY`, `GROUP BY` 等子句。
        *   必须带 `WHERE` 条件，且条件不能为恒真（如 `1=1`）。
        *   `WHERE` 条件中只能带 `=` 或 `IN`。
4.  **日志记录**：
    *   SQL语句执行详情记录到日志。
    *   **注意**：对于查询操作，如果结果集过大，可能会因log表字段长度限制导致无法完整记录该条日志，所以设置成其他字段正常记录但是结果集不保存。
5.  **结果集格式**：由于不同表的字段名称不同，结果中每行数据使用 `Map<String, Object>` 类型封装，最终转换成 `List<Map<String, Object>>` 返回。
6.  **历史记录**：增加SQL执行的历史日志功能，按时间倒序做成时间线展示。

## 备注
1. sql脚本在不同服务器的sqlserver导入时要根据sqlserver的安装路径修改脚本中的路径和指定编码(导出sql时如指定则忽略)：
```javascript
FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER02\MSSQL\DATA

COLLATE Chinese_Taiwan_Stroke_BIN

/*示例代码*/
USE [master]
GO
/****** Object:  Database [dailytools]    Script Date: 2025/11/18 23:53:04 ******/
CREATE DATABASE [dailytools]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'dailytools', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER02\MSSQL\DATA\dailytools.mdf' , SIZE = 8192KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON 
( NAME = N'dailytools_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER02\MSSQL\DATA\dailytools_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 COLLATE Chinese_Taiwan_Stroke_BIN
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
```
2.stringTool模块相关的表string_tool_temp(用于大数量级字符串导入到db存储)
```javascript
-- 如果存在旧表，先删除
DROP TABLE IF EXISTS string_tool_temp;
-- 创建新表 id自增1
CREATE TABLE string_tool_temp (
    id BIGINT NOT NULL IDENTITY(1,1) PRIMARY KEY,
    data NVARCHAR(100) NOT NULL,
    user_id INT NOT NULL,
    -- 在创建表的同时创建索引
    INDEX IX_string_tool_temp_user_id NONCLUSTERED (user_id)
);
```
3.stringTool模块相关的表string_tool_info(用于存储资料信息)
```javascript
USE [dailytools]
GO
/****** Object:  Table [dbo].[string_tool_info]
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[string_tool_info](
	[info_id] [int] IDENTITY(10,1) NOT NULL,
	[info_title] [nvarchar](50) NOT NULL,
	[info_tags] [nvarchar](255) NULL,
	[info_type] [nvarchar](50) NULL,
	[info_content] [nvarchar](max) NULL,
	[status] [char](1) NULL,
	[create_by] [nvarchar](64) NULL,
	[create_time] [datetime] NULL,
	[update_by] [nvarchar](64) NULL,
	[update_time] [datetime] NULL,
	[remark] [nvarchar](255) NULL,
	[search_count] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[info_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
ALTER TABLE [dbo].[string_tool_info] ADD  DEFAULT (NULL) FOR [info_tags]
GO
ALTER TABLE [dbo].[string_tool_info] ADD  DEFAULT (NULL) FOR [info_content]
GO
ALTER TABLE [dbo].[string_tool_info] ADD  DEFAULT ('0') FOR [status]
GO
ALTER TABLE [dbo].[string_tool_info] ADD  DEFAULT ('') FOR [create_by]
GO
ALTER TABLE [dbo].[string_tool_info] ADD  DEFAULT (getdate()) FOR [create_time]
GO
ALTER TABLE [dbo].[string_tool_info] ADD  DEFAULT ('') FOR [update_by]
GO
ALTER TABLE [dbo].[string_tool_info] ADD  DEFAULT (NULL) FOR [remark]
GO
ALTER TABLE [dbo].[string_tool_info] ADD  DEFAULT ((0)) FOR [search_count]
GO
```
## 演示图
<table>
    <tr>
        <td><img src=".\demopic\homepage.png"/></td>
        <td><img src=".\demopic\jumpstation.png"/></td>
    </tr>
    <tr>
        <td><img src=".\demopic\stringtool.png"/></td>
        <td><img src=".\demopic\changepwd.png"/></td>
    </tr>
    <tr>
        <td><img src=".\demopic\queryinfo.png"/></td>
        <td><img src=".\demopic\executesql.png"/></td>
    </tr>
</table>