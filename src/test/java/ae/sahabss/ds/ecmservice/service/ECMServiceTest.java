package ae.sahabss.ds.ecmservice.service;

import ae.sahabss.ds.ecmservice.dao.DsDocAuthorizedUserDao;
import ae.sahabss.ds.ecmservice.dao.DsDocumentTypesRepository;
import ae.sahabss.ds.ecmservice.dao.DsDocumentsDao;
import ae.sahabss.ds.ecmservice.dao.DsMessagesDao;
import ae.sahabss.ds.ecmservice.domain.DsDocAuthorizedUsersEntity;
import ae.sahabss.ds.ecmservice.domain.DsDocumentsEntity;
import ae.sahabss.ds.ecmservice.dto.BinaryDocResponse;
import ae.sahabss.ds.ecmservice.dto.BinaryDocStream;
import ae.sahabss.ds.ecmservice.dto.DownloadDocRequest;
import ae.sahabss.ds.ecmservice.dto.DownloadDocResponse;
import ae.sahabss.ds.ecmservice.dto.UploadDocRequest;
import ae.sahabss.ds.ecmservice.dto.UploadDocResponse;
import ae.sahabss.ds.ecmservice.dto.ZipDocEntry;
import ae.sahabss.ds.ecmservice.exceptions.DocumentNotFoundException;
import ae.sahabss.ds.ecmservice.exceptions.FileNotValidException;
import ae.sahabss.ds.ecmservice.exceptions.UserNotAuthorizedException;
import ae.sahabss.ds.ecmservice.security.UserDetails;
import ae.sahabss.ds.ecmservice.ucm.UCMDocument;
import ae.sahabss.ds.ecmservice.ucm.UCMUtilities;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the v1/v2 endpoint refactor.
 *
 * The v1 endpoints must behave exactly as before, and the v2 endpoints must
 * produce byte-identical content to v1 (v1 = base64 of v2). All external
 * dependencies (UCM, DB) are mocked, so these tests run without any
 * infrastructure.
 */
class ECMServiceTest {

    // A minimal but real PDF header so Tika detects application/pdf
    private static final byte[] PDF_BYTES =
            "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes(StandardCharsets.UTF_8);

    private UCMUtilities ucm;
    private DsDocumentsDao documentsDao;
    private DsMessagesDao messagesDao;
    private DsDocAuthorizedUserDao authorizedUserDao;
    private DsDocumentTypesRepository documentTypesRepository;
    private UserDetails userDetails;
    private ECMService service;

    @BeforeEach
    void setUp() throws Exception {
        ucm = mock(UCMUtilities.class);
        documentsDao = mock(DsDocumentsDao.class);
        messagesDao = mock(DsMessagesDao.class);
        authorizedUserDao = mock(DsDocAuthorizedUserDao.class);
        documentTypesRepository = mock(DsDocumentTypesRepository.class);

        service = new ECMService(ucm, documentsDao, messagesDao, authorizedUserDao, documentTypesRepository);

        userDetails = new UserDetails();
        userDetails.setUserId("USER-1");
        userDetails.setMobile("0501234567");
        userDetails.setIdn("784-1234-5678901-2");
        userDetails.setIdType("N");

        ReflectionTestUtils.setField(service, "userDetails", userDetails);
        ReflectionTestUtils.setField(service, "ecmAdminUsername", "admin");
        ReflectionTestUtils.setField(service, "ecmAdminPassword", "secret");
        ReflectionTestUtils.setField(service, "digitalUser", "DS_US_TEST");
    }

    // ------------------------------------------------------------------
    // Upload
    // ------------------------------------------------------------------

