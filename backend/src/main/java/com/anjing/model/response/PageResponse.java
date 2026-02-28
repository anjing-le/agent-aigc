package com.anjing.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应类
 * 
 * <p>按照标准实现的分页响应，继承MultiResponse</p>
 * 
 * <h3>🎯 设计特点：</h3>
 * <ul>
 *   <li>📄 标准字段 - currentPage、pageSize、totalPage、total</li>
 *   <li>🏗️ 继承结构 - 继承MultiResponse，复用数据列表字段</li>
 *   <li>🔧 便捷方法 - 提供静态of方法快速创建</li>
 * </ul>
 * 
 * @param <T> 数据类型
 * @author Backend Template Team
 * @version 1.0
 */
@Setter
@Getter
@NoArgsConstructor
public class PageResponse<T> extends MultiResponse<T>
{
    private static final long serialVersionUID = 1L;

    /**
     * 当前页（兼容前端records字段）
     */
    private Integer current;

    /**
     * 当前页（原字段保留兼容）
     */
    private Integer currentPage;

    /**
     * 每页结果数（兼容前端size字段）
     */
    private Integer size;

    /**
     * 每页结果数（原字段保留兼容）
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPage;

    /**
     * 总记录数（Long类型兼容大数据量）
     */
    private Long total;

    /**
     * 数据列表（兼容前端records字段）
     */
    private List<T> records;

    /**
     * 创建分页响应
     * 
     * @param datas    数据列表
     * @param total    总记录数
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(List<T> datas, int total, int pageSize)
    {
        PageResponse<T> multiResponse = new PageResponse<>();
        multiResponse.setSuccess(true);
        multiResponse.setResponseCode("0");
        multiResponse.setResponseMessage("查询成功");
        multiResponse.setDatas(datas);
        multiResponse.setRecords(datas);
        multiResponse.setTotal((long) total);
        multiResponse.setPageSize(pageSize);
        multiResponse.setSize(pageSize);
        multiResponse.setTotalPage((total + pageSize - 1) / pageSize);
        return multiResponse;
    }

    /**
     * 根据Spring Data Page对象创建分页结果
     * 
     * @param page Spring Data Page对象
     * @param <T>  数据类型
     * @return 分页结果
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        PageResponse<T> result = new PageResponse<>();
        result.setSuccess(true);
        result.setResponseCode("0");
        result.setResponseMessage("查询成功");
        result.setDatas(page.getContent());
        result.setRecords(page.getContent());
        result.setTotal(page.getTotalElements());
        result.setCurrentPage(page.getNumber() + 1);
        result.setCurrent(page.getNumber() + 1);
        result.setPageSize(page.getSize());
        result.setSize(page.getSize());
        result.setTotalPage(page.getTotalPages());
        return result;
    }

    /**
     * Builder模式创建分页响应
     */
    public static <T> PageResponseBuilder<T> builder() {
        return new PageResponseBuilder<>();
    }

    /**
     * Builder类
     */
    public static class PageResponseBuilder<T> {
        private List<T> records;
        private Integer current;
        private Integer size;
        private Long total;

        public PageResponseBuilder<T> records(List<T> records) {
            this.records = records;
            return this;
        }

        public PageResponseBuilder<T> current(Integer current) {
            this.current = current;
            return this;
        }

        public PageResponseBuilder<T> size(Integer size) {
            this.size = size;
            return this;
        }

        public PageResponseBuilder<T> total(Long total) {
            this.total = total;
            return this;
        }

        public PageResponse<T> build() {
            PageResponse<T> response = new PageResponse<>();
            response.setSuccess(true);
            response.setResponseCode("0");
            response.setResponseMessage("查询成功");
            response.setRecords(records);
            response.setDatas(records);
            response.setCurrent(current);
            response.setCurrentPage(current);
            response.setSize(size);
            response.setPageSize(size);
            response.setTotal(total);
            if (total != null && size != null && size > 0) {
                response.setTotalPage((int) ((total + size - 1) / size));
            }
            return response;
        }
    }
}
