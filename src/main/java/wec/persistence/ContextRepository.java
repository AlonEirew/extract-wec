package wec.persistence;

import org.springframework.data.repository.CrudRepository;
import wec.data.WECContext;

public interface ContextRepository extends CrudRepository<WECContext, Long> {
}
