package wec.persistence;

import wec.data.WECMention;
import org.springframework.data.repository.CrudRepository;

public interface WecRepository extends CrudRepository<WECMention, Long> {
}
