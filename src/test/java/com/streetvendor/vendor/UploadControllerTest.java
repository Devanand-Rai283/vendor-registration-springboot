package com.streetvendor.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.support.AbstractIntegrationTest;
import com.streetvendor.vendor.dto.GeneratePresignedUrlRequest;
import com.streetvendor.vendor.dto.GeneratePresignedUrlResponse;
import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.service.DocumentUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
class UploadControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private DocumentUploadService documentUploadService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturn200OnSuccessfulPresignedUrlGeneration() throws Exception {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                DocumentType.FSSAI_CERTIFICATE, "application/pdf", 1024L
        );
        GeneratePresignedUrlResponse response = new GeneratePresignedUrlResponse(
                "https://upload.example.com/presigned-url",
                "vendors/" + UUID.randomUUID() + "/documents/fssai_certificate/uuid"
        );

        when(documentUploadService.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("https://upload.example.com/presigned-url"))
                .andExpect(jsonPath("$.fileUrl").exists());
    }

    @Test
    void shouldReturn400OnNullFileType() throws Exception {
        String invalidBody = """
                {
                    "mimeType": "application/pdf",
                    "fileSizeBytes": 1024
                }
                """;

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn400OnBlankMimeType() throws Exception {
        String invalidBody = """
                {
                    "fileType": "FSSAI_CERTIFICATE",
                    "mimeType": "",
                    "fileSizeBytes": 1024
                }
                """;

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400OnNullFileSizeBytes() throws Exception {
        String invalidBody = """
                {
                    "fileType": "FSSAI_CERTIFICATE",
                    "mimeType": "application/pdf"
                }
                """;

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400OnNegativeFileSize() throws Exception {
        String invalidBody = """
                {
                    "fileType": "FSSAI_CERTIFICATE",
                    "mimeType": "application/pdf",
                    "fileSizeBytes": -1
                }
                """;

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldDelegateToService() throws Exception {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                DocumentType.FSSAI_CERTIFICATE, "application/pdf", 1024L
        );
        GeneratePresignedUrlResponse response = new GeneratePresignedUrlResponse(
                "https://upload.example.com/url", "file-key"
        );

        when(documentUploadService.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(documentUploadService).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    }
}
