package com.frontend.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Generic base CRUD service providing common operations.
 * Extend this class to reduce boilerplate in entity-specific services.
 *
 * @param <T>  The entity type
 * @param <ID> The entity ID type
 */
public abstract class BaseCrudService<T, ID> {

    protected abstract JpaRepository<T, ID> getRepository();

    public List<T> findAll() {
        return getRepository().findAll();
    }

    public Optional<T> findById(ID id) {
        return getRepository().findById(id);
    }

    public T save(T entity) {
        return getRepository().save(entity);
    }

    public void deleteById(ID id) {
        getRepository().deleteById(id);
    }

    public boolean existsById(ID id) {
        return getRepository().existsById(id);
    }

    public long count() {
        return getRepository().count();
    }
}
