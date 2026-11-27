package com.bibliomesh.opac;

import com.bibliomesh.catalog.CatalogDtos.TitleResponse;
import com.bibliomesh.catalog.TechResourceDtos.TechResourceResponse;
import com.bibliomesh.opac.OpacDtos.CreateDigitalResourceRequest;
import com.bibliomesh.opac.OpacDtos.DigitalLibraryResponse;
import com.bibliomesh.opac.OpacDtos.DigitalResourceResponse;
import com.bibliomesh.opac.OpacDtos.LabelDataResponse;
import com.bibliomesh.opac.internal.OpacService;
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
@RequestMapping("/api/opac")
public class OpacController {

    private final OpacService opacService;

    public OpacController(OpacService opacService) {
        this.opacService = opacService;
    }

    @GetMapping("/books")
    public List<TitleResponse> searchBooks(@RequestParam(name = "q", required = false) String query) {
        return opacService.searchBooks(query);
    }

    @GetMapping("/tech-resources")
    public List<TechResourceResponse> listTechResources() {
        return opacService.listTechResources();
    }

    @GetMapping("/digital")
    public DigitalLibraryResponse getDigitalLibrary() {
        return opacService.getDigitalLibrary();
    }

    @PostMapping("/digital")
    @ResponseStatus(HttpStatus.CREATED)
    public DigitalResourceResponse addDigitalResource(@Valid @RequestBody CreateDigitalResourceRequest request) {
        return opacService.addDigitalResource(request);
    }

    @GetMapping("/labels/{inscriptionNumber}")
    public LabelDataResponse getLabelData(@PathVariable String inscriptionNumber) {
        return opacService.getLabelData(inscriptionNumber);
    }
}