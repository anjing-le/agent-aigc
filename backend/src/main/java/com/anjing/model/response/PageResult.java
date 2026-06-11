package com.anjing.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard page payload used inside {@link APIResponse#data}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * Current page records.
     */
    private List<T> records;

    /**
     * Current page number, starting from 1.
     */
    private Integer current;

    /**
     * Page size.
     */
    private Integer size;

    /**
     * Total record count.
     */
    private Long total;

    public static <T> PageResult<T> of(List<T> records, long total, int current, int size) {
        return PageResult.<T>builder()
                .records(records)
                .current(current)
                .size(size)
                .total(total)
                .build();
    }

}
