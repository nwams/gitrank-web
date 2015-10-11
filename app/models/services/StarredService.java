package models.services;

import com.google.gson.reflect.TypeToken;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.service.GitHubService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_STARRED;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USER;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

public class StarredService extends GitHubService {

  public StarredService() {
    super();
  }

  public PageIterator<Repository> pageStarredRepositories() {
    return pageStarredRepositories(PAGE_SIZE);
  }

  public PageIterator<Repository> pageStarredRepositories(final int size) {
    return pageStarredRepositories(PAGE_FIRST, size);
  }

  public PageIterator<Repository> pageStarredRepositories(final int start, final int size) {
    PagedRequest<Repository> request = createPagedRequest(start, size);
    request.setUri(SEGMENT_USER + SEGMENT_STARRED);
    request.setType(new TypeToken<List<Repository>>() {
    }.getType());
    return createPageIterator(request);
  }

  public List<Repository> getStarredRepositories() throws IOException {
    return getAll(pageStarredRepositories());
  }
}
