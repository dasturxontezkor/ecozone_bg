package uz.ekoulash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.ekoulash.entity.Branch;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findAllByOrderByCreatedAtDesc();
}
