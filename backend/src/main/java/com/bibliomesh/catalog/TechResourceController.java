package com.bibliomesh.catalog;

import com.bibliomesh.catalog.TechResourceDtos.TechResourceResponse;
import com.bibliomesh.catalog.TechResourceDtos.UpsertTechResourceRequest;
import com.bibliomesh.catalog.internal.TechResourceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/tech-resources")
public class TechResourceController {

    private final TechResourceService techResourceService;

    public TechResourceController(TechResourceService techResourceService) {
        this.techResourceService = techResourceService;
    }

    @GetMapping
    public List<TechResourceResponse> list(@RequestParam(name = "q", required = false) String query) {
        return techResourceService.search(query);
    }

    @GetMapping("/{assetTag}")
    public TechResourceResponse getByAssetTag(@PathVariable String assetTag) {
        return techResourceService.getByAssetTag(assetTag);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TechResourceResponse upsert(@Valid @RequestBody UpsertTechResourceRequest request) {
        return techResourceService.upsert(request);
    }
}