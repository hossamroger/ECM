package ae.sahabss.ds.ecmservice.dao;

import ae.sahabss.ds.ecmservice.domain.DsDocumentTypes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DsDocumentTypesRepository extends JpaRepository<DsDocumentTypes, String> {

    List<DsDocumentTypes> findByEntityIdEquals(Integer entityId);

}