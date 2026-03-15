package com.frontend.repository;

import com.frontend.entity.KitchenOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenOrderRepository extends JpaRepository<KitchenOrder, Integer> {

    @Query("SELECT DISTINCT ko FROM KitchenOrder ko LEFT JOIN FETCH ko.items WHERE ko.tableNo = :tableNo ORDER BY ko.sentAt ASC")
    List<KitchenOrder> findByTableNoWithItems(@Param("tableNo") Integer tableNo);

    @Query("SELECT DISTINCT ko FROM KitchenOrder ko LEFT JOIN FETCH ko.items WHERE ko.tableNo = :tableNo AND ko.status = :status ORDER BY ko.sentAt ASC")
    List<KitchenOrder> findByTableNoAndStatusWithItems(@Param("tableNo") Integer tableNo, @Param("status") String status);

    @Query("SELECT DISTINCT ko FROM KitchenOrder ko LEFT JOIN FETCH ko.items WHERE ko.status = :status ORDER BY ko.sentAt ASC")
    List<KitchenOrder> findByStatusWithItems(@Param("status") String status);

    @Query("SELECT DISTINCT ko FROM KitchenOrder ko LEFT JOIN FETCH ko.items ORDER BY ko.sentAt ASC")
    List<KitchenOrder> findAllWithItems();

    @Query("SELECT ko FROM KitchenOrder ko LEFT JOIN FETCH ko.items WHERE ko.id = :id")
    java.util.Optional<KitchenOrder> findByIdWithItems(@Param("id") Integer id);

    List<KitchenOrder> findByTableNoOrderBySentAtAsc(Integer tableNo);

    List<KitchenOrder> findByTableNoAndStatusOrderBySentAtAsc(Integer tableNo, String status);

    boolean existsByTableNoAndStatus(Integer tableNo, String status);

    @Modifying
    @Query("DELETE FROM KitchenOrder ko WHERE ko.tableNo = :tableNo")
    void deleteByTableNo(@Param("tableNo") Integer tableNo);

    @Modifying
    @Query("UPDATE KitchenOrder ko SET ko.tableNo = :targetTableNo, ko.tableName = :targetTableName WHERE ko.tableNo = :sourceTableNo")
    void shiftKitchenOrdersToTable(@Param("sourceTableNo") Integer sourceTableNo,
                                   @Param("targetTableNo") Integer targetTableNo,
                                   @Param("targetTableName") String targetTableName);
}