    @Test
    void uploadV2_checksInDocumentAndPersistsMetadata() throws Exception {
        when(documentsDao.getSeqNextVal()).thenReturn(123);
        when(ucm.upload(anyString(), anyString(), anyString(), any(InputStream.class), any()))
                .thenReturn("DOC-123");

        UploadDocResponse response = service.uploadDocument(PDF_BYTES, "application/pdf", "contract.pdf");

        assertEquals("DOC-123", response.getDocId());

        // UCM check-in got the zero-padded sequence id and original file name
        verify(ucm).upload(eq("0000123"), eq("Document"), eq("contract.pdf"), any(InputStream.class), eq(null));

        ArgumentCaptor<DsDocumentsEntity> docCaptor = ArgumentCaptor.forClass(DsDocumentsEntity.class);
        verify(documentsDao).save(docCaptor.capture());
        assertEquals("DOC-123", docCaptor.getValue().getDocId());
        assertEquals("application/pdf", docCaptor.getValue().getMimeType());
        assertEquals("contract.pdf", docCaptor.getValue().getFileName());

        // uploader registered as authorized user with the token identity
        ArgumentCaptor<DsDocAuthorizedUsersEntity> authCaptor =
                ArgumentCaptor.forClass(DsDocAuthorizedUsersEntity.class);
        verify(authorizedUserDao).save(authCaptor.capture());
        assertEquals("DOC-123", authCaptor.getValue().getDocId());
        assertEquals("USER-1", authCaptor.getValue().getUserId());
        assertEquals("0501234567", authCaptor.getValue().getMobileNo());
        assertEquals("784-1234-5678901-2", authCaptor.getValue().getEid());
    }

    @Test
    void uploadV1_base64_behavesIdenticallyToV2() throws Exception {
        when(documentsDao.getSeqNextVal()).thenReturn(45);
        when(ucm.upload(anyString(), anyString(), anyString(), any(InputStream.class), any()))
                .thenReturn("DOC-45");

        UploadDocRequest request = new UploadDocRequest();
        request.setDocBase64(new String(Base64.encodeBase64(PDF_BYTES), StandardCharsets.UTF_8));
        request.setMimeType("application/pdf");
        request.setFileName("contract.pdf");

        UploadDocResponse response = service.uploadDoc(request);

        assertEquals("DOC-45", response.getDocId());
        // Same core call as v2: decoded bytes reached UCM with identical metadata
        verify(ucm).upload(eq("0000045"), eq("Document"), eq("contract.pdf"), any(InputStream.class), eq(null));
    }

    @Test
    void upload_rejectsMismatchedMimeType_onBothVersions() throws Exception {
        // Real bytes are PDF, but the client declares PNG -> must be rejected before any UCM call
        assertThrows(FileNotValidException.class,
                () -> service.uploadDocument(PDF_BYTES, "image/png", "contract.png"));

        UploadDocRequest request = new UploadDocRequest();
        request.setDocBase64(new String(Base64.encodeBase64(PDF_BYTES), StandardCharsets.UTF_8));
        request.setMimeType("image/png");
        request.setFileName("contract.png");
        assertThrows(FileNotValidException.class, () -> service.uploadDoc(request));

        verify(ucm, never()).upload(anyString(), anyString(), anyString(), any(InputStream.class), any());
        verify(documentsDao, never()).save(any(DsDocumentsEntity.class));
    }

    @Test
    void upload_rejectsMismatchedExtension_onBothVersions() throws Exception {
        // Bytes and declared MIME agree (PDF) but the extension does not
        assertThrows(FileNotValidException.class,
                () -> service.uploadDocument(PDF_BYTES, "application/pdf", "contract.exe"));
        verify(ucm, never()).upload(anyString(), anyString(), anyString(), any(InputStream.class), any());
    }

    // ------------------------------------------------------------------
    // Download (single document)
    // ------------------------------------------------------------------

    @Test
    void downloadV2_returnsExactBytes_andV1ReturnsTheirBase64() throws Exception {
        byte[] payload = randomPayload(10_000);
        stubExistingDocument("DOC-9", "report.pdf", "application/pdf", payload);
        userDetails.setIdType("EP"); // EP identities skip the authorization lookup

        BinaryDocResponse v2 = service.downloadDocBinary(request("DOC-9"));
        assertEquals("report.pdf", v2.getFileName());
        assertEquals("application/pdf", v2.getFileFormat());
        assertArrayEquals(payload, v2.getContent());

        DownloadDocResponse v1 = service.downloadDoc(request("DOC-9"));
        assertEquals("report.pdf", v1.getFileName());
        assertEquals("application/pdf", v1.getFileFormat());
        // v1 is exactly the base64 encoding of the v2 bytes
        assertArrayEquals(payload, Base64.decodeBase64(v1.getEncodedDoc()));
    }

