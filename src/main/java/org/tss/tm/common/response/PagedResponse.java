package org.tss.tm.common.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;
    private Meta meta;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;

        private int numberOfElements;

        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;

        private String sortBy;
        private String sortDirection;
    }

    public static <T> PagedResponse<T> of(
            List<T> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            String sortBy,
            String sortDirection) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        return PagedResponse.<T>builder()
                .content(content)
                .meta(Meta.builder()
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .numberOfElements(content.size())
                        .first(pageNumber == 0)
                        .last(pageNumber == totalPages - 1)
                        .hasNext(pageNumber < totalPages - 1)
                        .hasPrevious(pageNumber > 0)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .build())
                .build();
    }

}