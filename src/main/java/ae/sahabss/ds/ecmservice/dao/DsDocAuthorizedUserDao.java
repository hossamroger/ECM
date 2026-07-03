package ae.sahabss.ds.ecmservice.dao;

import ae.sahabss.ds.ecmservice.domain.DsDocAuthorizedUsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DsDocAuthorizedUserDao extends JpaRepository<DsDocAuthorizedUsersEntity,Integer> {

    List<DsDocAuthorizedUsersEntity> findByDocIdAndUserId(String docId,String userId);
    List<DsDocAuthorizedUsersEntity> findByDocIdAndEid(String docId,String eId);
    List<DsDocAuthorizedUsersEntity> findByDocIdAndMobileNo(String docId,String mobNo);

}
