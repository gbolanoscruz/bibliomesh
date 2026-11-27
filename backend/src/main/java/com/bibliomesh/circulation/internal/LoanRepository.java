package com.bibliomesh.circulation.internal;

import com.bibliomesh.circulation.LoanStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByStatusInOrderByLoanedAtDesc(Collection<LoanStatus> statuses);

    List<Loan> findTop50ByStatusOrderByReturnedAtDesc(LoanStatus status);

    Optional<Loan> findFirstByCopyIdAndStatusIn(Long copyId, Collection<LoanStatus> statuses);

    Optional<Loan> findFirstByTechResourceIdAndStatusIn(Long techResourceId, Collection<LoanStatus> statuses);

    long countByStatusIn(Collection<LoanStatus> statuses);

    long countByStatus(LoanStatus status);
}