package com.bibliomesh.circulation;

import com.bibliomesh.circulation.CirculationDtos.CheckoutRequest;
import com.bibliomesh.circulation.CirculationDtos.DashboardStatsResponse;
import com.bibliomesh.circulation.CirculationDtos.LoanResponse;
import com.bibliomesh.circulation.CirculationDtos.ReturnRequest;
import com.bibliomesh.circulation.CirculationDtos.RoomConsultationRequest;
import com.bibliomesh.circulation.CirculationDtos.RoomConsultationResponse;
import com.bibliomesh.circulation.internal.CirculationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/circulation")
public class CirculationController {

    private final CirculationService circulationService;

    public CirculationController(CirculationService circulationService) {
        this.circulationService = circulationService;
    }

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return circulationService.getDashboardStats();
    }

    @GetMapping("/loans/active")
    public List<LoanResponse> listActiveLoans() {
        return circulationService.listActiveLoans();
    }

    @GetMapping("/loans/history")
    public List<LoanResponse> listReturnedHistory() {
        return circulationService.listRecentReturnedLoans();
    }

    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return circulationService.checkout(request);
    }

    @PostMapping("/returns")
    public LoanResponse returnItem(@Valid @RequestBody ReturnRequest request) {
        return circulationService.returnItem(request.itemCode());
    }

    @GetMapping("/room-consultations")
    public List<RoomConsultationResponse> listRecentRoomConsultations() {
        return circulationService.listRecentRoomConsultations();
    }

    @PostMapping("/room-consultations")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomConsultationResponse registerRoomConsultation(
            @Valid @RequestBody RoomConsultationRequest request) {
        return circulationService.registerRoomConsultation(request);
    }
}