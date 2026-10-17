package com.sonuSaitring.sonuSaitringManagement.Hisab.repository;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface HisabDetailRepository extends JpaRepository<HisabDetail, Long> {
  Optional<HisabDetail> findFirstByHisabIdAndEmployeeIdOrderByIdAsc(Long hisabId, Long employeeId);

  List<HisabDetail> findByHisabIdOrderByEmployeeNameAsc(Long hisabId);

  @Query("select d from HisabDetail d join d.hisab h where d.employee.id = :employeeId and h.owner.id = :ownerId order by h.year asc, h.month asc")
  List<HisabDetail> findAllForEmployeeAndOwner(@Param("employeeId") Long employeeId, @Param("ownerId") Long ownerId);

  @Query("select d from HisabDetail d join d.hisab h where d.employee.id = :employeeId and h.owner.id = :ownerId and (h.year < :year or (h.year = :year and h.month < :month)) order by h.year desc, h.month desc")
  List<HisabDetail> findPreviousDetails(@Param("employeeId") Long employeeId, @Param("ownerId") Long ownerId,
      @Param("year") Integer year, @Param("month") Integer month);

  @Query("select d from HisabDetail d join d.hisab h where h.id = :hisabId and h.owner.id = :ownerId and d.employee.id = :employeeId")
  Optional<HisabDetail> findByHisabIdAndEmployeeIdAndOwnerId(@Param("hisabId") Long hisabId,
      @Param("employeeId") Long employeeId, @Param("ownerId") Long ownerId);
}
