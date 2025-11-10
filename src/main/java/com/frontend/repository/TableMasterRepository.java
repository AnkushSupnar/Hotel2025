package com.frontend.repository;

import com.frontend.entity.TableMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TableMaster entity
 */
@Repository
public interface TableMasterRepository extends JpaRepository<TableMaster, Integer> {

    /**
     * Find table by table name
     */
    Optional<TableMaster> findByTableName(String tableName);

    /**
     * Find tables by name containing (case-insensitive search)
     */
    List<TableMaster> findByTableNameContainingIgnoreCase(String tableName);

    /**
     * Find tables by description containing (case-insensitive search)
     */
    List<TableMaster> findByDescriptionContainingIgnoreCase(String description);

    /**
     * Find tables by exact description
     */
    List<TableMaster> findByDescription(String description);

    /**
     * Check if table exists by table name
     */
    boolean existsByTableName(String tableName);

    /**
     * Find all tables ordered by table name
     */
    List<TableMaster> findAllByOrderByTableNameAsc();

    /**
     * Find all unique descriptions (sections) from tablemaster
     */
    @Query("SELECT DISTINCT t.description FROM TableMaster t ORDER BY t.description")
    List<String> findDistinctDescriptions();
}
