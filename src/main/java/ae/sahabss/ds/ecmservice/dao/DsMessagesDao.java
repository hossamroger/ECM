package ae.sahabss.ds.ecmservice.dao;

import ae.sahabss.ds.ecmservice.domain.DsMessagesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DsMessagesDao extends JpaRepository<DsMessagesEntity,Integer> {

    DsMessagesEntity findByMsgCodeAndLang(String msgCode, String lang);
}
