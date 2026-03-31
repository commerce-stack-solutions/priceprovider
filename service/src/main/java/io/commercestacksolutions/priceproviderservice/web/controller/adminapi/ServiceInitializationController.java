package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import io.commercestacksolutions.commons.service.setup.SelectiveDataImportManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/service-initialization")
@Tag(name = "ServiceInitialization", description = "Service data initialization API")
public class ServiceInitializationController {

    private final SelectiveDataImportManager dataImportManager;

    @Autowired
    public ServiceInitializationController(SelectiveDataImportManager dataImportManager) {
        this.dataImportManager = dataImportManager;
    }

    @Operation(summary = "Get preview of data files", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved file list")
    })
    @PreAuthorize("hasAuthority('priceprovider.admin:ServiceInitialization:write')")
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> getDataFilesPreview() {
        Map<String, Object> preview = new HashMap<>();
        preview.put("essentialFiles", dataImportManager.getEssentialDataFiles());
        preview.put("sampleFiles", dataImportManager.getSampleDataFiles());
        preview.put("essentialDataDirectory", dataImportManager.getEssentialDataDirectory());
        preview.put("sampleDataDirectory", dataImportManager.getSampleDataDirectory());
        return ResponseEntity.ok(preview);
    }

    @Operation(summary = "Load service data",
               description = "Asynchronously loads service data based on provided configuration. Returns 201 Accepted and starts background processing.",
               responses = {
            @ApiResponse(responseCode = "201", description = "Data loading started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - at least one data type must be selected")
    })
    @PreAuthorize("hasAuthority('priceprovider.admin:ServiceInitialization:write')")
    @PostMapping("/load")
    public ResponseEntity<Map<String, String>> loadData(
            @RequestParam(required = false, defaultValue = "false") boolean essential,
            @RequestParam(required = false, defaultValue = "false") boolean sample) {

        // Validate at least one option is selected
        if (!essential && !sample) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "At least one data type (essential or sample) must be selected");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Start async data loading
            dataImportManager.loadDataAsync(essential, sample);

            Map<String, String> response = new HashMap<>();
            response.put("status", "accepted");

            String dataTypes;
            if (essential && sample) {
                dataTypes = "essential and sample data";
            } else if (essential) {
                dataTypes = "essential data";
            } else {
                dataTypes = "sample data";
            }

            response.put("message", "Data loading started in background for " + dataTypes + ". Check server logs for progress.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error starting data load: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
