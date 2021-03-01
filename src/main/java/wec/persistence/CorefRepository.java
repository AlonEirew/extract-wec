package wec.persistence;

import org.springframework.data.repository.CrudRepository;
import wec.data.WECCoref;

public interface CorefRepository extends CrudRepository<WECCoref, Long> {
}
