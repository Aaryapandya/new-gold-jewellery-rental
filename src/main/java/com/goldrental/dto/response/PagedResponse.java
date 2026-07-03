package com.goldrental.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Uniform paginated response wrapper.
 *
 * @param <T> type of each item in the page
 */
@Data
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;

    /**
     * Builds a {@link PagedResponse} from a Spring Data {@link Page} with mapped content.
     */
    public static <T> PagedResponse<T> of(final Page<?> page, final List<T> mappedContent) {
        return PagedResponse.<T>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