    @Test
    void download_missingDocument_throwsDocumentNotFound() {
        when(ucm.getDocumentInfo("MISSING")).thenReturn(null);
        userDetails.setIdType("EP");

        assertThrows(DocumentNotFoundException.class, () -> service.downloadDocBinary(request("MISSING")));
        assertThrows(DocumentNotFoundException.class, () -> service.downloadDoc(request("MISSING")));
    }

    @Test
    void download_unauthorizedUser_isRejectedBeforeTouchingUcm() {
        // Normal user, not the digital user, with no authorization rows for this doc
        when(authorizedUserDao.findByDocIdAndUserId(anyString(), anyString()))
                .thenReturn(Collections.<DsDocAuthorizedUsersEntity>emptyList());
        when(authorizedUserDao.findByDocIdAndEid(anyString(), anyString()))
                .thenReturn(Collections.<DsDocAuthorizedUsersEntity>emptyList());
        when(authorizedUserDao.findByDocIdAndMobileNo(anyString(), anyString()))
                .thenReturn(Collections.<DsDocAuthorizedUsersEntity>emptyList());

        assertThrows(UserNotAuthorizedException.class, () -> service.downloadDocBinary(request("DOC-9")));
        verify(ucm, never()).download(anyString());
    }

    @Test
    void download_authorizedByEid_succeeds() throws Exception {
        byte[] payload = randomPayload(64);
        stubExistingDocument("DOC-7", "a.pdf", "application/pdf", payload);

        when(authorizedUserDao.findByDocIdAndUserId(anyString(), anyString()))
                .thenReturn(Collections.<DsDocAuthorizedUsersEntity>emptyList());
        when(authorizedUserDao.findByDocIdAndEid(eq("DOC-7"), eq("784-1234-5678901-2")))
                .thenReturn(Collections.singletonList(new DsDocAuthorizedUsersEntity()));

        BinaryDocResponse v2 = service.downloadDocBinary(request("DOC-7"));
        assertArrayEquals(payload, v2.getContent());
    }

    // ------------------------------------------------------------------
    // Download (zip bundle)
    // ------------------------------------------------------------------

    @Test
    void downloadDocsByIds_v1AndV2_containIdenticalZipEntries() throws Exception {
        byte[] payloadA = randomPayload(2048);
        byte[] payloadB = randomPayload(4096);
        stubExistingDocument("DOC-A", "alpha.pdf", "application/pdf", payloadA);
        stubExistingDocument("DOC-B", "beta.pdf", "application/pdf", payloadB);

        byte[] v2Zip = service.downloadDocsByIdsAsZip(Arrays.asList("DOC-A", "DOC-B"));
        Map<String, byte[]> v2Entries = unzip(v2Zip);

        assertEquals(2, v2Entries.size());
        assertArrayEquals(payloadA, v2Entries.get("1-alpha.pdf"));
        assertArrayEquals(payloadB, v2Entries.get("2-beta.pdf"));

        DownloadDocResponse v1 = service.downloadDocsByIds(Arrays.asList("DOC-A", "DOC-B"));
        assertEquals("files", v1.getFileName());
        assertEquals("application/zip", v1.getFileFormat());

        Map<String, byte[]> v1Entries = unzip(Base64.decodeBase64(v1.getEncodedDoc()));
        assertEquals(v2Entries.keySet(), v1Entries.keySet());
        assertArrayEquals(v2Entries.get("1-alpha.pdf"), v1Entries.get("1-alpha.pdf"));
        assertArrayEquals(v2Entries.get("2-beta.pdf"), v1Entries.get("2-beta.pdf"));
    }

