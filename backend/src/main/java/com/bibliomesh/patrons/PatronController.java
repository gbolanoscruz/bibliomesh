package com.bibliomesh.patrons;

import com.bibliomesh.patrons.PatronDtos.PatronResponse;
import com.bibliomesh.patrons.PatronDtos.UpsertPatronRequest;
import com.bibliomesh.patrons.internal.PatronService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patrons")
public class PatronController {

    private final PatronService patronService;

    public PatronController(PatronService patronService) {
        this.patronService = patronService;
    }

    @GetMapping
    public List<PatronResponse> search(@RequestParam(name = "q", required = false) String query) {
        return patronService.search(query);
    }

    @GetMapping("/{identificationNumber}")
    public PatronResponse getByIdentification(@PathVariable String identificationNumber) {
        return patronService.getByIdentification(identificationNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatronResponse upsert(@Valid @RequestBody UpsertPatronRequest request) {
        return patronService.upsert(request);
    }

    @DeleteMapping("/{identificationNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable String identificationNumber) {
        patronService.deactivate(identificationNumber);
    }
}