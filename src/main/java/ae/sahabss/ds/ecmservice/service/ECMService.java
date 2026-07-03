package ae.sahabss.ds.ecmservice.service;

import ae.sahabss.ds.ecmservice.dao.DsDocAuthorizedUserDao;
import ae.sahabss.ds.ecmservice.dao.DsDocumentTypesRepository;
import ae.sahabss.ds.ecmservice.dao.DsDocumentsDao;
import ae.sahabss.ds.ecmservice.dao.DsMessagesDao;
import ae.sahabss.ds.ecmservice.domain.DsDocAuthorizedUsersEntity;
import ae.sahabss.ds.ecmservice.domain.DsDocumentTypes;
import ae.sahabss.ds.ecmservice.domain.DsDocumentsEntity;
import ae.sahabss.ds.ecmservice.domain.DsMessagesEntity;
import ae.sahabss.ds.ecmservice.dto.*;
import ae.sahabss.ds.ecmservice.exceptions.DocumentNotFoundException;
import ae.sahabss.ds.ecmservice.exceptions.FileNotValidException;
import ae.sahabss.ds.ecmservice.exceptions.UserNotAuthorizedException;
import ae.sahabss.ds.ecmservice.security.UserDetails;
import ae.sahabss.ds.ecmservice.ucm.IContent;
import ae.sahabss.ds.ecmservice.ucm.UCMDocument;
import ae.sahabss.ds.ecmservice.ucm.UCMUtilities;
import ae.sahabss.ds.ecmservice.utils.FileTypeEnum;
import org.apache.commons.codec.binary.Base64;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ECMService {

    @Resource(name = "getUserDetails")
    private UserDetails userDetails;
    private final IContent ucmUtilities;
    private final DsDocumentsDao dsDocumentsDao;
    private final DsMessagesDao messagesDao;

    private final DsDocAuthorizedUserDao dsDocAuthorizedUserDao;
    private final DsDocumentTypesRepository dsDocumentTypesRepository;
    @Value("${ucm.admin.username}")
    private String ecmAdminUsername;

    @Value("${ucm.admin.password}")
    private String ecmAdminPassword;

    @Value("${ds-valid-user}")
    private String digitalUser;

    public ECMService(UCMUtilities ucmUtilities, DsDocumentsDao dsDocumentsDao, DsMessagesDao messagesDao, DsDocAuthorizedUserDao dsDocAuthorizedUserDao, DsDocumentTypesRepository dsDocumentTypesRepository) {
        this.ucmUtilities = ucmUtilities;
        this.dsDocumentsDao = dsDocumentsDao;
        this.messagesDao = messagesDao;
        this.dsDocAuthorizedUserDao = dsDocAuthorizedUserDao;
        this.dsDocumentTypesRepository = dsDocumentTypesRepository;
    }

    public UploadDocResponse uploadDoc(UploadDocRequest request) throws Exception {
        byte[] decodedBytes = Base64.decodeBase64(request.getDocBase64().getBytes());

        //detect file mime type
        Tika tika = new Tika();
        String actualMimeType = tika.detect(decodedBytes);

        //validate mime type
        if (!FileTypeEnum.isValidType(actualMimeType, request.getMimeType(), request.getFileName()))
            throw new FileNotValidException();

        ucmUtilities.login(ecmAdminUsername, ecmAdminPassword);

        InputStream docInputStream = new ByteArrayInputStream(decodedBytes);

        Integer contentId = dsDocumentsDao.getSeqNextVal();
        String contentIdString = "0000000".substring(0, 7 - contentId.toString().length()) + contentId;

        String docId = ucmUtilities.upload(contentIdString, "Document", request.getFileName(), docInputStream, null);

        DsDocumentsEntity documentsEntity = new DsDocumentsEntity();
        documentsEntity.setDsDocumentId(contentId);
        documentsEntity.setDocId(docId);
        documentsEntity.setMimeType(request.getMimeType());
        documentsEntity.setFileName(request.getFileName());
        documentsEntity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        dsDocumentsDao.save(documentsEntity);


        //adding creator as an Authorized user
        DsDocAuthorizedUsersEntity dsDocAuthorizedUsersEntity = new DsDocAuthorizedUsersEntity();
        dsDocAuthorizedUsersEntity.setDocId(docId);
        dsDocAuthorizedUsersEntity.setUserId(userDetails.getUserId());
        dsDocAuthorizedUsersEntity.setMobileNo(userDetails.getMobile());
        dsDocAuthorizedUsersEntity.setEid(userDetails.getIdn());
        dsDocAuthorizedUserDao.save(dsDocAuthorizedUsersEntity);

        UploadDocResponse uploadDocResponse = new UploadDocResponse();
        uploadDocResponse.setDocId(docId);

        return uploadDocResponse;
    }

    public DownloadDocResponse downloadDoc(DownloadDocRequest request) throws Exception {
        validateAuthorizedUser(request);

        ucmUtilities.login(ecmAdminUsername, ecmAdminPassword);

        UCMDocument documentInfo = ucmUtilities.getDocumentInfo(request.getDocId());

        if (documentInfo == null)
            throw new DocumentNotFoundException();

        DownloadDocResponse downloadDocResponse = new DownloadDocResponse();
        downloadDocResponse.setFileName(documentInfo.getFilename());
        downloadDocResponse.setFileFormat(documentInfo.getFormat());


        InputStream docInputStream = ucmUtilities.download(request.getDocId());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[4];

        while ((nRead = docInputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        byte[] targetArray = buffer.toByteArray();

        String encodedDoc = new String(Base64.encodeBase64(targetArray));

        downloadDocResponse.setEncodedDoc(encodedDoc);

        return downloadDocResponse;
    }

    private void validateAuthorizedUser(DownloadDocRequest request) throws UserNotAuthorizedException {
        //validate authorized user
        if (userDetails != null && !"EP".equalsIgnoreCase(userDetails.getIdType())) {
            if(!userDetails.isEntity()) {
                // Check if the user is not a digital user
                if (!digitalUser.equalsIgnoreCase(userDetails.getUserId())) {
                    // Check if the user is authorized
                    boolean isValid = isAuthorizedUser(request);
                    if (!isValid)
                        throw new UserNotAuthorizedException();
                }
            }
        }
    }

    private boolean isAuthorizedUser(DownloadDocRequest request) {
        // Use interface type instead of concrete implementation for flexibility
        List<DsDocAuthorizedUsersEntity> docAuthorizedUsersEntities = new ArrayList<>();

        // Check each condition separately and return immediately if authorized
        if (userDetails.getUserId() != null) {
            docAuthorizedUsersEntities = dsDocAuthorizedUserDao.findByDocIdAndUserId(request.getDocId(), userDetails.getUserId());
            if (!docAuthorizedUsersEntities.isEmpty()) {
                return true;
            }
        }

        if (userDetails.getIdn() != null) {
            docAuthorizedUsersEntities = dsDocAuthorizedUserDao.findByDocIdAndEid(request.getDocId(), userDetails.getIdn());
            if (!docAuthorizedUsersEntities.isEmpty()) {
                return true;
            }
        }

        if (userDetails.getMobile() != null) {
            docAuthorizedUsersEntities = dsDocAuthorizedUserDao.findByDocIdAndMobileNo(request.getDocId(), userDetails.getMobile());
            if (!docAuthorizedUsersEntities.isEmpty()) {
                return true;
            }
        }

        // If none of the conditions were met, return false
        return false;
    }

    public AllowedUploadDocumentsResponse getAllowedUploadDocumentsByEntityId(Integer entityId) {
        List<DsDocumentTypes> documentTypesList = dsDocumentTypesRepository.findByEntityIdEquals(entityId);
        return new AllowedUploadDocumentsResponse(documentTypesList);
    }

    public DownloadDocResponse downloadDocsByIds(List<String> docIdsList) throws Exception {
        ucmUtilities.login(ecmAdminUsername, ecmAdminPassword);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(byteArrayOutputStream)) {
            int index = 1;
            for (String docId : docIdsList) {

                UCMDocument documentInfo = ucmUtilities.getDocumentInfo(docId);

                ZipEntry zipEntry = new ZipEntry(index + "-" + documentInfo.getFilename());
                zos.putNextEntry(zipEntry);
                index++;

                InputStream docInputStream = ucmUtilities.download(docId);
//                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int len;
                byte[] buffer = new byte[1024];
                while ((len = docInputStream.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
//                buffer.flush();
//                byte[] targetArray = buffer.toByteArray();
//                ByteArrayInputStream bais = new ByteArrayInputStream(targetArray);
                // one line, able to handle large size?
                //zos.write(bais.readAllBytes());

                // play safe
//                byte[] buffer = new byte[1024];
//                int len;
//                while ((len = bais.read(buffer)) > 0) {
//                    zos.write(buffer, 0, len);
//                }
                zos.closeEntry();
            }
            zos.finish();
            zos.close();
            DownloadDocResponse downloadDocResponse = new DownloadDocResponse();
            downloadDocResponse.setFileName("files");
            downloadDocResponse.setFileFormat("application/zip");
            downloadDocResponse.setEncodedDoc(new String(Base64.encodeBase64(byteArrayOutputStream.toByteArray())));
//            try (OutputStream stream = new FileOutputStream("C:\\Users\\Mohamedsalah\\Desktop\\test.zip")) {
//                stream.write(byteArrayOutputStream.toByteArray());
//            }
            return downloadDocResponse;
        }
    }


    public DSMessageDto getMessage(String msgCode) {
        DsMessagesEntity msgEntity = messagesDao.findByMsgCodeAndLang(msgCode, userDetails.getUserCurrentLocale().toLowerCase(Locale.ROOT));
        if (msgEntity != null)
            return new DSMessageDto(msgEntity.getMsgCode(), msgEntity.getMsgText(), msgEntity.getHttpStatusCode());

        return new DSMessageDto(msgCode, "??" + msgCode + "??", 500);

    }
}