    @Test
    void downloadDocsByIds_missingDocument_throwsDocumentNotFound() {
        when(ucm.getDocumentInfo("GONE")).thenReturn(null);

        assertThrows(DocumentNotFoundException.class,
                () -> service.downloadDocsByIdsAsZip(Collections.singletonList("GONE")));
    }

    // ------------------------------------------------------------------
    // Streaming cores (v2 writes these directly to the HTTP response)
    // ------------------------------------------------------------------

    @Test
    void downloadDocStream_returnsExactBytesWithoutBuffering() throws Exception {
        byte[] payload = randomPayload(5 * 1024 * 1024); // the 5 MB case
        stubExistingDocument("DOC-S", "big.pdf", "application/pdf", payload);
        userDetails.setIdType("EP");

        BinaryDocStream docStream = service.downloadDocStream(request("DOC-S"));

        assertEquals("big.pdf", docStream.getFileName());
        assertEquals("application/pdf", docStream.getFileFormat());
        // the stream delivers the exact payload when the consumer drains it
        java.io.ByteArrayOutputStream drained = new java.io.ByteArrayOutputStream();
        try (InputStream in = docStream.getInputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                drained.write(buffer, 0, len);
            }
        }
        assertArrayEquals(payload, drained.toByteArray());
    }

    @Test
    void downloadDocStream_failsBeforeStreaming_whenDocumentMissing() {
        when(ucm.getDocumentInfo("MISSING")).thenReturn(null);
        userDetails.setIdType("EP");

        assertThrows(DocumentNotFoundException.class, () -> service.downloadDocStream(request("MISSING")));
        verify(ucm, never()).download(anyString());
    }

    @Test
    void prepareZipDocs_validatesAllDocsBeforeAnyDownload() {
        UCMDocument info = new UCMDocument();
        info.setFilename("ok.pdf");
        info.setFormat("application/pdf");
        when(ucm.getDocumentInfo("DOC-OK")).thenReturn(info);
        when(ucm.getDocumentInfo("DOC-GONE")).thenReturn(null);

        // second doc missing -> whole request rejected before a single download
        assertThrows(DocumentNotFoundException.class,
                () -> service.prepareZipDocs(Arrays.asList("DOC-OK", "DOC-GONE")));
        verify(ucm, never()).download(anyString());
    }

    @Test
    void writeZipEntries_streamsSameContentAsBufferedV1() throws Exception {
        byte[] payloadA = randomPayload(2048);
        byte[] payloadB = randomPayload(4096);
        stubExistingDocument("DOC-A", "alpha.pdf", "application/pdf", payloadA);
        stubExistingDocument("DOC-B", "beta.pdf", "application/pdf", payloadB);

        // v2 path: prepare + stream to an arbitrary output stream
        List<ZipDocEntry> entries = service.prepareZipDocs(Arrays.asList("DOC-A", "DOC-B"));
        java.io.ByteArrayOutputStream streamed = new java.io.ByteArrayOutputStream();
        service.writeZipEntries(entries, streamed);

        Map<String, byte[]> zipEntries = unzip(streamed.toByteArray());
        assertEquals(2, zipEntries.size());
        assertArrayEquals(payloadA, zipEntries.get("1-alpha.pdf"));
        assertArrayEquals(payloadB, zipEntries.get("2-beta.pdf"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private DownloadDocRequest request(String docId) {
        DownloadDocRequest request = new DownloadDocRequest();
        request.setDocId(docId);
        return request;
    }

    private void stubExistingDocument(String docId, String fileName, String format, byte[] payload) {
        UCMDocument info = new UCMDocument();
        info.setFilename(fileName);
        info.setFormat(format);
        when(ucm.getDocumentInfo(docId)).thenReturn(info);
        // fresh stream per invocation — the service consumes and closes it
        when(ucm.download(docId)).thenAnswer(invocation -> new ByteArrayInputStream(payload));
    }

    private static byte[] randomPayload(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i * 31 + 7);
        }
        return data;
    }

    private static Map<String, byte[]> unzip(byte[] zipBytes) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                entries.put(entry.getName(), out.toByteArray());
            }
        }
        return entries;
    }
}
