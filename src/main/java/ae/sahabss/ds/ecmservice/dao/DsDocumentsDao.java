package ae.sahabss.ds.ecmservice.dao;

import ae.sahabss.ds.ecmservice.domain.DsDocumentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DsDocumentsDao extends JpaRepository<DsDocumentsEntity,Integer> {

    @Query(nativeQuery = true,value = "select DS_DOCUMENTS_SEQ.nextval from dual")
    Integer getSeqNextVal();
}
