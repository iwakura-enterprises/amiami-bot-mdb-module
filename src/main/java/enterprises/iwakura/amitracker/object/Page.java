package enterprises.iwakura.amitracker.object;

import java.util.List;

import lombok.Data;

@Data
public class Page<T> {

    private List<T> content;
    private int pageSize;
    private int totalPages;
    private long totalElements;

    public Page(List<T> content, int pageSize, int totalPages, long totalElements) {
        this.content = content;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }
}
